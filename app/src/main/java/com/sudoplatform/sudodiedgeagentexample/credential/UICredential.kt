package com.sudoplatform.sudodiedgeagentexample.credential

import com.sudoplatform.sudodiedgeagent.SudoDIEdgeAgent
import com.sudoplatform.sudodiedgeagent.anoncreds.types.CredentialDefinitionInfo
import com.sudoplatform.sudodiedgeagent.anoncreds.types.CredentialSchemaInfo
import com.sudoplatform.sudodiedgeagent.credentials.types.AnoncredV1CredentialAttribute
import com.sudoplatform.sudodiedgeagent.credentials.types.AnoncredV1CredentialMetadata
import com.sudoplatform.sudodiedgeagent.credentials.types.Credential
import com.sudoplatform.sudodiedgeagent.credentials.types.CredentialFormatData
import com.sudoplatform.sudodiedgeagent.credentials.types.CredentialSource
import com.sudoplatform.sudodiedgeagent.credentials.types.JsonLdProofType
import com.sudoplatform.sudodiedgeagent.credentials.types.SdJwtVerifiableCredential
import com.sudoplatform.sudodiedgeagent.credentials.types.W3cCredential

/**
 * Wrapper around the Edge Agent [Credential] with extra collected/resolve data
 * needed for UI displaying purposes (e.g. the resolved anoncreds metadata)
 */
sealed interface UICredential {
    val id: String
    val source: CredentialSource

    companion object {
        /**
         * helper function to resolve the full anoncreds metadata from [AnoncredV1CredentialMetadata].
         * Including the schema & credential definition details.
         */
        suspend fun resolveFullAnoncredMetadata(
            agent: SudoDIEdgeAgent,
            metadata: AnoncredV1CredentialMetadata,
        ): FullAnoncredMetadata {
            val schema =
                agent.anoncreds.resolveSchema(metadata.schemaId)
            val credentialDefinition =
                agent.anoncreds.resolveCredentialDefinition(metadata.credentialDefinitionId)

            return FullAnoncredMetadata(schema, credentialDefinition)
        }

        /**
         * special constructor of [UICredential], where the [UICredential] is assembled from
         * the [Credential], using the [SudoDIEdgeAgent] to load any extra data if required.
         */
        suspend fun fromCredential(agent: SudoDIEdgeAgent, credential: Credential): UICredential {
            return when (val formatData = credential.formatData) {
                is CredentialFormatData.AnoncredV1 -> {
                    Anoncred(
                        id = credential.credentialId,
                        source = credential.credentialSource,
                        metadata = resolveFullAnoncredMetadata(
                            agent,
                            formatData.credentialMetadata,
                        ),
                        credentialAttributes = formatData.credentialAttributes,
                    )
                }

                is CredentialFormatData.SdJwtVc -> SdJwtVc(
                    id = credential.credentialId,
                    source = credential.credentialSource,
                    sdJwtVc = formatData.credential,
                )

                is CredentialFormatData.W3C -> W3C(
                    id = credential.credentialId,
                    source = credential.credentialSource,
                    w3cVc = formatData.credential,
                    proofType = formatData.credential.proof?.firstOrNull()?.proofType,
                )
            }
        }

        /**
         * special constructor of [UICredential], where the [UICredential] is assembled from
         * [CredentialFormatData], using the [SudoDIEdgeAgent] to load any extra data if required.
         */
        suspend fun fromFormatData(
            agent: SudoDIEdgeAgent,
            formatData: CredentialFormatData,
            id: String,
            source: CredentialSource,
        ): UICredential {
            return when (formatData) {
                is CredentialFormatData.AnoncredV1 -> {
                    Anoncred(
                        id = id,
                        source = source,
                        metadata = resolveFullAnoncredMetadata(
                            agent,
                            formatData.credentialMetadata,
                        ),
                        credentialAttributes = formatData.credentialAttributes,
                    )
                }

                is CredentialFormatData.SdJwtVc -> SdJwtVc(
                    id = id,
                    source = source,
                    sdJwtVc = formatData.credential,
                )

                is CredentialFormatData.W3C -> W3C(
                    id = id,
                    source = source,
                    w3cVc = formatData.credential,
                    proofType = formatData.credential.proof?.firstOrNull()?.proofType,
                )
            }
        }
    }

    data class FullAnoncredMetadata(
        val schema: CredentialSchemaInfo,
        val credentialDefinition: CredentialDefinitionInfo,
    )

    /** Get the UI displayable type/name for this credential */
    val previewName: String
        get() = when (this) {
            is Anoncred -> metadata.schema.name
            is SdJwtVc -> sdJwtVc.verifiableCredentialType
            is W3C -> w3cVc.types.find { it != "VerifiableCredential" }
                ?: "VerifiableCredential"
        }

    /** Get the UI displayable format of this credential */
    val previewFormat: String
        get() = when (this) {
            is Anoncred -> "Anoncred"
            is SdJwtVc -> "SD-JWT VC"
            is W3C -> "W3C"
        }

    data class Anoncred(
        override val id: String,
        override val source: CredentialSource,
        val metadata: FullAnoncredMetadata,
        val credentialAttributes: List<AnoncredV1CredentialAttribute>,
    ) : UICredential

    data class W3C(
        override val id: String,
        override val source: CredentialSource,
        val w3cVc: W3cCredential,
        val proofType: JsonLdProofType? = null,
    ) : UICredential

    data class SdJwtVc(
        override val id: String,
        override val source: CredentialSource,
        val sdJwtVc: SdJwtVerifiableCredential,
    ) : UICredential
}
