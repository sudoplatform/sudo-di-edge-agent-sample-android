/*
 * Copyright © 2023 Anonyome Labs, Inc. All rights reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.sudoplatform.sudodiedgeagentexample.proof.exchanges.dif

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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.sudoplatform.sudodiedgeagent.credentials.types.Credential
import com.sudoplatform.sudodiedgeagent.credentials.types.CredentialFormatData
import com.sudoplatform.sudodiedgeagent.credentials.types.CredentialIssuer
import com.sudoplatform.sudodiedgeagent.credentials.types.CredentialSource
import com.sudoplatform.sudodiedgeagent.credentials.types.CredentialSubject
import com.sudoplatform.sudodiedgeagent.credentials.types.SdJwtVerifiableCredential
import com.sudoplatform.sudodiedgeagent.credentials.types.W3cCredential
import com.sudoplatform.sudodiedgeagent.proofs.exchange.types.dif.Constraints
import com.sudoplatform.sudodiedgeagent.proofs.exchange.types.dif.Field
import com.sudoplatform.sudodiedgeagent.proofs.exchange.types.dif.Filter
import com.sudoplatform.sudodiedgeagent.proofs.exchange.types.dif.InputDescriptorV2
import com.sudoplatform.sudodiedgeagent.proofs.exchange.types.dif.StringOrNumber
import com.sudoplatform.sudodiedgeagent.types.SdJsonElement
import com.sudoplatform.sudodiedgeagentexample.credential.AnoncredCredentialInfoColumn
import com.sudoplatform.sudodiedgeagentexample.credential.SdJwtCredentialInfoColumn
import com.sudoplatform.sudodiedgeagentexample.credential.W3cCredentialInfoColumn
import com.sudoplatform.sudodiedgeagentexample.proof.exchanges.asString
import com.sudoplatform.sudodiedgeagentexample.ui.theme.SCREEN_PADDING
import com.sudoplatform.sudodiedgeagentexample.ui.theme.SudoDIEdgeAgentExampleTheme
import com.sudoplatform.sudodiedgeagentexample.utils.NameValueTextColumn
import com.sudoplatform.sudodiedgeagentexample.utils.NameValueTextRow
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject

/**
 * Modal bottom sheet view for displaying the list of credentials which are appropriate
 * for presenting a requested [descriptor].
 *
 * Displays the credentials as cards, showing their ID and showing the details of the credential
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SelectCredentialForDifItemModal(
    onDismissRequest: () -> Unit,
    descriptor: InputDescriptorV2,
    suitableCredentials: List<Credential>,
    onSelect: (credentialId: String) -> Unit,
) {
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
                    text = "Requested Descriptor:",
                    Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp),
                    fontWeight = FontWeight.Bold,
                )
                NameValueTextColumn(name = "Name", value = descriptor.name ?: "None")
                NameValueTextColumn(name = "Purpose", value = descriptor.purpose ?: "None")
                descriptor.constraints.fields.forEachIndexed { index, field ->
                    NameValueTextRow(name = "Constraint #$index", value = "")
                    NameValueTextColumn(
                        name = "Constraint Purpose",
                        value = field.purpose ?: "None",
                    )
                    NameValueTextColumn(
                        name = "Attribute Path",
                        value = field.path.toString(),
                    )

                    field.filter?.format?.let {
                        NameValueTextColumn(name = "required format", value = it)
                    }
                    field.filter?.pattern?.let {
                        NameValueTextColumn(name = "required pattern", value = it)
                    }
                    field.filter?.minimum?.let {
                        NameValueTextColumn(name = "required minimum value", value = it.asString())
                    }
                    field.filter?.exclusiveMinimum?.let {
                        NameValueTextColumn(
                            name = "required exclusiveMinimum value",
                            value = it.asString(),
                        )
                    }
                    field.filter?.maximum?.let {
                        NameValueTextColumn(name = "required maximum value", value = it.asString())
                    }
                    field.filter?.exclusiveMaximum?.let {
                        NameValueTextColumn(
                            name = "required exclusiveMaximum value",
                            value = it.asString(),
                        )
                    }
                    field.filter?.minLength?.let {
                        NameValueTextColumn(name = "required minLength", value = it.toString())
                    }
                    field.filter?.maxLength?.let {
                        NameValueTextColumn(name = "required maxLength", value = it.toString())
                    }
                    field.filter?.const?.let {
                        NameValueTextColumn(name = "required const value", value = it.asString())
                    }
                    field.filter?.enum?.let {
                        NameValueTextColumn(
                            name = "required enum value",
                            value = it.map { x -> x.asString() }.toString(),
                        )
                    }
                    field.filter?.not?.let {
                        NameValueTextColumn(name = "required not filter", value = it.toString())
                    }
                    field.filter?.other?.let {
                        NameValueTextColumn(name = "required other filter", value = it.toString())
                    }
                }

                Text(
                    text = "Select a credential to present this item with",
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
                key = { it.credentialId },
            ) { item ->
                val currentItem = rememberUpdatedState(item)

                Card(Modifier.padding(vertical = 4.dp)) {
                    Row(
                        Modifier.padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(Modifier.weight(1.0f)) {
                            when (val data = currentItem.value.formatData) {
                                is CredentialFormatData.AnoncredV1 -> AnoncredCredentialInfoColumn(
                                    id = currentItem.value.credentialId,
                                    fromSource = currentItem.value.credentialSource,
                                    metadata = data.credentialMetadata,
                                    attributes = data.credentialAttributes,
                                )

                                is CredentialFormatData.SdJwtVc -> SdJwtCredentialInfoColumn(
                                    id = currentItem.value.credentialId,
                                    fromSource = currentItem.value.credentialSource,
                                    sdJwtVc = data.credential,
                                )

                                is CredentialFormatData.W3C -> W3cCredentialInfoColumn(
                                    id = currentItem.value.credentialId,
                                    fromSource = currentItem.value.credentialSource,
                                    w3cCredential = data.credential,
                                )
                            }
                            Button(
                                onClick = { onSelect(currentItem.value.credentialId) },
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

@Preview(showBackground = true)
@Composable
private fun DefaultPreview() {
    val w3cCred = Credential(
        "cred1",
        "",
        CredentialSource.DidCommConnection("conn1"),
        formatData = CredentialFormatData.W3C(
            W3cCredential(
                contexts = listOf(),
                null,
                types = listOf("Sample"),
                credentialSubject = listOf(
                    CredentialSubject(
                        id = "did:example:123",
                        properties = JsonObject(
                            mapOf(
                                "givenName" to JsonPrimitive("John Smith"),
                            ),
                        ),
                    ),
                ),
                issuer = CredentialIssuer(
                    "did:example:issuer123",
                    JsonObject(emptyMap()),
                ),
                issuanceDate = "2024-02-12T15:30:45.123Z",
                null,
                proof = null,
                properties = JsonObject(mapOf()),
            ),
        ),
        listOf(),
    )
    val sdJwtVc = Credential(
        "cred1",
        "",
        CredentialSource.OpenId4VcIssuer("did:foo:issuer"),
        formatData = CredentialFormatData.SdJwtVc(
            SdJwtVerifiableCredential(
                compactSdJwt = "foo.bar.xyz",
                verifiableCredentialType = "accumsan",
                issuer = "expetenda",
                validAfter = null,
                validBefore = null,
                issuedAt = null,
                keyBinding = mapOf(),
                claims = mapOf("foo" to SdJsonElement.Primitive(false, JsonPrimitive("bar"))),
            ),
        ),
        listOf(),
    )
    SudoDIEdgeAgentExampleTheme {
        SelectCredentialForDifItemModal(
            onDismissRequest = { },
            descriptor = InputDescriptorV2(
                "1",
                name = "Proof of Residency",
                purpose = "Prove you are a resident",
                constraints = Constraints(
                    limitDisclosure = null,
                    statuses = null,
                    subjectIsIssuer = null,
                    isHolder = emptyList(),
                    sameSubject = emptyList(),
                    fields = listOf(
                        Field(
                            path = listOf("$.credentialSubject.givenName"),
                            id = null,
                            purpose = "Given name is Bob",
                            filter = Filter(
                                null,
                                null,
                                null,
                                null,
                                null,
                                null,
                                null,
                                null,
                                null,
                                StringOrNumber.StringValue("Bob"),
                                null,
                                null,
                                buildJsonObject { },
                            ),
                            predicate = null,
                            name = "Given Name",
                            optional = null,
                        ),
                    ),
                ),
            ),
            suitableCredentials = listOf(
                w3cCred,
                w3cCred.copy(credentialId = "2"),
                sdJwtVc.copy(credentialId = "3"),
            ),
            onSelect = {},
        )
    }
}
