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
import com.sudoplatform.sudodiedgeagent.credentials.types.Credential
import com.sudoplatform.sudodiedgeagent.credentials.types.CredentialSource
import com.sudoplatform.sudodiedgeagentexample.Routes
import com.sudoplatform.sudodiedgeagentexample.ui.theme.SCREEN_PADDING
import com.sudoplatform.sudodiedgeagentexample.ui.theme.SudoDIEdgeAgentExampleTheme
import com.sudoplatform.sudodiedgeagentexample.utils.PreviewDataHelper
import com.sudoplatform.sudodiedgeagentexample.utils.SwipeToDeleteCard
import com.sudoplatform.sudodiedgeagentexample.utils.showToastOnFailure
import com.sudoplatform.sudodiedgeagentexample.utils.swapList
import com.sudoplatform.sudologging.Logger
import kotlinx.coroutines.launch

@Composable
fun CredentialsScreen(
    navController: NavController,
    agent: SudoDIEdgeAgent,
    logger: Logger,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var isListLoading by remember { mutableStateOf(false) }
    val credentialList = remember { mutableStateListOf<UICredential>() }

    /**
     * Re-set the `credentialList` state to be the latest list of [Credential]s
     * fetched from the [SudoDIEdgeAgent] SDK.
     */
    fun refreshCredentialList() {
        scope.launch {
            isListLoading = true
            runCatching {
                val newList = agent.credentials.listAll().trySortByDateDescending().map {
                    UICredential.fromCredential(agent, it)
                }
                credentialList.swapList(newList)
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
                credentialList.removeIf { it.id == id }
            }.showToastOnFailure(context, logger)
        }
    }

    fun navigateToCredentialInfo(item: UICredential) {
        navController.navigate("${Routes.CREDENTIAL_INFO}/${item.id}")
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
    credentialList: List<UICredential>,
    refreshCredentialList: () -> Unit,
    deleteCredential: (id: String) -> Unit,
    showInfo: (item: UICredential) -> Unit,
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
                    items = credentialList,
                    key = { it.id },
                    itemContent = { item ->
                        val currentItem = rememberUpdatedState(item)
                        Box(modifier = Modifier.padding(vertical = 4.dp)) {
                            SwipeToDeleteCard(onDelete = {
                                deleteCredential(currentItem.value.id)
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
    item: UICredential,
    showInfo: () -> Unit,
) {
    Row(
        Modifier.padding(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1.0f)) {
            Text(
                text = item.id,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = item.previewName,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(text = item.previewFormat)
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
                PreviewDataHelper.dummyUICredentialAnoncred().copy(id = "1"),
                PreviewDataHelper.dummyUICredentialW3C().copy(
                    id = "2",
                    source = CredentialSource.OpenId4VcIssuer("https://issuer.john"),
                ),
            ),
            {},
            {},
            {},
        )
    }
}
