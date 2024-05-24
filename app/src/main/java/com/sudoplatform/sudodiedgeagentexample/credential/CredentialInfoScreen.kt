/*
 * Copyright © 2023 Anonyome Labs, Inc. All rights reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.sudoplatform.sudodiedgeagentexample.credential

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.sudoplatform.sudodiedgeagent.SudoDIEdgeAgent
import com.sudoplatform.sudodiedgeagent.credentials.types.AnoncredV1CredentialAttribute
import com.sudoplatform.sudodiedgeagent.credentials.types.AnoncredV1CredentialMetadata
import com.sudoplatform.sudodiedgeagent.credentials.types.Credential
import com.sudoplatform.sudodiedgeagent.credentials.types.CredentialDefinitionInfo
import com.sudoplatform.sudodiedgeagent.credentials.types.CredentialFormatData
import com.sudoplatform.sudodiedgeagent.credentials.types.CredentialIssuer
import com.sudoplatform.sudodiedgeagent.credentials.types.CredentialSubject
import com.sudoplatform.sudodiedgeagent.credentials.types.JsonLdProof
import com.sudoplatform.sudodiedgeagent.credentials.types.JsonLdProofType
import com.sudoplatform.sudodiedgeagent.credentials.types.ProofPurpose
import com.sudoplatform.sudodiedgeagent.credentials.types.SchemaInfo
import com.sudoplatform.sudodiedgeagent.credentials.types.W3cCredential
import com.sudoplatform.sudodiedgeagentexample.ui.theme.SCREEN_PADDING
import com.sudoplatform.sudodiedgeagentexample.ui.theme.SudoDIEdgeAgentExampleTheme
import com.sudoplatform.sudodiedgeagentexample.utils.showToastOnFailure
import com.sudoplatform.sudologging.Logger
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.add
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.putJsonObject

@Composable
fun CredentialInfoScreen(
    credentialId: String,
    agent: SudoDIEdgeAgent,
    logger: Logger,
) {
    val context = LocalContext.current

    var credential: Credential? by remember { mutableStateOf(null) }

    /**
     * When this composable initializes, load the [Credential] from the ID that was
     * passed in. Displaying an error toast if the [Credential] cannot be found in the
     * agent (should be logically impossible/unlikely).
     */
    LaunchedEffect(key1 = Unit) {
        runCatching {
            val loadedCredEx = agent.credentials.getById(credentialId)
                ?: throw Exception("Could not find credential")
            credential = loadedCredEx
        }.showToastOnFailure(context, logger, "Failed to load credential")
    }

    CredentialInfoScreenView(
        credential,
    )
}

/**
 * UI for the "Credential Info Screen". Shows the details of a given [Credential].
 *
 * UI will display a loading spinner until [credential] becomes non-null.
 */
@Composable
fun CredentialInfoScreenView(credential: Credential?) {
    Column(
        Modifier
            .fillMaxSize()
            .padding(SCREEN_PADDING),
    ) {
        if (credential == null) {
            Column(
                Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                CircularProgressIndicator(Modifier.padding(vertical = 8.dp))
            }
        } else {
            when (val formatData = credential.formatData) {
                is CredentialFormatData.AnoncredV1 -> AnoncredCredentialInfoColumn(
                    Modifier.weight(1.0f),
                    id = credential.credentialId,
                    fromConnection = credential.connectionId,
                    metadata = formatData.credentialMetadata,
                    attributes = formatData.credentialAttributes,
                )

                is CredentialFormatData.W3C -> W3cCredentialInfoColumn(
                    Modifier.weight(1.0f),
                    id = credential.credentialId,
                    fromConnection = credential.connectionId,
                    w3cCredential = formatData.credential,
                    proofType = formatData.credential.proof?.firstOrNull()?.proofType,
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun PreviewAnoncred() {
    SudoDIEdgeAgentExampleTheme {
        CredentialInfoScreenView(
            credential = Credential(
                "cred1",
                "credEx1",
                "conn1",
                CredentialFormatData.AnoncredV1(
                    AnoncredV1CredentialMetadata(
                        "credDef1",
                        CredentialDefinitionInfo("My Cred Def 1"),
                        "schema1",
                        SchemaInfo("My Schema 1", "1.0"),
                    ),
                    listOf(
                        AnoncredV1CredentialAttribute("Attribute 1", "Value 1", null),
                        AnoncredV1CredentialAttribute("Attribute 2", "Value 2", null),
                    ),
                ),
                listOf(),
            ),
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun PreviewW3c() {
    SudoDIEdgeAgentExampleTheme {
        CredentialInfoScreenView(
            credential = Credential(
                "cred1",
                "credEx1",
                "conn1",
                CredentialFormatData.W3C(
                    W3cCredential(
                        contexts = emptyList(),
                        id = null,
                        types = listOf("Foobar"),
                        credentialSubject = listOf(
                            CredentialSubject(
                                "did:foo:holder1",
                                properties = buildJsonObject {
                                    put("attribute 1", "value 1")
                                    put("attribute 2", 2)
                                    putJsonObject("attribute 3") {
                                        put("attribute 3.1", 3.1)
                                        putJsonArray("attributes 3.2") {
                                            add(3.2)
                                            add("3.2")
                                        }
                                    }
                                },
                            ),
                        ),
                        issuer = CredentialIssuer("did:foo:issuer1", JsonObject(emptyMap())),
                        issuanceDate = "2018-04-01T15:20:15Z",
                        expirationDate = null,
                        proof = listOf(
                            JsonLdProof(
                                JsonLdProofType.BBS_BLS_SIGNATURE2020,
                                "",
                                "",
                                ProofPurpose.ASSERTION_METHOD,
                                JsonObject(
                                    emptyMap(),
                                ),
                            ),
                        ),
                        properties = JsonObject(emptyMap()),
                    ),
                ),
                listOf(),
            ),
        )
    }
}
