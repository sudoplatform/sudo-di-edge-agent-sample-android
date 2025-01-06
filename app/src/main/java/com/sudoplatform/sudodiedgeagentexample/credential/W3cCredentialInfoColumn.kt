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
import com.sudoplatform.sudodiedgeagentexample.utils.NameValueTextColumn
import com.sudoplatform.sudodiedgeagentexample.utils.NameValueTextRow

/**
 * Helper UI element for displaying the details of a w3c credential or credential exchange.
 */
@Composable
fun W3cCredentialInfoColumn(
    modifier: Modifier = Modifier,
    credential: UICredential.W3C,
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
            NameValueTextColumn("ID", credential.id)
            when (val source = credential.source) {
                is CredentialSource.DidCommConnection -> NameValueTextColumn(
                    "From Connection",
                    source.connectionId,
                )

                is CredentialSource.OpenId4VcIssuer -> NameValueTextColumn(
                    "From OID Issuer",
                    source.issuerUrl,
                )
            }
            NameValueTextColumn("Format", "W3C")
            NameValueTextColumn("Issuer", credential.w3cVc.issuer.id)
            NameValueTextColumn("Issuance Date", credential.w3cVc.issuanceDate)
            NameValueTextColumn(
                "Type",
                credential.w3cVc.types.find { it != "VerifiableCredential" } ?: "",
            )
            credential.proofType?.let {
                NameValueTextColumn("Issuer Proof Type", it.toString())
            }
        }
        credential.w3cVc.credentialSubject.forEachIndexed { i, credSubject ->
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
