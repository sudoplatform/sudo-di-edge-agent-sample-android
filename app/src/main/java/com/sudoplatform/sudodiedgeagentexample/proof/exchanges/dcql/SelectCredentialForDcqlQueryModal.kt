/*
 * Copyright © 2026 Anonyome Labs, Inc. All rights reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.sudoplatform.sudodiedgeagentexample.proof.exchanges.dcql

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
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.sudoplatform.sudodiedgeagent.proofs.exchange.types.dcql.CredentialQuery
import com.sudoplatform.sudodiedgeagent.proofs.exchange.types.dcql.CredentialQueryFormatMeta
import com.sudoplatform.sudodiedgeagentexample.credential.AnoncredCredentialInfoColumn
import com.sudoplatform.sudodiedgeagentexample.credential.SdJwtCredentialInfoColumn
import com.sudoplatform.sudodiedgeagentexample.credential.UICredential
import com.sudoplatform.sudodiedgeagentexample.credential.W3cCredentialInfoColumn
import com.sudoplatform.sudodiedgeagentexample.ui.theme.SCREEN_PADDING
import com.sudoplatform.sudodiedgeagentexample.utils.NameValueTextColumn

/**
 * Modal bottom sheet for selecting a credential to present for a DCQL [CredentialQuery].
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SelectCredentialForDcqlQueryModal(
    onDismissRequest: () -> Unit,
    credentialQuery: CredentialQuery,
    suitableCredentials: List<UICredential>,
    onSelect: (credentialId: String) -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    ) {
        LazyColumn(
            Modifier
                .fillMaxSize()
                .padding(SCREEN_PADDING),
        ) {
            item {
                Text(
                    text = "Requested Credential Query:",
                    Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp),
                    fontWeight = FontWeight.Bold,
                )
                NameValueTextColumn(name = "Query ID", value = credentialQuery.id)
                NameValueTextColumn(
                    name = "Format",
                    value = when (val meta = credentialQuery.formatMeta) {
                        is CredentialQueryFormatMeta.SdJwtVc -> "SD-JWT VC (vct: ${meta.meta.vctValues.joinToString()})"
                        is CredentialQueryFormatMeta.W3cLdp -> "W3C LDP VC"
                        is CredentialQueryFormatMeta.W3cSdJwt -> "W3C SD-JWT VC"
                        is CredentialQueryFormatMeta.Unknown -> "Unknown (${meta.format})"
                    },
                )
                credentialQuery.claims?.forEachIndexed { index, claim ->
                    NameValueTextColumn(
                        name = "Claim #$index path",
                        value = claim.path.joinToString(".") { component ->
                            when (component) {
                                is com.sudoplatform.sudodiedgeagent.proofs.exchange.types.dcql.DcqlClaimPathComponent.StringValue -> component.value
                                is com.sudoplatform.sudodiedgeagent.proofs.exchange.types.dcql.DcqlClaimPathComponent.Integer -> "[${component.value}]"
                                is com.sudoplatform.sudodiedgeagent.proofs.exchange.types.dcql.DcqlClaimPathComponent.Null -> "[*]"
                            }
                        },
                    )
                }

                Text(
                    text = "Select a credential to present for this query",
                    Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp),
                    textAlign = TextAlign.Center,
                    fontWeight = FontWeight.Bold,
                )
            }
            if (suitableCredentials.isEmpty()) {
                item {
                    Text(
                        text = "No suitable credentials found.",
                        Modifier
                            .fillMaxWidth()
                            .padding(vertical = 16.dp),
                        textAlign = TextAlign.Center,
                    )
                }
            }
            items(
                items = suitableCredentials,
                key = { it.id },
            ) { item ->
                val currentItem = rememberUpdatedState(item)

                Card(Modifier.padding(vertical = 4.dp)) {
                    Row(
                        Modifier.padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(Modifier.weight(1.0f)) {
                            when (val data = currentItem.value) {
                                is UICredential.Anoncred -> AnoncredCredentialInfoColumn(
                                    credential = data,
                                )
                                is UICredential.SdJwtVc -> SdJwtCredentialInfoColumn(
                                    credential = data,
                                )
                                is UICredential.W3C -> W3cCredentialInfoColumn(
                                    credential = data,
                                )
                            }
                            Button(
                                onClick = { onSelect(currentItem.value.id) },
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
