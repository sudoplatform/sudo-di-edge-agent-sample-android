/*
 * Copyright © 2023 Anonyome Labs, Inc. All rights reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.sudoplatform.sudodiedgeagentexample.credential

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.sudoplatform.sudodiedgeagent.credentials.types.AnoncredV1CredentialAttribute
import com.sudoplatform.sudodiedgeagent.credentials.types.AnoncredV1CredentialMetadata
import com.sudoplatform.sudodiedgeagentexample.utils.NameValueTextColumn
import com.sudoplatform.sudodiedgeagentexample.utils.NameValueTextRow

/**
 * Helper UI element for displaying the details of an anoncred credential or credential exchange.
 */
@Composable
fun AnoncredCredentialInfoColumn(
    modifier: Modifier = Modifier,
    id: String,
    fromConnection: String,
    metadata: AnoncredV1CredentialMetadata,
    attributes: List<AnoncredV1CredentialAttribute>,
) {
    LazyColumn(modifier) {
        item {
            Text(
                text = "Info",
                Modifier
                    .fillMaxWidth(),
                textAlign = TextAlign.Center,
                fontWeight = FontWeight.Bold,
            )
            Column(
                Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp),
            ) {
                NameValueTextColumn("ID", id)
                NameValueTextColumn("From Connection", fromConnection)
                NameValueTextColumn("Format", "Anoncreds")
                NameValueTextColumn("Cred Def ID", metadata.credentialDefinitionId)
                NameValueTextColumn(
                    "Cred Def Name",
                    metadata.credentialDefinitionInfo?.name ?: "Unknown",
                )
                NameValueTextColumn("Schema ID", metadata.schemaId)
            }
            HorizontalDivider()

            Text(
                text = "Attributes",
                Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp),
                textAlign = TextAlign.Center,
                fontWeight = FontWeight.Bold,
            )
        }
        items(
            items = attributes.sortAlphabetically(),
            key = { it.name },
            itemContent = { item ->
                val currentItem = rememberUpdatedState(item)
                NameValueTextRow(
                    name = currentItem.value.name,
                    value = currentItem.value.value,
                )
            },
        )
    }
}
