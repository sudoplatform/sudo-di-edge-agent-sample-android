/*
 * Copyright © 2023 Anonyome Labs, Inc. All rights reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.sudoplatform.sudodiedgeagentexample.credential

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
import com.sudoplatform.sudodiedgeagent.credentials.types.AnoncredV1CredentialMetadata
import com.sudoplatform.sudodiedgeagent.credentials.types.Credential
import com.sudoplatform.sudodiedgeagent.credentials.types.CredentialDefinitionInfo
import com.sudoplatform.sudodiedgeagent.credentials.types.CredentialFormatData
import com.sudoplatform.sudodiedgeagent.credentials.types.CredentialIssuer
import com.sudoplatform.sudodiedgeagent.credentials.types.SchemaInfo
import com.sudoplatform.sudodiedgeagent.credentials.types.W3cCredential
import com.sudoplatform.sudodiedgeagentexample.Routes
import com.sudoplatform.sudodiedgeagentexample.ui.theme.SCREEN_PADDING
import com.sudoplatform.sudodiedgeagentexample.ui.theme.SudoDIEdgeAgentExampleTheme
import com.sudoplatform.sudodiedgeagentexample.utils.SwipeToDeleteCard
import com.sudoplatform.sudodiedgeagentexample.utils.showToastOnFailure
import com.sudoplatform.sudodiedgeagentexample.utils.swapList
import com.sudoplatform.sudologging.Logger
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonObject

@Composable
fun CredentialsScreen(
    navController: NavController,
    agent: SudoDIEdgeAgent,
    logger: Logger,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var isListLoading by remember { mutableStateOf(false) }
    val credentialList = remember { mutableStateListOf<Credential>() }

    /**
     * Re-set the `credentialList` state to be the latest list of [Credential]s
     * fetched from the [SudoDIEdgeAgent] SDK.
     */
    fun refreshCredentialList() {
        scope.launch {
            isListLoading = true
            runCatching {
                credentialList.swapList(agent.credentials.listAll())
            }.showToastOnFailure(context, logger)
            isListLoading = false
        }
    }

    /**
     * Delete a [Credential] by its ID, [Credential.credentialId],
     * Refreshing the displayed connection list if successful.
     */
    fun deleteCredential(id: String) {
        scope.launch {
            runCatching {
                agent.credentials.deleteById(id)
                credentialList.removeIf { it.credentialId == id }
            }.showToastOnFailure(context, logger)
        }
    }

    fun navigateToCredentialInfo(item: Credential) {
        navController.navigate("${Routes.CREDENTIAL_INFO}/${item.credentialId}")
    }

    /**
     * When this composable initializes, load the credential list
     */
    LaunchedEffect(key1 = Unit) {
        refreshCredentialList()
    }

    CredentialsScreenView(
        isListLoading,
        credentialList,
        refreshCredentialList = { refreshCredentialList() },
        deleteCredential = { deleteCredential(it) },
        showInfo = { navigateToCredentialInfo(it) },
    )
}

/**
 * UI for the "Credentials screen". Allows viewing and managing the [Credential]s held by the agent.
 */
@Composable
fun CredentialsScreenView(
    isListLoading: Boolean,
    credentialList: List<Credential>,
    refreshCredentialList: () -> Unit,
    deleteCredential: (id: String) -> Unit,
    showInfo: (item: Credential) -> Unit,
) {
    Column(
        Modifier
            .fillMaxSize()
            .padding(SCREEN_PADDING),
    ) {
        Text(
            text = "Credentials",
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
                    items = credentialList.trySortByDateDescending(),
                    key = { it.credentialId },
                    itemContent = { item ->
                        val currentItem = rememberUpdatedState(item)
                        Box(modifier = Modifier.padding(vertical = 4.dp)) {
                            SwipeToDeleteCard(onDelete = {
                                deleteCredential(currentItem.value.credentialId)
                            }) {
                                CredentialItemCardContent(currentItem.value, showInfo = {
                                    showInfo(currentItem.value)
                                })
                            }
                        }
                    },
                )
            }
        }
        Button(onClick = { refreshCredentialList() }, Modifier.fillMaxWidth()) {
            Text(text = "Refresh")
        }
    }
}

/**
 * Composable for displaying a [Credential] on a card.
 */
@Composable
private fun CredentialItemCardContent(
    item: Credential,
    showInfo: () -> Unit,
) {
    Row(
        Modifier.padding(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1.0f)) {
            Text(
                text = item.credentialId,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            val (credName, credFormatName) = when (val data = item.formatData) {
                is CredentialFormatData.AnoncredV1 -> Pair(
                    data.credentialMetadata.credentialDefinitionInfo?.name
                        ?: data.credentialMetadata.credentialDefinitionId,
                    "Anoncred",
                )

                is CredentialFormatData.W3C -> Pair(
                    data.credential.types.find { it != "VerifiableCredential" }
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
        CredentialsScreenView(
            isListLoading = false,
            listOf(
                Credential(
                    "1",
                    "conn1",
                    "John",
                    CredentialFormatData.AnoncredV1(
                        AnoncredV1CredentialMetadata(
                            "",
                            CredentialDefinitionInfo("Driver's License"),
                            "",
                            SchemaInfo("", ""),
                        ),
                        listOf(),
                    ),
                    listOf(),
                ),
                Credential(
                    "2",
                    "conn1",
                    "John",
                    CredentialFormatData.W3C(
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
                    ),
                    listOf(),
                ),
            ),
            {},
            {},
            {},
        )
    }
}
