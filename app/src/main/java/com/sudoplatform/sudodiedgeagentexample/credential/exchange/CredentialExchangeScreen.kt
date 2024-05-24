/*
 * Copyright © 2023 Anonyome Labs, Inc. All rights reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.sudoplatform.sudodiedgeagentexample.credential.exchange

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
import com.sudoplatform.sudodiedgeagent.credentials.exchange.types.CredentialExchange
import com.sudoplatform.sudodiedgeagent.credentials.exchange.types.CredentialExchangeFormatData
import com.sudoplatform.sudodiedgeagent.credentials.exchange.types.CredentialExchangeInitiator
import com.sudoplatform.sudodiedgeagent.credentials.exchange.types.CredentialExchangeState
import com.sudoplatform.sudodiedgeagent.credentials.types.AnoncredV1CredentialMetadata
import com.sudoplatform.sudodiedgeagent.credentials.types.CredentialDefinitionInfo
import com.sudoplatform.sudodiedgeagent.credentials.types.CredentialIssuer
import com.sudoplatform.sudodiedgeagent.credentials.types.JsonLdProofType
import com.sudoplatform.sudodiedgeagent.credentials.types.SchemaInfo
import com.sudoplatform.sudodiedgeagent.credentials.types.W3cCredential
import com.sudoplatform.sudodiedgeagent.subscriptions.AgentEventSubscriber
import com.sudoplatform.sudodiedgeagentexample.Routes
import com.sudoplatform.sudodiedgeagentexample.ui.theme.SCREEN_PADDING
import com.sudoplatform.sudodiedgeagentexample.ui.theme.SudoDIEdgeAgentExampleTheme
import com.sudoplatform.sudodiedgeagentexample.utils.SwipeToDeleteCard
import com.sudoplatform.sudodiedgeagentexample.utils.showToast
import com.sudoplatform.sudodiedgeagentexample.utils.showToastOnFailure
import com.sudoplatform.sudodiedgeagentexample.utils.swapList
import com.sudoplatform.sudologging.Logger
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonObject

@Composable
fun CredentialExchangeScreen(
    navController: NavController,
    agent: SudoDIEdgeAgent,
    logger: Logger,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var isListLoading by remember { mutableStateOf(false) }
    val credentialExchangeList = remember { mutableStateListOf<CredentialExchange>() }

    /**
     * Re-set the `credentialExchangeList` state to be the latest list of [CredentialExchange]s
     * fetched from the [SudoDIEdgeAgent] SDK.
     */
    fun refreshCredentialExchangeList() {
        scope.launch {
            isListLoading = true
            runCatching {
                credentialExchangeList.swapList(agent.credentials.exchange.listAll())
            }.showToastOnFailure(context, logger)
            isListLoading = false
        }
    }

    /**
     * Delete a [CredentialExchange] by its ID, [CredentialExchange.credentialExchangeId],
     * Refreshing the displayed connection exchange list if successful.
     */
    fun deleteCredentialExchange(id: String) {
        scope.launch {
            runCatching {
                agent.credentials.exchange.deleteById(id)
                credentialExchangeList.removeIf { it.credentialExchangeId == id }
            }.showToastOnFailure(context, logger)
        }
    }

    fun navigateToCredentialExchangeInfo(item: CredentialExchange) {
        navController.navigate("${Routes.CREDENTIAL_EXCHANGE_INFO}/${item.credentialExchangeId}")
    }

    /**
     * When this composable initializes, load the credential exchange list
     */
    LaunchedEffect(key1 = Unit) {
        refreshCredentialExchangeList()
    }

    /**
     * When this composable initializes, subscribe to credential exchange updates.
     * Whenever a credential exchange update occurs, refresh the whole list of displayed
     * [CredentialExchange]s.
     *
     * If the credential update is for the [CredentialExchangeState.ACKED] state, then
     * display a toast indicating that the credential has been stored.
     *
     * When the composable disposes, unsubscribe from the events.
     *
     * Note that refreshing the entire list for each event is NOT efficient, but is done
     * for simplicity demonstration purposes.
     */
    DisposableEffect(key1 = Unit) {
        val subscriptionId = agent.subscribeToAgentEvents(object : AgentEventSubscriber {
            override fun credentialExchangeStateChanged(credentialExchange: CredentialExchange) {
                refreshCredentialExchangeList()

                if (credentialExchange.state == CredentialExchangeState.ACKED) {
                    val msg =
                        "Credential Exchange completed. A new credential can be found in the 'Credentials' screen: ${credentialExchange.credentialId}"
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

    CredentialExchangeScreenView(
        isListLoading,
        credentialExchangeList,
        refreshCredentialExchangeList = { refreshCredentialExchangeList() },
        deleteCredentialExchange = { deleteCredentialExchange(it) },
        showInfo = { navigateToCredentialExchangeInfo(it) },
    )
}

/**
 * UI for the "Credential Exchange screen". Allows viewing and managing
 * the [CredentialExchange]s held by the agent (credentials mid-issuance).
 *
 * Clicking the "Info" button for a [CredentialExchange] item will navigate to the
 * [CredentialExchangeInfoScreen] where the details of that specific [CredentialExchange]
 * will be displayed.
 */
@Composable
fun CredentialExchangeScreenView(
    isListLoading: Boolean,
    credentialExchangeList: List<CredentialExchange>,
    refreshCredentialExchangeList: () -> Unit,
    deleteCredentialExchange: (id: String) -> Unit,
    showInfo: (item: CredentialExchange) -> Unit,
) {
    Column(
        Modifier
            .fillMaxSize()
            .padding(SCREEN_PADDING),
    ) {
        Text(
            text = "Credential Exchanges",
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
                    items = credentialExchangeList.trySortByDateDescending(),
                    key = { it.credentialExchangeId },
                    itemContent = { item ->
                        val currentItem = rememberUpdatedState(item)
                        Box(modifier = Modifier.padding(vertical = 4.dp)) {
                            SwipeToDeleteCard(onDelete = {
                                deleteCredentialExchange(currentItem.value.credentialExchangeId)
                            }) {
                                CredentialExchangeItemCardContent(
                                    currentItem.value,
                                    showInfo = {
                                        showInfo(currentItem.value)
                                    },
                                )
                            }
                        }
                    },
                )
            }
        }
        Button(
            onClick = { refreshCredentialExchangeList() },
            Modifier.fillMaxWidth(),
        ) {
            Text(text = "Refresh")
        }
    }
}

/**
 * Composable for displaying a [CredentialExchange] on a card to be displayed in a
 * list.
 */
@Composable
private fun CredentialExchangeItemCardContent(
    item: CredentialExchange,
    showInfo: () -> Unit,
) {
    Row(
        Modifier.padding(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1.0f)) {
            Text(
                text = item.credentialExchangeId,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            val (credName, credFormatName) = when (val data = item.formatData) {
                is CredentialExchangeFormatData.Indy -> Pair(
                    data.credentialMetadata.credentialDefinitionInfo?.name
                        ?: data.credentialMetadata.credentialDefinitionId,
                    "Anoncred",
                )

                is CredentialExchangeFormatData.AriesLdProof -> Pair(
                    data.currentProposedCredential.types.find { it != "VerifiableCredential" }
                        ?: "VerifiableCredential",
                    "W3C",
                )
            }
            Text(
                text = credName,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(text = credFormatName)
            Text(
                text = item.state.toString().lowercase()
                    .replaceFirstChar { it.uppercase() },
            )
        }
        Button(onClick = { showInfo() }) {
            Text(text = "Info")
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun DefaultPreview() {
    SudoDIEdgeAgentExampleTheme {
        CredentialExchangeScreenView(
            isListLoading = false,
            credentialExchangeList = listOf(
                CredentialExchange(
                    "credEx1",
                    "",
                    "",
                    CredentialExchangeInitiator.EXTERNAL,
                    CredentialExchangeState.ACKED,
                    CredentialExchangeFormatData.Indy(
                        AnoncredV1CredentialMetadata(
                            "",
                            CredentialDefinitionInfo("Driver's License"),
                            "",
                            SchemaInfo("", ""),
                        ),
                        listOf(),
                    ),
                    null,
                    listOf(),
                ),
                CredentialExchange(
                    "credEx2",
                    "",
                    "",
                    CredentialExchangeInitiator.EXTERNAL,
                    CredentialExchangeState.OFFER,
                    formatData = CredentialExchangeFormatData.AriesLdProof(
                        currentProposedCredential =
                        W3cCredential(
                            contexts = emptyList(),
                            id = null,
                            types = listOf("Foobar"),
                            credentialSubject = emptyList(),
                            issuer = CredentialIssuer("", JsonObject(emptyMap())),
                            issuanceDate = "",
                            expirationDate = null,
                            proof = null,
                            properties = JsonObject(emptyMap()),
                        ),
                        currentProposedProofType = JsonLdProofType.ED25519_SIGNATURE2018,
                    ),
                    null,
                    listOf(),
                ),
            ),
            {},
            {},
            {},
        )
    }
}
