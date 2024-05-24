/*
 * Copyright © 2023 Anonyome Labs, Inc. All rights reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.sudoplatform.sudodiedgeagentexample.connection

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.MailOutline
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import com.sudoplatform.sudodiedgeagent.connections.types.Connection
import com.sudoplatform.sudodiedgeagentexample.Routes
import com.sudoplatform.sudodiedgeagentexample.connection.chat.ConnectionChatScreen
import com.sudoplatform.sudodiedgeagentexample.ui.theme.SCREEN_PADDING
import com.sudoplatform.sudodiedgeagentexample.ui.theme.SudoDIEdgeAgentExampleTheme
import com.sudoplatform.sudodiedgeagentexample.utils.SwipeToDeleteCard
import com.sudoplatform.sudodiedgeagentexample.utils.showToastOnFailure
import com.sudoplatform.sudodiedgeagentexample.utils.swapList
import com.sudoplatform.sudologging.Logger
import kotlinx.coroutines.launch

@Composable
fun ConnectionsScreen(
    navController: NavController,
    agent: SudoDIEdgeAgent,
    logger: Logger,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var isListLoading by remember { mutableStateOf(false) }
    val connectionList = remember { mutableStateListOf<Connection>() }

    /**
     * Re-set the `connectionList` state to be the latest list of [Connection]s
     * fetched from the [SudoDIEdgeAgent] SDK.
     */
    fun refreshConnectionList() {
        scope.launch {
            isListLoading = true
            kotlin.runCatching {
                connectionList.swapList(agent.connections.listAll())
            }.showToastOnFailure(context, logger)
            isListLoading = false
        }
    }

    /**
     * Delete a [Connection] by its ID, [Connection.connectionId],
     * Refreshing the displayed connection list if successful.
     */
    fun deleteConnection(id: String) {
        scope.launch {
            runCatching {
                agent.connections.deleteById(id)
                connectionList.removeIf { it.connectionId == id }
            }.showToastOnFailure(context, logger)
        }
    }

    /**
     * Navigate to the [ConnectionChatScreen] for the selected connection [id]
     */
    fun navigateToChatView(id: String) {
        navController.navigate("${Routes.CHAT}/$id")
    }

    /**
     * When this composable initializes, load the connection list
     */
    LaunchedEffect(key1 = Unit) {
        refreshConnectionList()
    }

    ConnectionsScreenView(
        isListLoading,
        connectionList,
        refreshConnectionList = { refreshConnectionList() },
        deleteConnection = { deleteConnection(it) },
        onOpenChat = { navigateToChatView(it) },
    )
}

/**
 * UI for the "Connections screen". Allows viewing and managing the [Connection]s held by the agent.
 */
@Composable
fun ConnectionsScreenView(
    isListLoading: Boolean,
    connectionList: List<Connection>,
    refreshConnectionList: () -> Unit,
    deleteConnection: (id: String) -> Unit,
    onOpenChat: (id: String) -> Unit,
) {
    Column(
        Modifier
            .fillMaxSize()
            .padding(SCREEN_PADDING),
    ) {
        Text(
            text = "Connections",
            Modifier
                .fillMaxWidth(),
            textAlign = TextAlign.Center,
            fontWeight = FontWeight.Bold,
        )
        if (isListLoading) {
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
                    items = connectionList.trySortByDateDescending(),
                    key = { it.connectionId },
                    itemContent = { item ->
                        val currentItem = rememberUpdatedState(item)
                        Box(modifier = Modifier.padding(vertical = 4.dp)) {
                            SwipeToDeleteCard(onDelete = {
                                deleteConnection(currentItem.value.connectionId)
                            }) {
                                ConnectionItemCardContent(
                                    currentItem.value,
                                    onOpenChat = { onOpenChat(currentItem.value.connectionId) },
                                )
                            }
                        }
                    },
                )
            }
        }
        Button(onClick = { refreshConnectionList() }, Modifier.fillMaxWidth()) {
            Text(text = "Refresh")
        }
    }
}

/**
 * Composable for displaying a [Connection] on a card.
 */
@Composable
private fun ConnectionItemCardContent(
    item: Connection,
    onOpenChat: () -> Unit,
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
                text = item.connectionId,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        IconButton(onOpenChat) {
            Icon(
                Icons.Outlined.MailOutline,
                contentDescription = "Start chatting",
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun DefaultPreview() {
    SudoDIEdgeAgentExampleTheme {
        ConnectionsScreenView(
            isListLoading = false,
            listOf(
                Connection(
                    "1",
                    "conn1",
                    "John",
                    listOf(),
                ),
                Connection(
                    "2",
                    "conn2",
                    "Doe",
                    listOf(),
                ),
            ),
            {},
            {},
            {},
        )
    }
}
