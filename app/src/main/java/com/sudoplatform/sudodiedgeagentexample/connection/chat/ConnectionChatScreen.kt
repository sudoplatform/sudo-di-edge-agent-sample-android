/*
 * Copyright © 2023 Anonyome Labs, Inc. All rights reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.sudoplatform.sudodiedgeagentexample.connection.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.sudoplatform.sudodiedgeagent.SudoDIEdgeAgent
import com.sudoplatform.sudodiedgeagent.connections.messaging.types.BasicMessage
import com.sudoplatform.sudodiedgeagent.connections.messaging.types.ListBasicMessagesFilter
import com.sudoplatform.sudodiedgeagent.connections.messaging.types.ListBasicMessagesOptions
import com.sudoplatform.sudodiedgeagent.connections.messaging.types.ListBasicMessagesSorting
import com.sudoplatform.sudodiedgeagent.connections.types.Connection
import com.sudoplatform.sudodiedgeagent.subscriptions.AgentEventSubscriber
import com.sudoplatform.sudodiedgeagent.types.Paging
import com.sudoplatform.sudodiedgeagent.types.SortDirection
import com.sudoplatform.sudodiedgeagentexample.ui.theme.SCREEN_PADDING
import com.sudoplatform.sudodiedgeagentexample.ui.theme.SudoDIEdgeAgentExampleTheme
import com.sudoplatform.sudodiedgeagentexample.utils.showToastOnFailure
import com.sudoplatform.sudodiedgeagentexample.utils.swapList
import com.sudoplatform.sudologging.Logger
import kotlinx.coroutines.launch
import java.time.Instant

/** Paging limit of basic messages to fetch. Kept low for sake of demonstrating pagination. */
private const val PAGE_LIMIT = 10

@Composable
fun ConnectionChatScreen(
    connectionId: String,
    agent: SudoDIEdgeAgent,
    logger: Logger,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var connection: Connection? by remember { mutableStateOf(null) }

    var isLoadingMore by remember { mutableStateOf(false) }
    var nextToken: String? by remember { mutableStateOf(null) }
    val messageList = remember { mutableStateListOf<BasicMessage>() }

    /**
     * On initialization of the composable, load an initial page of messages for this [connectionId]
     * and fetch the full details of the [Connection]. Setting the initial state for the view.
     */
    LaunchedEffect(key1 = Unit) {
        runCatching {
            val page = agent.connections.messaging.listBasicMessages(
                ListBasicMessagesOptions(
                    filters = ListBasicMessagesFilter(connectionId = connectionId),
                    paging = Paging(PAGE_LIMIT.toUInt()),
                    sorting = ListBasicMessagesSorting.Chronological(SortDirection.DESCENDING),
                ),
            )
            nextToken = page.nextToken
            messageList.swapList(page.items)

            connection = agent.connections.getById(connectionId)!!
        }.showToastOnFailure(context, logger, "Failed to initial load chat")
    }

    /**
     * Subscribe to inbound messages. If an inbound message from this connection, then
     * add it to the UI message list. Unsubscribe on disposal.
     */
    DisposableEffect(key1 = Unit) {
        val subscriptionId = agent.subscribeToAgentEvents(object : AgentEventSubscriber {
            override fun inboundBasicMessage(message: BasicMessage.Inbound) {
                if (message.connectionId != connectionId) return
                messageList.add(0, message)
            }
        })

        onDispose {
            agent.unsubscribeToAgentEvents(subscriptionId)
        }
    }

    /**
     * Send a message to the [connectionId], then add the sent message to the UI message list.
     */
    fun sendMessage(content: String) = scope.launch {
        runCatching {
            val sentMessage = agent.connections.messaging.sendBasicMessage(connectionId, content)
            messageList.add(0, sentMessage)
        }.showToastOnFailure(context, logger, "Failed to send message")
    }

    /**
     * Loads the next page of older messages to fetch (if any). Adds the new page to the
     * UI message list, and remembers the new [Paging.nextToken].
     */
    fun loadMore() = scope.launch {
        isLoadingMore = true
        runCatching {
            if (nextToken == null) return@runCatching

            val page = agent.connections.messaging.listBasicMessages(
                ListBasicMessagesOptions(
                    filters = ListBasicMessagesFilter(connectionId = connectionId),
                    paging = Paging(PAGE_LIMIT.toUInt(), nextToken),
                    sorting = ListBasicMessagesSorting.Chronological(SortDirection.DESCENDING),
                ),
            )
            nextToken = page.nextToken
            messageList.addAll(page.items)
        }.showToastOnFailure(context, logger, "Failed to load next page")
        isLoadingMore = false
    }

    ConnectionChatScreenView(
        connection,
        messageList,
        isMoreToLoad = nextToken != null,
        isLoadingMore = isLoadingMore,
        sendMessage = { sendMessage(it) },
        loadOlderMessages = { loadMore() },
    )
}

/**
 * UI for the "Connection Chat screen". Allows previous messages to a connection to be seen, and
 * allows new messages to be sent and received in real-time.
 *
 * If there is more messages, the message list renders a "load more" button at the top of the list
 * which when clicked will fetch the next page of messages using the agent's pagination system.
 */
@Composable
private fun ConnectionChatScreenView(
    connection: Connection?,
    messages: List<BasicMessage>,
    isMoreToLoad: Boolean,
    isLoadingMore: Boolean,
    sendMessage: (String) -> Unit,
    loadOlderMessages: () -> Unit,
) {
    Column(
        Modifier
            .fillMaxSize(),
    ) {
        if (connection == null) {
            Column(
                Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                CircularProgressIndicator(Modifier.padding(vertical = 8.dp))
            }
        } else {
            Text(
                text = connection.theirLabel ?: connection.connectionId,
                Modifier
                    .fillMaxWidth()
                    .padding(vertical = SCREEN_PADDING),
                textAlign = TextAlign.Center,
                fontWeight = FontWeight.Bold,
            )
            Column(
                Modifier
                    .weight(1.0f)
                    .fillMaxSize(),
            ) {
                MessageList(
                    messages = messages,
                    isMoreToLoad,
                    isLoadingMore,
                    loadOlderMessages = loadOlderMessages,
                )
            }
            ChatInput(sendMessage)
        }
    }
}

@Composable
private fun MessageList(
    messages: List<BasicMessage>,
    isMoreToLoad: Boolean,
    isLoadingMore: Boolean,
    loadOlderMessages: () -> Unit,
) {
    val listState = rememberLazyListState()
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        state = listState,
        reverseLayout = true,
    ) {
        items(messages) { item ->
            ChatItem(item)
        }
        if (isMoreToLoad) {
            item {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                    if (isLoadingMore) {
                        CircularProgressIndicator()
                    } else {
                        Button(onClick = { loadOlderMessages() }) {
                            Text(text = "Load more")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ChatItem(message: BasicMessage) {
    val isOutbound = message is BasicMessage.Outbound
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(4.dp),
    ) {
        Box(
            modifier = Modifier
                .align(if (isOutbound) Alignment.End else Alignment.Start)
                .padding(
                    start = if (isOutbound) 48.dp else 0.dp,
                    end = if (isOutbound) 0.dp else 48.dp,
                )
                .clip(
                    RoundedCornerShape(
                        topStart = 48f,
                        topEnd = 48f,
                        bottomStart = if (isOutbound) 48f else 0f,
                        bottomEnd = if (isOutbound) 0f else 48f,
                    ),
                )
                .background(
                    if (isOutbound) {
                        MaterialTheme.colorScheme.primaryContainer
                    } else {
                        MaterialTheme.colorScheme.surfaceContainer
                    },
                )
                .padding(16.dp),
        ) {
            Text(text = message.content)
        }
    }
}

@Composable
private fun ChatInput(
    sendMessage: (String) -> Unit,
) {
    var chatBoxValue by remember { mutableStateOf(TextFieldValue("")) }

    fun onSendClick() {
        sendMessage(chatBoxValue.text)
        chatBoxValue = TextFieldValue("")
    }

    Row(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        TextField(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1.0f),
            shape = RoundedCornerShape(24.dp),
            value = chatBoxValue,
            onValueChange = { newText ->
                chatBoxValue = newText
            },
            colors = TextFieldDefaults.colors(
                focusedIndicatorColor = Color.Transparent,
                disabledIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
            ),
            placeholder = { Text("Type something") },
            trailingIcon = {
                IconButton(
                    modifier = Modifier
                        .clip(CircleShape),
                    onClick = { onSendClick() },
                    enabled = chatBoxValue.text.isNotEmpty(),
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Send,
                        contentDescription = "Send",
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(8.dp),
                    )
                }
            },
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun DefaultPreview() {
    SudoDIEdgeAgentExampleTheme {
        ConnectionChatScreenView(
            connection = Connection(
                "conn1",
                "connEx1",
                "Foo Bar",
                emptyList(),
            ),
            messages = (0 until 20).map { i ->
                if (i % 2 == 0) {
                    BasicMessage.Inbound(
                        i.toString(),
                        "",
                        "Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua. $i",
                        receivedTime = Instant.now(),
                        reportedSentTime = Instant.now(),
                    )
                } else {
                    BasicMessage.Outbound(
                        i.toString(),
                        "",
                        "Ut enim ad minim veniam, quis nostrud exercitation ullamco laboris nisi ut aliquip ex ea commodo consequat. $i",
                        sentTime = Instant.now(),
                    )
                }
            },
            isMoreToLoad = true,
            isLoadingMore = true,
            sendMessage = {},
            loadOlderMessages = {},
        )
    }
}
