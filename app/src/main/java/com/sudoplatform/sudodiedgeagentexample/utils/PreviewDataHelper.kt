/*
 * Copyright © 2024 Anonyome Labs, Inc. All rights reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.sudoplatform.sudodiedgeagentexample.utils

import com.sudoplatform.sudodiedgeagent.anoncreds.types.CredentialDefinitionInfo
import com.sudoplatform.sudodiedgeagent.anoncreds.types.CredentialSchemaInfo
import com.sudoplatform.sudodiedgeagent.credentials.types.AnoncredV1CredentialAttribute
import com.sudoplatform.sudodiedgeagent.credentials.types.CredentialIssuer
import com.sudoplatform.sudodiedgeagent.credentials.types.CredentialSource
import com.sudoplatform.sudodiedgeagent.credentials.types.CredentialSubject
import com.sudoplatform.sudodiedgeagent.credentials.types.JsonLdProof
import com.sudoplatform.sudodiedgeagent.credentials.types.JsonLdProofType
import com.sudoplatform.sudodiedgeagent.credentials.types.ProofPurpose
import com.sudoplatform.sudodiedgeagent.credentials.types.SdJwtVerifiableCredential
import com.sudoplatform.sudodiedgeagent.credentials.types.W3cCredential
import com.sudoplatform.sudodiedgeagent.types.SdJsonElement
import com.sudoplatform.sudodiedgeagentexample.credential.UICredential
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.add
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.putJsonObject

object PreviewDataHelper {
    fun dummyUICredentialAnoncred(): UICredential.Anoncred {
        return UICredential.Anoncred(
            id = "cred1",
            source = CredentialSource.DidCommConnection("conn1"),
            metadata = UICredential.FullAnoncredMetadata(
                schema = CredentialSchemaInfo(
                    "id1",
                    "author",
                    "My Schema 1",
                    "1.0",
                    emptyList(),
                ),
                credentialDefinition = CredentialDefinitionInfo(
                    "id2",
                    "issuer",
                    "id1",
                    "My Cred Def 1",
                    false,
                ),
            ),
            credentialAttributes =
            listOf(
                AnoncredV1CredentialAttribute("Attribute 1", "Value 1", null),
                AnoncredV1CredentialAttribute("Attribute 2", "Value 2", null),
            ),
        )
    }

    fun dummyW3CCredential(): W3cCredential {
        val sub = CredentialSubject(
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
        )

        return W3cCredential(
            contexts = emptyList(),
            id = null,
            types = listOf("Foobar"),
            credentialSubject = listOf(sub, sub, sub),
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
        )
    }

    fun dummyUICredentialW3C(): UICredential.W3C {
        return UICredential.W3C(
            id = "cred1",
            source = CredentialSource.OpenId4VcIssuer("https://issuer.com"),
            w3cVc = dummyW3CCredential(),
        )
    }

    fun dummyUICredentialSdJwt(): UICredential.SdJwtVc {
        return UICredential.SdJwtVc(
            id = "cred1",
            source = CredentialSource.OpenId4VcIssuer("https://issuer.com"),
            sdJwtVc =
            SdJwtVerifiableCredential(
                compactSdJwt = "j.w.t~",
                verifiableCredentialType = "UniversityDegree",
                issuer = "did:foo:bar",
                validAfter = null,
                validBefore = null,
                issuedAt = 1727244595u,
                keyBinding = null,
                claims = mapOf(
                    "code" to SdJsonElement.Primitive(true, JsonPrimitive("Math")),
                    "gpa" to SdJsonElement.Primitive(true, JsonPrimitive(4)),
                ),
            ),
        )
    }
}
