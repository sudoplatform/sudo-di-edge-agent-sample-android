/*
 * Copyright © 2023 Anonyome Labs, Inc. All rights reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.sudoplatform.sudodiedgeagentexample.proof.exchanges

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
import com.sudoplatform.sudodiedgeagent.proofs.exchange.types.ProofExchange
import com.sudoplatform.sudodiedgeagent.proofs.exchange.types.ProofExchangeState
import com.sudoplatform.sudodiedgeagent.subscriptions.AgentEventSubscriber
import com.sudoplatform.sudodiedgeagentexample.Routes
import com.sudoplatform.sudodiedgeagentexample.ui.theme.SCREEN_PADDING
import com.sudoplatform.sudodiedgeagentexample.ui.theme.SudoDIEdgeAgentExampleTheme
import com.sudoplatform.sudodiedgeagentexample.utils.SwipeToDeleteCard
import com.sudoplatform.sudodiedgeagentexample.utils.showToastOnFailure
import com.sudoplatform.sudodiedgeagentexample.utils.swapList
import com.sudoplatform.sudologging.Logger
import kotlinx.coroutines.launch

@Composable
fun ProofExchangeScreen(
    navController: NavController,
    agent: SudoDIEdgeAgent,
    logger: Logger,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var isListLoading by remember { mutableStateOf(false) }
    val proofExchangeList = remember { mutableStateListOf<ProofExchange>() }

    /**
     * Re-set the `proofExchangeList` state to be the latest list of [ProofExchange]s
     * fetched from the [SudoDIEdgeAgent] SDK.
     */
    fun refreshProofExchangeList() {
        scope.launch {
            isListLoading = true
            runCatching {
                proofExchangeList.swapList(agent.proofs.exchange.listAll())
            }.showToastOnFailure(context, logger)
            isListLoading = false
        }
    }

    /**
     * Delete a [ProofExchange] by its ID, [ProofExchange.proofExchangeId],
     * Refreshing the displayed connection exchange list if successful.
     */
    fun deleteProofExchange(id: String) {
        scope.launch {
            runCatching {
                agent.proofs.exchange.deleteById(id)
                proofExchangeList.removeIf { it.proofExchangeId == id }
            }.showToastOnFailure(context, logger)
        }
    }

    fun navigateToProofExchangePresentation(item: ProofExchange) {
        navController.navigate("${Routes.PROOF_EXCHANGE_PRESENTATION}/${item.proofExchangeId}")
    }

    /**
     * When this composable initializes, load the proof exchange list
     */
    LaunchedEffect(key1 = Unit) {
        refreshProofExchangeList()
    }

    /**
     * When this composable initializes, subscribe to proof exchange updates.
     * Whenever a proof exchange update occurs, refresh the whole list of displayed
     * [ProofExchange]s.
     *
     * When the composable disposes, unsubscribe from the events.
     *
     * Note that refreshing the entire list for each event is NOT efficient, but is done
     * for simplicity demonstration purposes.
     */
    DisposableEffect(key1 = Unit) {
        val subscriptionId = agent.subscribeToAgentEvents(object : AgentEventSubscriber {
            override fun proofExchangeStateChanged(proofExchange: ProofExchange) {
                refreshProofExchangeList()
            }
        })

        onDispose {
            agent.unsubscribeToAgentEvents(subscriptionId)
        }
    }

    ProofExchangeScreenView(
        isListLoading,
        proofExchangeList,
        refreshProofExchangeList = { refreshProofExchangeList() },
        deleteProofExchange = { deleteProofExchange(it) },
        showPresentationInfo = { navigateToProofExchangePresentation(it) },
    )
}

/**
 * UI for the "Proof Exchange screen". Allows viewing and managing
 * the [ProofExchange]s held by the agent.
 *
 * For [ProofExchange]s in the [ProofExchangeState.REQUEST] state, a "Present" button
 * will be shown, which when clicked will navigate to the [ProofExchangePresentationScreen]
 * where the presentation of that specific [ProofExchange] will be displayed.
 */
@Composable
fun ProofExchangeScreenView(
    isListLoading: Boolean,
    proofExchangeList: List<ProofExchange>,
    refreshProofExchangeList: () -> Unit,
    deleteProofExchange: (id: String) -> Unit,
    showPresentationInfo: (item: ProofExchange) -> Unit,
) {
    Column(
        Modifier
            .fillMaxSize()
            .padding(SCREEN_PADDING),
    ) {
        Text(
            text = "Proof Exchanges",
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
                    items = proofExchangeList.trySortByDateDescending(),
                    key = { it.proofExchangeId },
                    itemContent = { item ->
                        val currentItem = rememberUpdatedState(item)
                        Box(modifier = Modifier.padding(vertical = 4.dp)) {
                            SwipeToDeleteCard(onDelete = {
                                deleteProofExchange(currentItem.value.proofExchangeId)
                            }) {
                                ProofExchangeItemCardContent(
                                    currentItem.value,
                                    showPresentationInfo = {
                                        showPresentationInfo(currentItem.value)
                                    },
                                )
                            }
                        }
                    },
                )
            }
        }
        Button(
            onClick = { refreshProofExchangeList() },
            Modifier.fillMaxWidth(),
        ) {
            Text(text = "Refresh")
        }
    }
}

/**
 * Composable for displaying a [ProofExchange] on a card to be displayed in a list.
 */
@Composable
private fun ProofExchangeItemCardContent(
    item: ProofExchange,
    showPresentationInfo: () -> Unit,
) {
    Row(
        Modifier.padding(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1.0f)) {
            Text(
                text = item.proofExchangeId,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = item.state.toString().lowercase()
                    .replaceFirstChar { it.uppercase() },
            )
        }
        if (item.state == ProofExchangeState.REQUEST) {
            Button(onClick = { showPresentationInfo() }) {
                Text(text = "Present")
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun DefaultPreview() {
    SudoDIEdgeAgentExampleTheme {
        ProofExchangeScreenView(
            isListLoading = false,
            proofExchangeList = listOf(),
            {},
            {},
            {},
        )
    }
}
