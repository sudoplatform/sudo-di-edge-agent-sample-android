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
import com.sudoplatform.sudodiedgeagent.credentials.types.JsonLdProofType
import com.sudoplatform.sudodiedgeagent.credentials.types.W3cCredential
import com.sudoplatform.sudodiedgeagentexample.utils.NameValueTextColumn
import com.sudoplatform.sudodiedgeagentexample.utils.NameValueTextRow

/**
 * Helper UI element for displaying the details of a w3c credential or credential exchange.
 */
@Composable
fun W3cCredentialInfoColumn(
    modifier: Modifier = Modifier,
    id: String,
    fromConnection: String,
    w3cCredential: W3cCredential,
    proofType: JsonLdProofType? = null,
) {
    val credSubjectId = w3cCredential.credentialSubject.firstOrNull()?.id ?: "None"
    val credSubjectAttributes =
        w3cCredential.credentialSubject.firstOrNull()?.properties?.entries?.sortedBy { it.key }?.toList()
            ?: emptyList()

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
                NameValueTextColumn("Format", "W3C")
                NameValueTextColumn("Issuer", w3cCredential.issuer.id)
                NameValueTextColumn("Issuance Date", w3cCredential.issuanceDate)
                NameValueTextColumn("Type", w3cCredential.types.find { it != "VerifiableCredential" } ?: "")
                proofType?.let {
                    NameValueTextColumn("Issuer Proof Type", it.toString())
                }
            }
            HorizontalDivider()

            Text(
                text = "Credential Subject",
                Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp),
                textAlign = TextAlign.Center,
                fontWeight = FontWeight.Bold,
            )
            NameValueTextRow("Subject Id", credSubjectId)
        }
        items(
            items = credSubjectAttributes,
            key = { it.key },
            itemContent = { item ->
                val currentItem = rememberUpdatedState(item)
                NameValueTextRow(
                    name = currentItem.value.key,
                    value = currentItem.value.value.toString(),
                )
            },
        )
    }
}
