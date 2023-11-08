package com.sudoplatform.sudodiedgeagentexample.relay

import com.sudoplatform.sudodiedgeagent.connections.exchange.ConnectionExchangeModule
import com.sudoplatform.sudodiedgeagent.plugins.messagesource.Message
import com.sudoplatform.sudodiedgeagent.plugins.messagesource.MessageSource
import com.sudoplatform.sudodiedgeagent.types.Routing
import com.sudoplatform.sudodirelay.SudoDIRelayClient
import com.sudoplatform.sudodirelay.types.ListOutput
import com.sudoplatform.sudodirelay.types.Postbox
import com.sudoplatform.sudologging.Logger
import com.sudoplatform.sudodirelay.types.Message as RelayMessage

/**
 * Implementation of [MessageSource] which consumes a [SudoDIRelayClient] to fetch messages.
 *
 * This is a very simple [MessageSource] implementation, which sacrifices efficiency for
 * simplicity. More advanced implementations may want to consider application specific requirements
 * and integrations, such as: hooks into application lifecycle, usage of the relay service's
 * websocket subscriptions, integration with a notification service, etc.
 */
class SudoDIRelayMessageSource(private val relayClient: SudoDIRelayClient, private val logger: Logger) :
    MessageSource {

    companion object {
        /**
         * Utility function to translate a [SudoDIRelayClient]'s [Postbox] into a
         * [Routing] such that the postbox's public information can be shared with
         * peers when starting a connection (i.e. via [ConnectionExchangeModule.acceptConnection]).
         */
        fun routingFromPostbox(postbox: Postbox): Routing {
            return Routing(
                serviceEndpoint = postbox.serviceEndpoint,
                routingVerkeys = emptyList(),
            )
        }
    }

    // NOTE - CONSIDERATION: [SudoDIRelayMessageSource] could hook into the
    // relayClient.subscribeToRelayEvents method to subscribe to new messages as a way to flag
    // new messages. This would save `getMessage` having to query `listMessages` every time.
    /**
     * try "get" the next message from the [MessageSource] by fetching the list of relay messages
     * and returning the top one if any. The relay message is then transformed into a [Message]
     * understood by the edge agent.
     */
    @Throws(SudoDIRelayClient.DIRelayException::class)
    override suspend fun getMessage(): Message? {
        val result = tryGetMessage()
        if (result != null) {
            return Message(result.id, result.message.toByteArray())
        }
        return null
    }

    /**
     * After a relay message is processed by the agent, it can be "finalized" by deleting it
     * from the backend, this way it will not re-appear in subsequent calls to [getMessage].
     */
    @Throws(SudoDIRelayClient.DIRelayException::class)
    override suspend fun finalizeMessage(id: String) {
        try {
            relayClient.deleteMessage(id)
        } catch (e: SudoDIRelayClient.DIRelayException) {
            logger.error("Unable to delete message $id: ${e.localizedMessage}")
            throw e
        }
    }

    @Throws(SudoDIRelayClient.DIRelayException::class)
    private suspend fun tryGetMessage(): RelayMessage? {
        try {
            val messagesList: ListOutput<RelayMessage> = relayClient.listMessages()
            return messagesList.items.firstOrNull()
            // Note in theory it would be possible to cache multiple messages here, and return them
            // as requested. This implementation would be made more complex by having to manage
            // the interaction with finalizeMessage and is not deemed necessary for the most common
            // use case (where there are a very small number of messages in the postbox), and each call
            // to getMessage is matched by an almost immediate finalize.
        } catch (e: SudoDIRelayClient.DIRelayException) {
            logger.error("Unable to retrieve messages: ${e.localizedMessage}")
            throw e
        }
    }
}
