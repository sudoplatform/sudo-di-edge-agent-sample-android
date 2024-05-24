/*
 * Copyright © 2023 Anonyome Labs, Inc. All rights reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.sudoplatform.sudodiedgeagentexample.proof.exchanges.anoncred

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.sudoplatform.sudodiedgeagent.credentials.types.AnoncredV1CredentialAttribute
import com.sudoplatform.sudodiedgeagentexample.proof.exchanges.RetrievedCredentialsForAnoncredItem
import com.sudoplatform.sudodiedgeagentexample.ui.theme.SCREEN_PADDING
import com.sudoplatform.sudodiedgeagentexample.utils.NameValueTextRow

/**
 * Modal bottom sheet view for displaying the list of credentials which are appropriate
 * for presenting a requested [item].
 *
 * Displays the credentials as cards, showing their ID and showing the relevant attributes
 * which are being requested from the given credential.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SelectCredentialForAnoncredItemModal(
    onDismissRequest: () -> Unit,
    item: RetrievedCredentialsForAnoncredItem,
    onSelect: (credentialId: String) -> Unit,
    /**
     * Map of relevant credential IDs to the list of [AnoncredV1CredentialAttribute]s
     * belonging to that credential
     */
    attributesByCredentialId: Map<String, List<AnoncredV1CredentialAttribute>>,
) {
    /**
     * For a given credentialId, get the list of [AnoncredV1CredentialAttribute] of this credential
     * which are relevant to the presentation [item].
     *
     * For instance, if [item] is requesting attributes "A" & "B", only [AnoncredV1CredentialAttribute]s
     * for "A" & "B" should be returned.
     */
    fun getRelevantCredentialAttributes(credentialId: String): List<AnoncredV1CredentialAttribute> {
        val relevantAttributeNames = item.requestedAttributeNames()
        val allAttributes = attributesByCredentialId[credentialId] ?: listOf()
        return allAttributes.filter { relevantAttributeNames.contains(it.name) }
    }

    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = SheetState(skipPartiallyExpanded = true, density = LocalDensity.current),
    ) {
        LazyColumn(
            Modifier
                .fillMaxSize()
                .padding(SCREEN_PADDING),
        ) {
            item {
                Text(
                    text = "Select a credential to present this item with",
                    Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp),
                    textAlign = TextAlign.Center,
                    fontWeight = FontWeight.Bold,
                )
            }
            if (item.suitableCredentialIds.isEmpty()) {
                item {
                    Text(
                        text = "No suitable credentials found.",
                        Modifier.fillMaxWidth().padding(vertical = 16.dp),
                        textAlign = TextAlign.Center,
                    )
                }
            }
            items(
                items = item.suitableCredentialIds,
                key = { it },
            ) { item ->
                val currentItem = rememberUpdatedState(item)
                val relevantAttributes = getRelevantCredentialAttributes(currentItem.value)

                Card(Modifier.padding(vertical = 4.dp)) {
                    Row(
                        Modifier.padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(Modifier.weight(1.0f)) {
                            NameValueTextRow(
                                name = "ID",
                                value = currentItem.value,
                                handleValueTextOverflow = true,
                            )
                            Text("Attributes:")
                            relevantAttributes.forEach {
                                NameValueTextRow(name = it.name, value = it.value)
                            }
                            Button(
                                onClick = { onSelect(currentItem.value) },
                                Modifier
                                    .fillMaxWidth()
                                    .padding(top = 8.dp),
                            ) {
                                Text(text = "Select")
                            }
                        }
                    }
                }
            }
        }
    }
}
