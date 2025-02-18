/*
 * Copyright © 2024 Anonyome Labs, Inc. All rights reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.sudoplatform.sudodiedgeagentexample.dids

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
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
import androidx.compose.ui.window.Dialog
import com.sudoplatform.sudodiedgeagent.SudoDIEdgeAgent
import com.sudoplatform.sudodiedgeagent.dids.types.CreateDidOptions
import com.sudoplatform.sudodiedgeagent.dids.types.DidInformation
import com.sudoplatform.sudodiedgeagent.dids.types.DidKeyType
import com.sudoplatform.sudodiedgeagent.dids.types.DidMethod
import com.sudoplatform.sudodiedgeagent.plugins.externalcryptoprovider.hardware.AndroidHardwareCryptoProvider
import com.sudoplatform.sudodiedgeagent.types.CreateKeyPairEnclaveOptions
import com.sudoplatform.sudodiedgeagent.types.RecordTag
import com.sudoplatform.sudodiedgeagentexample.ui.theme.SCREEN_PADDING
import com.sudoplatform.sudodiedgeagentexample.ui.theme.SudoDIEdgeAgentExampleTheme
import com.sudoplatform.sudodiedgeagentexample.utils.NameValueTextRow
import com.sudoplatform.sudodiedgeagentexample.utils.SwipeToDeleteCard
import com.sudoplatform.sudodiedgeagentexample.utils.showToastOnFailure
import com.sudoplatform.sudodiedgeagentexample.utils.swapList
import com.sudoplatform.sudologging.Logger
import kotlinx.coroutines.launch

@Composable
fun DidsScreen(
    agent: SudoDIEdgeAgent,
    logger: Logger,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var isListLoading by remember { mutableStateOf(false) }
    val didList = remember { mutableStateListOf<DidInformation>() }

    /**
     * Re-set the `didsList` state to be the latest list of [DidInformation]s
     * fetched from the [SudoDIEdgeAgent] SDK.
     */
    fun refreshDidList() = scope.launch {
        isListLoading = true
        kotlin.runCatching {
            didList.swapList(agent.dids.listAll())
        }.showToastOnFailure(context, logger)
        isListLoading = false
    }

    /**
     * Delete a [DidInformation] by its DID,
     * Refreshing the displayed DID list if successful.
     */
    fun deleteDid(id: String) = scope.launch {
        runCatching {
            agent.dids.deleteById(id)
            didList.removeIf { it.did == id }
        }.showToastOnFailure(context, logger)
    }

    /**
     * Create a new DID, use custom logic to assign an [alias] to the [DidInformation]s
     * metadata.
     * Refreshing the displayed DID list if successful.
     */
    fun createDid(opts: CreateDidOptions, alias: String) = scope.launch {
        runCatching {
            val did = agent.dids.createDid(opts)
            assignAliasToDid(agent, did, alias)
            refreshDidList()
        }.showToastOnFailure(context, logger)
    }

    /**
     * When this composable initializes, load the connection list
     */
    LaunchedEffect(key1 = Unit) {
        refreshDidList()
    }

    DidsScreenView(
        isListLoading = isListLoading,
        didList = didList,
        refreshDidList = { refreshDidList() },
        deleteDid = { deleteDid(it) },
        createDid = { opts, alias -> createDid(opts, alias) },
    )
}

/**
 * UI for the "DIDs screen". Allows viewing and managing the DIDs held by the agent.
 */
@Composable
fun DidsScreenView(
    isListLoading: Boolean,
    didList: List<DidInformation>,
    refreshDidList: () -> Unit,
    deleteDid: (id: String) -> Unit,
    createDid: (opts: CreateDidOptions, alias: String) -> Unit,
) {
    var openCreateDidDialog by remember { mutableStateOf(false) }

    if (openCreateDidDialog) {
        Dialog(onDismissRequest = { openCreateDidDialog = false }) {
            CreateDidDialogContent(createDid = { opts, alias ->
                createDid(opts, alias)
                openCreateDidDialog = false
            })
        }
    }

    Column(
        Modifier
            .fillMaxSize()
            .padding(SCREEN_PADDING),
    ) {
        Text(
            text = "DIDs",
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
                    items = didList.trySortByDateDescending(),
                    key = { it.did },
                    itemContent = { item ->
                        val currentItem = rememberUpdatedState(item)
                        Box(modifier = Modifier.padding(vertical = 4.dp)) {
                            SwipeToDeleteCard(onDelete = {
                                deleteDid(currentItem.value.did)
                            }) {
                                DidItemCardContent(currentItem.value)
                            }
                        }
                    },
                )
            }
        }
        Button(onClick = { refreshDidList() }, Modifier.fillMaxWidth()) {
            Text(text = "Refresh")
        }
        Button(onClick = { openCreateDidDialog = true }, Modifier.fillMaxWidth()) {
            Text(text = "Create DID")
        }
    }
}

@Composable
private fun DidItemCardContent(
    item: DidInformation,
) {
    Row(
        Modifier.padding(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1.0f)) {
            Text(
                text = item.did,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            NameValueTextRow("alias", item.alias() ?: "None")
            NameValueTextRow("keyType", item.keyType().toString())
        }
    }
}

sealed interface CreateDidContentState {
    data object SelectMethod : CreateDidContentState

    data class SelectKeyType(val method: DidMethod) : CreateDidContentState

    data class SelectEnclaveType(
        val method: DidMethod,
        val keyType: DidKeyType,
    ) : CreateDidContentState

    data class EnterAlias(
        val method: DidMethod,
        val keyType: DidKeyType,
        val enclaveType: CreateKeyPairEnclaveOptions,
    ) : CreateDidContentState

    data class ReadyToCreate(
        val method: DidMethod,
        val keyType: DidKeyType,
        val enclaveType: CreateKeyPairEnclaveOptions,
        val alias: String,
    ) : CreateDidContentState {
        fun asOptions(): CreateDidOptions =
            when (method) {
                DidMethod.DID_KEY -> CreateDidOptions.DidKey(
                    keyType = keyType,
                    enclaveOptions = enclaveType,
                )

                DidMethod.DID_JWK -> CreateDidOptions.DidJwk(
                    keyType = keyType,
                    enclaveOptions = enclaveType,
                )
            }
    }
}

@Composable
private fun CreateDidDialogContent(
    createDid: (opts: CreateDidOptions, alias: String) -> Unit,
) {
    var state: CreateDidContentState by remember { mutableStateOf(CreateDidContentState.SelectMethod) }
    var aliasInput by remember { mutableStateOf("") }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp)
            .padding(16.dp),
        shape = RoundedCornerShape(16.dp),
    ) {
        when (val currentState = state) {
            is CreateDidContentState.SelectMethod -> {
                Text(
                    text = "Select a DID Method",
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                    textAlign = TextAlign.Center,
                    fontWeight = FontWeight.Bold,
                )
                Column(
                    Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Button(onClick = {
                        state = CreateDidContentState.SelectKeyType(DidMethod.DID_KEY)
                    }) {
                        Text("did:key")
                    }
                    Button(onClick = {
                        state = CreateDidContentState.SelectKeyType(DidMethod.DID_JWK)
                    }) {
                        Text("did:jwk")
                    }
                }
            }

            is CreateDidContentState.SelectKeyType -> {
                Text(
                    text = "Select a Key Type",
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                    textAlign = TextAlign.Center,
                    fontWeight = FontWeight.Bold,
                )
                Column(
                    Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Button(onClick = {
                        state = CreateDidContentState.SelectEnclaveType(
                            currentState.method,
                            DidKeyType.P256,
                        )
                    }) {
                        Text("P256")
                    }
                    Button(onClick = {
                        state = CreateDidContentState.SelectEnclaveType(
                            currentState.method,
                            DidKeyType.ED25519,
                        )
                    }) {
                        Text("Ed25519")
                    }
                }
            }

            is CreateDidContentState.SelectEnclaveType -> {
                Text(
                    text = "Select an Enclave Type",
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                    textAlign = TextAlign.Center,
                    fontWeight = FontWeight.Bold,
                )
                Column(
                    Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Button(onClick = {
                        state = CreateDidContentState.EnterAlias(
                            currentState.method,
                            currentState.keyType,
                            CreateKeyPairEnclaveOptions.Internal,
                        )
                    }) {
                        Text("Software")
                    }
                    Button(onClick = {
                        state = CreateDidContentState.EnterAlias(
                            currentState.method,
                            currentState.keyType,
                            CreateKeyPairEnclaveOptions.External(
                                AndroidHardwareCryptoProvider::class,
                                AndroidHardwareCryptoProvider.CreateKeyPairOptions(),
                            ),
                        )
                    }) {
                        Text("Android Hardware")
                    }
                }
            }

            is CreateDidContentState.EnterAlias -> {
                Text(
                    text = "Enter an alias for this DID",
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                    textAlign = TextAlign.Center,
                    fontWeight = FontWeight.Bold,
                )
                Column(
                    Modifier
                        .fillMaxSize()
                        .padding(8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    TextField(
                        modifier = Modifier.fillMaxWidth(),
                        value = aliasInput,
                        onValueChange = { newText ->
                            aliasInput = newText
                        },
                        placeholder = { Text(text = "Enter an alias...") },
                        singleLine = true,
                    )
                    Button(onClick = {
                        state = CreateDidContentState.ReadyToCreate(
                            currentState.method,
                            currentState.keyType,
                            currentState.enclaveType,
                            alias = aliasInput,
                        )
                    }) {
                        Text("Next")
                    }
                }
            }

            is CreateDidContentState.ReadyToCreate -> {
                Column(
                    Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Button(onClick = { createDid(currentState.asOptions(), currentState.alias) }) {
                        Text("Create")
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun DefaultPreview() {
    SudoDIEdgeAgentExampleTheme {
        DidsScreenView(
            isListLoading = false,
            listOf(
                DidInformation(
                    did = "did:key:bar",
                    methodData = DidInformation.MethodData.DidKey(DidKeyType.P256),
                    tags = listOf(RecordTag("alias", "School DID")),
                ),
                DidInformation(
                    did = "did:key:foo",
                    methodData = DidInformation.MethodData.DidKey(DidKeyType.ED25519),
                    tags = listOf(RecordTag("alias", "Work DID")),
                ),
                DidInformation(
                    did = "did:jwk:bar",
                    methodData = DidInformation.MethodData.DidJwk(DidKeyType.P256),
                    tags = listOf(),
                ),
            ),
            {},
            {},
            { _, _ -> },
        )
    }
}
