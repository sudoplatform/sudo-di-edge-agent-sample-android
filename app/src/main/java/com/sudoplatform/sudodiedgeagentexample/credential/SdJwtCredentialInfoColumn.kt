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
import java.time.Instant
import java.util.Date

/**
 * Helper UI element for displaying the details of a SD-JWT credential or credential exchange.
 */
@Composable
fun SdJwtCredentialInfoColumn(
    modifier: Modifier = Modifier,
    credential: UICredential.SdJwtVc,
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
            NameValueTextColumn("Format", "SD-JWT")
            NameValueTextColumn("Issuer", credential.sdJwtVc.issuer)
            credential.sdJwtVc.issuedAt?.let {
                NameValueTextColumn(
                    "Issuance Date",
                    Date.from(Instant.ofEpochSecond(it.toLong())).toString(),
                )
            }
            NameValueTextColumn(
                "Type",
                credential.sdJwtVc.verifiableCredentialType,
            )
        }
        HorizontalDivider()

        Text(
            text = "Claims",
            Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp),
            textAlign = TextAlign.Center,
            fontWeight = FontWeight.Bold,
        )
        credential.sdJwtVc.claims.entries.forEach {
            NameValueTextRow(
                name = it.key,
                value = it.value.toString(),
            )
        }
    }
}
