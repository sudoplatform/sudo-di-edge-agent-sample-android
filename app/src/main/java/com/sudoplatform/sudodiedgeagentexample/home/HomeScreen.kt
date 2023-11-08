/*
 * Copyright © 2023 Anonyome Labs, Inc. All rights reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.sudoplatform.sudodiedgeagentexample.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
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
import androidx.navigation.NavController
import com.sudoplatform.sudodiedgeagent.SudoDIEdgeAgent
import com.sudoplatform.sudodiedgeagent.connections.exchange.types.ConnectionExchange
import com.sudoplatform.sudodiedgeagent.credentials.exchange.types.CredentialExchange
import com.sudoplatform.sudodiedgeagent.proofs.exchange.types.ProofExchange
import com.sudoplatform.sudodiedgeagent.subscriptions.AgentEventSubscriber
import com.sudoplatform.sudodiedgeagentexample.Routes
import com.sudoplatform.sudodiedgeagentexample.SingleSudoManager
import com.sudoplatform.sudodiedgeagentexample.ui.theme.SCREEN_PADDING
import com.sudoplatform.sudodiedgeagentexample.ui.theme.SudoDIEdgeAgentExampleTheme
import com.sudoplatform.sudodiedgeagentexample.utils.showToast
import com.sudoplatform.sudodiedgeagentexample.utils.showToastOnFailure
import com.sudoplatform.sudologging.Logger
import kotlinx.coroutines.launch

@Composable
fun HomeScreen(
    navController: NavController,
    agent: SudoDIEdgeAgent,
    sudoManager: SingleSudoManager,
    logger: Logger,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var isAgentRunning by remember { mutableStateOf(agent.isRunning()) }

    /**
     * Toggle the agent's run loop on and off. When toggled on, the agent is ran
     * using the [sudoManager]'s instance of the Sudo DI Relay Message Source,
     * meaning that the agent will use the relay service to receive messages.
     *
     * Note that in a real application, it's more likely that an agent's run loop
     * will always be running, as such, this toggling ability is an unlikely feature,
     * it is within this application simply for sandbox-ing purposes.
     */
    fun toggleAgentRunning() = scope.launch {
        runCatching {
            isAgentRunning = if (isAgentRunning) {
                agent.stop()
                false
            } else {
                agent.run(sudoManager.relayMessageSource)
                true
            }
        }.showToastOnFailure(context, logger, "Failed to toggle agent state")
    }

    /**
     * When this composable initializes, listen for agent events and display toasts for each event.
     *
     * When the composable disposes, unsubscribe from the agent events.
     */
    DisposableEffect(key1 = Unit) {
        val subscriptionId = agent.subscribeToAgentEvents(object : AgentEventSubscriber {
            override fun connectionExchangeStateChanged(connectionExchange: ConnectionExchange) {
                val msg =
                    "Connection updated: ${connectionExchange.connectionExchangeId}, state: ${connectionExchange.state}"
                scope.launch { showToast(msg, context) }
            }

            override fun credentialExchangeStateChanged(credentialExchange: CredentialExchange) {
                val msg =
                    "Credential exchange updated: ${credentialExchange.credentialExchangeId}, state: ${credentialExchange.state}"
                scope.launch { showToast(msg, context) }
            }

            override fun proofExchangeStateChanged(proofExchange: ProofExchange) {
                val msg =
                    "Proof exchange updated: ${proofExchange.proofExchangeId}, state: ${proofExchange.state}"
                scope.launch { showToast(msg, context) }
            }
        })

        onDispose {
            agent.unsubscribeToAgentEvents(subscriptionId)
        }
    }

    HomeScreenView(
        toggleAgentRunning = { toggleAgentRunning() },
        isAgentRunning = isAgentRunning,
        navigateTo = { navController.navigate(it) },
    )
}

/**
 * UI for the "Home screen". Allows toggling of the Agent's run loop on and off,
 * and navigation to several of the sub pages.
 */
@Composable
fun HomeScreenView(
    toggleAgentRunning: () -> Unit,
    isAgentRunning: Boolean,
    navigateTo: (route: String) -> Unit,
) {
    Column(
        Modifier
            .fillMaxSize()
            .padding(horizontal = SCREEN_PADDING),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterVertically),
    ) {
        Button(onClick = toggleAgentRunning, Modifier.fillMaxWidth()) {
            Text(text = if (isAgentRunning) "Stop Agent" else "Run Agent")
        }
        Button(onClick = { navigateTo(Routes.CONNECTION_EXCHANGES) }, Modifier.fillMaxWidth()) {
            Text(text = "Connection Exchanges")
        }
        Button(onClick = { navigateTo(Routes.CONNECTIONS) }, Modifier.fillMaxWidth()) {
            Text(text = "Connections")
        }
        Button(onClick = { navigateTo(Routes.CREDENTIAL_EXCHANGES) }, Modifier.fillMaxWidth()) {
            Text(text = "Credential Exchanges")
        }
        Button(onClick = { navigateTo(Routes.CREDENTIALS) }, Modifier.fillMaxWidth()) {
            Text(text = "Credentials")
        }
        Button(onClick = { navigateTo(Routes.PROOF_EXCHANGES) }, Modifier.fillMaxWidth()) {
            Text(text = "Proof Exchanges")
        }
    }
}

@Preview(showBackground = true)
@Composable
fun DefaultPreview() {
    SudoDIEdgeAgentExampleTheme {
        HomeScreenView({}, true, {})
    }
}
