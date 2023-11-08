/*
 * Copyright © 2023 Anonyome Labs, Inc. All rights reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.sudoplatform.sudodiedgeagentexample.credential.exchange

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import com.sudoplatform.sudodiedgeagent.credentials.exchange.types.AcceptCredentialOfferConfiguration
import com.sudoplatform.sudodiedgeagent.credentials.exchange.types.CredentialExchange
import com.sudoplatform.sudodiedgeagent.credentials.exchange.types.CredentialExchangeInitiator
import com.sudoplatform.sudodiedgeagent.credentials.exchange.types.CredentialExchangeState
import com.sudoplatform.sudodiedgeagent.credentials.types.CredentialAttribute
import com.sudoplatform.sudodiedgeagent.credentials.types.CredentialDefinitionInfo
import com.sudoplatform.sudodiedgeagent.credentials.types.CredentialMetadata
import com.sudoplatform.sudodiedgeagent.credentials.types.SchemaInfo
import com.sudoplatform.sudodiedgeagentexample.credential.CredentialInfoColumn
import com.sudoplatform.sudodiedgeagentexample.ui.theme.SCREEN_PADDING
import com.sudoplatform.sudodiedgeagentexample.ui.theme.SudoDIEdgeAgentExampleTheme
import com.sudoplatform.sudodiedgeagentexample.utils.showToastOnFailure
import com.sudoplatform.sudologging.Logger
import kotlinx.coroutines.launch

@Composable
fun CredentialExchangeInfoScreen(
    navController: NavController,
    credentialExchangeId: String,
    agent: SudoDIEdgeAgent,
    logger: Logger,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var credentialExchange: CredentialExchange? by remember { mutableStateOf(null) }
    var isAcceptingCredential by remember { mutableStateOf(false) }

    /**
     * Attempt to accept this [CredentialExchange] in the offer state.
     *
     * On success, navigate one screen backwards.
     */
    fun acceptCredential() {
        scope.launch {
            isAcceptingCredential = true
            runCatching {
                agent.credentials.exchange.acceptOffer(
                    credentialExchangeId,
                    AcceptCredentialOfferConfiguration(autoStoreCredential = true),
                )
                navController.popBackStack()
            }.showToastOnFailure(context, logger, "Failed to accept credential offer")
            isAcceptingCredential = false
        }
    }

    /**
     * When this composable initializes, load the [CredentialExchange] from the ID that was
     * passed in. Displaying an error toast if the [CredentialExchange] cannot be found in the
     * agent (should be logically impossible/unlikely).
     */
    LaunchedEffect(key1 = Unit) {
        runCatching {
            val loadedCredEx = agent.credentials.exchange.getById(credentialExchangeId)
                ?: throw Exception("Could not find credential exchange")
            credentialExchange = loadedCredEx
        }.showToastOnFailure(context, logger, "Failed to load credential exchange")
    }

    CredentialExchangeInfoScreenView(
        credentialExchange,
        acceptCredential = { acceptCredential() },
        isAcceptingCredential,
    )
}

/**
 * UI for the "Credential Exchange Info Screen". Shows the details of a given [CredentialExchange].
 *
 * UI will display a loading spinner until [credentialExchange] becomes non-null.
 *
 * When the [CredentialExchange] is in the [CredentialExchangeState.OFFER] state, then display
 * an "Accept" button, which upon clicking will inform the issuer that the agent would like to accept
 * the offer.
 */
@Composable
fun CredentialExchangeInfoScreenView(
    credentialExchange: CredentialExchange?,
    acceptCredential: () -> Unit,
    isAccepting: Boolean,
) {
    Column(
        Modifier
            .fillMaxSize()
            .padding(SCREEN_PADDING),
    ) {
        if (isAccepting || credentialExchange == null) {
            Column(
                Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                CircularProgressIndicator(Modifier.padding(vertical = 8.dp))
                if (isAccepting) {
                    Text("Accepting...")
                }
            }
        } else {
            CredentialInfoColumn(
                Modifier.weight(1.0f),
                id = credentialExchange.credentialExchangeId,
                fromConnection = credentialExchange.connectionId,
                metadata = credentialExchange.credentialMetadata,
                attributes = credentialExchange.credentialAttributes,
            )
            if (credentialExchange.state == CredentialExchangeState.OFFER) {
                Button(
                    onClick = { acceptCredential() },
                    Modifier
                        .fillMaxWidth(),
                ) {
                    Text(text = "Accept")
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun DefaultPreview() {
    SudoDIEdgeAgentExampleTheme {
        CredentialExchangeInfoScreenView(
            credentialExchange = CredentialExchange(
                "credEx1",
                null,
                "conn1",
                CredentialExchangeInitiator.EXTERNAL,
                CredentialExchangeState.OFFER,
                CredentialMetadata(
                    "credDef1",
                    CredentialDefinitionInfo("My Cred Def 1"),
                    "schema1",
                    SchemaInfo("My Schema 1", "1.0"),
                ),
                listOf(
                    CredentialAttribute("Attribute 1", "Value 1", null),
                    CredentialAttribute("Attribute 2", "Value 2", null),
                ),
                null,
                listOf(),
            ),
            {},
            false,
        )
    }
}
