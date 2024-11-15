/*
 * Copyright © 2023 Anonyome Labs, Inc. All rights reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.sudoplatform.sudodiedgeagentexample.credential

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.sudoplatform.sudodiedgeagent.credentials.types.CredentialSource
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
    fromSource: CredentialSource,
    w3cCredential: W3cCredential,
    proofType: JsonLdProofType? = null,
) {
    Column(modifier) {
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
            when (fromSource) {
                is CredentialSource.DidCommConnection -> NameValueTextColumn(
                    "From Connection",
                    fromSource.connectionId,
                )

                is CredentialSource.OpenId4VcIssuer -> NameValueTextColumn(
                    "From OID Issuer",
                    fromSource.issuerUrl,
                )
            }
            NameValueTextColumn("Format", "W3C")
            NameValueTextColumn("Issuer", w3cCredential.issuer.id)
            NameValueTextColumn("Issuance Date", w3cCredential.issuanceDate)
            NameValueTextColumn(
                "Type",
                w3cCredential.types.find { it != "VerifiableCredential" } ?: "",
            )
            proofType?.let {
                NameValueTextColumn("Issuer Proof Type", it.toString())
            }
        }
        w3cCredential.credentialSubject.forEachIndexed { i, credSubject ->
            HorizontalDivider()

            Text(
                text = "Credential Subject #$i",
                Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp),
                textAlign = TextAlign.Center,
                fontWeight = FontWeight.Bold,
            )
            NameValueTextRow("Subject Id", credSubject.id ?: "N/A")

            credSubject.properties.entries.sortedBy { it.key }.forEach { (k, v) ->
                NameValueTextRow(name = k, value = v.toString())
            }
        }
    }
}
