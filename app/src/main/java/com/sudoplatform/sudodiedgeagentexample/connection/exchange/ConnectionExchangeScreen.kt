/*
 * Copyright © 2023 Anonyome Labs, Inc. All rights reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.sudoplatform.sudodiedgeagentexample.connection.exchange

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.sudoplatform.sudodiedgeagent.SudoDIEdgeAgent
import com.sudoplatform.sudodiedgeagent.connections.exchange.types.ConnectionExchange
import com.sudoplatform.sudodiedgeagent.connections.exchange.types.ConnectionExchangeRole
import com.sudoplatform.sudodiedgeagent.connections.exchange.types.ConnectionExchangeState
import com.sudoplatform.sudodiedgeagent.subscriptions.AgentEventSubscriber
import com.sudoplatform.sudodiedgeagent.types.Routing
import com.sudoplatform.sudodiedgeagentexample.Routes
import com.sudoplatform.sudodiedgeagentexample.SingleSudoManager
import com.sudoplatform.sudodiedgeagentexample.relay.SudoDIRelayMessageSource
import com.sudoplatform.sudodiedgeagentexample.ui.theme.SCREEN_PADDING
import com.sudoplatform.sudodiedgeagentexample.ui.theme.SudoDIEdgeAgentExampleTheme
import com.sudoplatform.sudodiedgeagentexample.utils.SwipeToDeleteCard
import com.sudoplatform.sudodiedgeagentexample.utils.showToast
import com.sudoplatform.sudodiedgeagentexample.utils.showToastOnFailure
import com.sudoplatform.sudodiedgeagentexample.utils.swapList
import com.sudoplatform.sudodirelay.SudoDIRelayClient
import com.sudoplatform.sudodirelay.types.Postbox
import com.sudoplatform.sudologging.Logger
import kotlinx.coroutines.launch

@Composable
fun ConnectionExchangeScreen(
    navController: NavController,
    agent: SudoDIEdgeAgent,
    sudoManager: SingleSudoManager,
    logger: Logger,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var isListLoading by remember { mutableStateOf(false) }
    var isAcceptingConnection by remember { mutableStateOf(false) }
    val connectionExchangeList = remember { mutableStateListOf<ConnectionExchange>() }

    /**
     * Re-set the `connectionExchangeList` state to be the latest list of [ConnectionExchange]s
     * fetched from the [SudoDIEdgeAgent] SDK.
     */
    fun refreshConnectionExchangeList() {
        scope.launch {
            isListLoading = true
            runCatching {
                connectionExchangeList.swapList(agent.connections.exchange.listAll())
            }.showToastOnFailure(context, logger)
            isListLoading = false
        }
    }

    /**
     * Delete a [ConnectionExchange] by its ID, [ConnectionExchange.connectionExchangeId],
     * Refreshing the displayed connection exchange list if successful.
     */
    fun deleteConnectionExchange(id: String) {
        scope.launch {
            runCatching {
                agent.connections.exchange.deleteById(id)
                connectionExchangeList.removeIf { it.connectionExchangeId == id }
            }.showToastOnFailure(context, logger)
        }
    }

    /**
     * Attempt to accept a [ConnectionExchange] in the invitation state by its ID.
     *
     * This involves creating a [Postbox] with the [SudoDIRelayClient], then using the
     * [SudoDIRelayMessageSource] to translate the [Postbox] into a [Routing], which
     * is then used to inform the connection peer how they should communicate to this
     * agent via the relay service.
     */
    fun acceptInvitationConnection(id: String) = scope.launch {
        isAcceptingConnection = true

        showToast("Accepting connection", context)
        runCatching {
            val token = sudoManager.getSudoOwnershipProofToken()
            val postbox =
                sudoManager.relayClient.createPostbox(id, token)

            val routing = SudoDIRelayMessageSource.routingFromPostbox(postbox)

            agent.connections.exchange.acceptConnection(id, routing)

            showToast("Connection accepted", context)
        }.showToastOnFailure(context, logger, "Failed to accept connection")

        isAcceptingConnection = false
    }

    /**
     * Attempt to accept an inviter [ConnectionExchange] in the request state by its ID.
     *
     * This involves custom usage of tags to fetch the previously created [Postbox.serviceEndpoint]
     * for this [ConnectionExchange], then construct it back into a [Routing], which is passed
     * into the accept method. This eliminates the need to re-create another postbox.
     */
    fun acceptRequestConnection(id: String) = scope.launch {
        isAcceptingConnection = true
        showToast("Accepting connection", context)
        runCatching {
            val connEx = agent.connections.exchange.getById(id) ?: throw Exception("$id not found")
            // reconstruct routing
            val relayEndpoint = connEx.extractInviterRelayEndpointMetadata()
                ?: throw Exception("failed to determine relay endpoint of inviter")
            val routing = Routing(serviceEndpoint = relayEndpoint, emptyList())

            agent.connections.exchange.acceptConnection(id, routing)

            showToast("Connection accepted", context)
        }.showToastOnFailure(context, logger, "Failed to accept connection")
        isAcceptingConnection = false
    }

    /**
     * When this composable initializes, load the connection exchange list
     */
    LaunchedEffect(key1 = Unit) {
        refreshConnectionExchangeList()
    }

    /**
     * When this composable initializes, subscribe to connection exchange updates.
     * Whenever a connection exchange update occurs, refresh the whole list of displayed
     * [ConnectionExchange]s.
     *
     * If the connection update is for the [ConnectionExchangeState.COMPLETE] state, then
     * display a toast indicating that the connection is 'established'.
     *
     * When the composable disposes, unsubscribe from the events.
     *
     * Note that refreshing the entire list for each event is NOT efficient, but is done
     * for simplicity demonstration purposes.
     */
    DisposableEffect(key1 = Unit) {
        val subscriptionId = agent.subscribeToAgentEvents(object : AgentEventSubscriber {
            override fun connectionExchangeStateChanged(connectionExchange: ConnectionExchange) {
                refreshConnectionExchangeList()

                if (connectionExchange.state == ConnectionExchangeState.COMPLETE) {
                    val msg =
                        "Connection Exchange completed. A new connection can be found in the 'Connections' screen: ${connectionExchange.connectionId}"
                    scope.launch {
                        showToast(msg, context)
                    }
                }
            }
        })

        onDispose {
            agent.unsubscribeToAgentEvents(subscriptionId)
        }
    }

    ConnectionExchangeScreenView(
        isListLoading,
        isAcceptingConnection,
        connectionExchangeList,
        refreshConnectionExchangeList = { refreshConnectionExchangeList() },
        deleteConnectionExchange = { deleteConnectionExchange(it) },
        acceptConnectionInvitation = { acceptInvitationConnection(it) },
        acceptConnectionRequest = { acceptRequestConnection(it) },
        navigateToInvitationScanner = { navController.navigate(Routes.CONNECTION_INVITATION_SCANNER) },
        navigateToInvitationCreate = { navController.navigate(Routes.CONNECTION_INVITATION_CREATE) },
    )
}

/**
 * UI for the "Connection Exchange screen". Allows viewing and managing
 * the [ConnectionExchange]s held by the agent (connections mid-establishment).
 *
 * Clicking "Scan Invitation" will navigate to a screen where a connection invitation
 * can be scanned via QR Code.
 */
@Composable
fun ConnectionExchangeScreenView(
    isListLoading: Boolean,
    isAcceptingInvitation: Boolean,
    connectionExchangeList: List<ConnectionExchange>,
    refreshConnectionExchangeList: () -> Unit,
    deleteConnectionExchange: (id: String) -> Unit,
    acceptConnectionInvitation: (id: String) -> Unit,
    acceptConnectionRequest: (id: String) -> Unit,
    navigateToInvitationScanner: () -> Unit,
    navigateToInvitationCreate: () -> Unit,
) {
    Column(
        Modifier
            .fillMaxSize()
            .padding(SCREEN_PADDING),
    ) {
        Text(
            text = "Connection Exchanges",
            Modifier
                .fillMaxWidth(),
            textAlign = TextAlign.Center,
            fontWeight = FontWeight.Bold,
        )
        if (isListLoading || isAcceptingInvitation) {
            Column(
                Modifier
                    .weight(1.0f)
                    .fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn(
                Modifier
                    .weight(1.0f)
                    .fillMaxSize()
                    .padding(vertical = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                items(
                    items = connectionExchangeList.trySortByDateDescending(),
                    key = { it.connectionExchangeId },
                    itemContent = { item ->
                        val currentItem = rememberUpdatedState(item)
                        Box(modifier = Modifier.padding(vertical = 4.dp)) {
                            SwipeToDeleteCard(onDelete = {
                                deleteConnectionExchange(currentItem.value.connectionExchangeId)
                            }) {
                                ConnectionExchangeItemCardContent(
                                    currentItem.value,
                                    acceptConnectionInvitation,
                                    acceptConnectionRequest,
                                )
                            }
                        }
                    },
                )
            }
        }
        Button(
            onClick = { refreshConnectionExchangeList() },
            Modifier.fillMaxWidth(),
            enabled = !isAcceptingInvitation,
        ) {
            Text(text = "Refresh")
        }
        Button(
            onClick = navigateToInvitationScanner,
            Modifier.fillMaxWidth(),
            enabled = !isAcceptingInvitation,
        ) {
            Text(text = "Scan Invitation")
        }
        Button(
            onClick = navigateToInvitationCreate,
            Modifier.fillMaxWidth(),
            enabled = !isAcceptingInvitation,
        ) {
            Text(text = "Create Invitation")
        }
    }
}

/**
 * Composable for displaying a [ConnectionExchange] on a card to be displayed in a
 * list.
 *
 * If the [ConnectionExchange] is in the [ConnectionExchangeState.INVITATION] state,
 * then the ability (button) to [acceptConnectionInvitation] is shown.
 */
@Composable
private fun ConnectionExchangeItemCardContent(
    item: ConnectionExchange,
    acceptConnectionInvitation: (id: String) -> Unit,
    acceptConnectionRequest: (id: String) -> Unit,
) {
    Row(
        Modifier.padding(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1.0f)) {
            Text(
                text = item.theirLabel ?: "No Label",
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = item.connectionExchangeId,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = item.state.toString().lowercase()
                    .replaceFirstChar { it.uppercase() },
            )
        }
        if (
            item.state == ConnectionExchangeState.INVITATION &&
            item.role == ConnectionExchangeRole.INVITEE
        ) {
            Button(onClick = { acceptConnectionInvitation(item.connectionExchangeId) }) {
                Text(text = "Accept")
            }
        }
        if (
            item.state == ConnectionExchangeState.REQUEST &&
            item.role == ConnectionExchangeRole.INVITER
        ) {
            Button(onClick = { acceptConnectionRequest(item.connectionExchangeId) }) {
                Text(text = "Accept")
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun DefaultPreview() {
    SudoDIEdgeAgentExampleTheme {
        ConnectionExchangeScreenView(
            isListLoading = false,
            isAcceptingInvitation = false,
            listOf(
                ConnectionExchange(
                    "1",
                    "conn1",
                    ConnectionExchangeRole.INVITEE,
                    ConnectionExchangeState.INVITATION,
                    "John",
                    "",
                    null,
                    listOf(),
                ),
                ConnectionExchange(
                    "2",
                    "conn1",
                    ConnectionExchangeRole.INVITEE,
                    ConnectionExchangeState.REQUEST,
                    "Doe",
                    "",
                    null,
                    listOf(),
                ),
                ConnectionExchange(
                    "3",
                    "conn1",
                    ConnectionExchangeRole.INVITER,
                    ConnectionExchangeState.REQUEST,
                    "Doe",
                    "",
                    null,
                    listOf(),
                ),
            ),
            {},
            {},
            {},
            {},
            {},
            {},
        )
    }
}
