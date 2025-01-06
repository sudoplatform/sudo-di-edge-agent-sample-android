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
 * Helper UI element for displaying the details of an anoncred credential or credential exchange.
 */
@Composable
fun AnoncredCredentialInfoColumn(
    modifier: Modifier = Modifier,
    credential: UICredential.Anoncred,
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
            NameValueTextColumn("Format", "Anoncreds")
            NameValueTextColumn("Cred Def ID", credential.metadata.credentialDefinition.id)
            NameValueTextColumn(
                "Cred Def Issuer",
                credential.metadata.credentialDefinition.issuerId,
            )
            NameValueTextColumn("Schema ID", credential.metadata.schema.id)
            NameValueTextColumn("Schema Name", credential.metadata.schema.name)
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
        credential.credentialAttributes.sortAlphabetically().forEach {
            NameValueTextRow(
                name = it.name,
                value = it.value,
            )
        }
    }
}
