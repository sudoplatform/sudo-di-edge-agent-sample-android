/*
 * Copyright © 2023 Anonyome Labs, Inc. All rights reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.sudoplatform.sudodiedgeagentexample.connection.exchange

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.navigation.NavController
import com.sudoplatform.sudodiedgeagent.SudoDIEdgeAgent
import com.sudoplatform.sudodiedgeagent.connections.exchange.types.ConnectionExchange
import com.sudoplatform.sudodiedgeagent.connections.exchange.types.ConnectionExchangeRole
import com.sudoplatform.sudodiedgeagent.connections.exchange.types.ConnectionExchangeState
import com.sudoplatform.sudodiedgeagent.connections.exchange.types.CreateInvitationConfiguration
import com.sudoplatform.sudodiedgeagent.subscriptions.AgentEventSubscriber
import com.sudoplatform.sudodiedgeagent.types.Routing
import com.sudoplatform.sudodiedgeagentexample.SingleSudoManager
import com.sudoplatform.sudodiedgeagentexample.ui.theme.SCREEN_PADDING
import com.sudoplatform.sudodiedgeagentexample.ui.theme.SudoDIEdgeAgentExampleTheme
import com.sudoplatform.sudodiedgeagentexample.utils.QrCodeImage
import com.sudoplatform.sudodiedgeagentexample.utils.showToast
import com.sudoplatform.sudodiedgeagentexample.utils.showToastOnFailure
import com.sudoplatform.sudologging.Logger
import kotlinx.coroutines.launch
import java.util.UUID

/**
 * UI for a connection invitation creation screen. Displays a QR Code designed for peers to scan.
 * After the invitation is accepted by another peer, the request details
 * are displayed within an alert dialog, along with an option to accept or reject the connection.
 *
 * see here: https://github.com/hyperledger/aries-rfcs/tree/main/features/0160-connection-protocol#0-invitation-to-connect
 */
@Composable
fun ConnectionInvitationCreateScreen(
    navController: NavController,
    agent: SudoDIEdgeAgent,
    sudoManager: SingleSudoManager,
    logger: Logger,
) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    var incomingRequest: ConnectionExchange? by remember { mutableStateOf(null) }

    var isAccepting by remember { mutableStateOf(false) }
    var routing: Routing? by remember { mutableStateOf(null) }
    var createdConnectionExchangeId: String? by remember { mutableStateOf(null) }
    var createdInvitationUrl: String? by remember { mutableStateOf(null) }

    fun initializeInvitationUrl() = scope.launch {
        runCatching {
            val id = UUID.randomUUID().toString()
            val token = sudoManager.getSudoOwnershipProofToken()
            val postbox =
                sudoManager.relayClient.createPostbox(id, token)

            val newRouting = Routing(postbox.serviceEndpoint, routingVerkeys = emptyList())
            val createdInvite = agent.connections.exchange.createInvitation(
                CreateInvitationConfiguration.LegacyPairwise(newRouting),
            )
            // apply tag of service endpoint which may need to be fetched later
            createdInvite.exchange.applyInviterRelayEndpointMetadata(agent, postbox.serviceEndpoint)

            createdInvitationUrl = createdInvite.invitationUrl
            createdConnectionExchangeId = createdInvite.exchange.connectionExchangeId
            routing = newRouting
        }.showToastOnFailure(context, logger, "Failed to create")
    }

    /**
     * When the composable initializes, subscribe to agent events for connection updates.
     * If the connection update is for a connection entering the [ConnectionExchangeState.REQUEST]
     * state (i.e. request received), then set the `incomingRequest` state to this new [ConnectionExchange],
     * in turn, this state change will trigger the [AcceptRequestAlertDialog] to display.
     *
     * When the composable disposes, unsubscribe from the agent events.
     */
    DisposableEffect(key1 = Unit) {
        val subscriptionId = agent.subscribeToAgentEvents(object : AgentEventSubscriber {
            override fun connectionExchangeStateChanged(connectionExchange: ConnectionExchange) {
                if (connectionExchange.connectionExchangeId != createdConnectionExchangeId) return
                if (connectionExchange.state != ConnectionExchangeState.REQUEST) return
                incomingRequest = connectionExchange
            }
        })

        initializeInvitationUrl()

        onDispose {
            agent.unsubscribeToAgentEvents(subscriptionId)
        }
    }

    /**
     * Attempt to accept a [ConnectionExchange] in the request state by its ID.
     */
    fun acceptConnection(id: String) = scope.launch {
        showToast("Accepting connection", context)
        runCatching {
            isAccepting = true
            val existingRouting = routing ?: throw Error("Invalid state. No routing initialized")

            agent.connections.exchange.acceptConnection(id, existingRouting)

            showToast("Connection accepted", context)
            isAccepting = false
            incomingRequest = null
            navController.popBackStack()
        }.showToastOnFailure(context, logger, "Failed to accept connection")

        isAccepting = false
    }

    /**
     * Handler for the 'rejection' of a connection request. Rejection is done by
     * simply deleting the [ConnectionExchange]. Alternatively, the connection request
     * can just be ignored, however that will result in a [ConnectionExchange] left pending
     * within the agent.
     */
    fun rejectConnection(id: String) = scope.launch {
        runCatching {
            agent.connections.exchange.deleteById(id)
        }.showToastOnFailure(context, logger, "Failed to delete connection")
        incomingRequest = null
        navController.popBackStack()
    }

    ConnectionInvitationCreateScreenView(
        createdInvitationUrl = createdInvitationUrl,
        incomingRequest = incomingRequest,
        acceptConnection = { acceptConnection(it) },
        isAccepting = isAccepting,
        rejectConnection = { rejectConnection(it) },
    )
}

@Composable
private fun ConnectionInvitationCreateScreenView(
    createdInvitationUrl: String?,
    incomingRequest: ConnectionExchange?,
    acceptConnection: (id: String) -> Unit,
    isAccepting: Boolean,
    rejectConnection: (id: String) -> Unit,
) {
    // if an incoming request is set to non-null via the subscription, then display the
    // alert dialog.
    incomingRequest?.let { connEx ->
        AcceptRequestAlertDialog(
            connEx,
            onAccept = {
                acceptConnection(connEx.connectionExchangeId)
            },
            isLoading = isAccepting,
            onClose = {
                rejectConnection(connEx.connectionExchangeId)
            },
        )
    }

    Column(
        Modifier
            .fillMaxSize()
            .padding(SCREEN_PADDING),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        if (!isAccepting) {
            if (createdInvitationUrl != null) {
                QrCodeImage(content = createdInvitationUrl)
                Text(
                    text = "Scan with another wallet",
                    Modifier.padding(vertical = 32.dp),
                )
            } else {
                CircularProgressIndicator()
                Text(
                    text = "Creating invitation..",
                    Modifier.padding(vertical = 32.dp),
                )
            }
        } else {
            CircularProgressIndicator()
        }
    }
}

@Composable
private fun AcceptRequestAlertDialog(
    incomingConnEx: ConnectionExchange,
    onClose: () -> Unit,
    onAccept: () -> Unit,
    isLoading: Boolean,
) {
    if (isLoading) {
        Popup(alignment = Alignment.Center, onDismissRequest = {}) {
            CircularProgressIndicator()
        }
        return
    }

    AlertDialog(
        onDismissRequest = onClose,
        confirmButton = {
            TextButton(onClick = onAccept) {
                Text(text = "Accept")
            }
        },
        dismissButton = {
            TextButton(onClick = onClose) {
                Text(text = "Cancel")
            }
        },
        title = { Text(text = "Incoming Request") },
        text = { Text(text = "From label: ${incomingConnEx.theirLabel}") },
    )
}

@Preview(showBackground = true)
@Composable
private fun Preview() {
    SudoDIEdgeAgentExampleTheme {
        ConnectionInvitationCreateScreenView(
            createdInvitationUrl = "https://sudoplatform.com?c_i=foobar",
            incomingRequest = null,
            acceptConnection = {},
            isAccepting = false,
            rejectConnection = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun Preview2() {
    SudoDIEdgeAgentExampleTheme {
        ConnectionInvitationCreateScreenView(
            createdInvitationUrl = "https://sudoplatform.com?c_i=foobar",
            incomingRequest = null,
            acceptConnection = {},
            isAccepting = true,
            rejectConnection = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun Preview3() {
    SudoDIEdgeAgentExampleTheme {
        ConnectionInvitationCreateScreenView(
            createdInvitationUrl = "https://sudoplatform.com?c_i=foobar",
            incomingRequest = ConnectionExchange(
                "connEx1",
                null,
                ConnectionExchangeRole.INVITER,
                ConnectionExchangeState.REQUEST,
                theirLabel = "Bob",
                verkey = "",
                errorMessage = null,
                tags = emptyList(),
            ),
            acceptConnection = {},
            isAccepting = false,
            rejectConnection = {},
        )
    }
}
