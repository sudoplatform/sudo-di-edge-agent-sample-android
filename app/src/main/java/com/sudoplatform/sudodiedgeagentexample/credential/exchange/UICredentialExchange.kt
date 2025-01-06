/*
 * Copyright © 2024 Anonyome Labs, Inc. All rights reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.sudoplatform.sudodiedgeagentexample.credential.exchange

import com.sudoplatform.sudodiedgeagent.SudoDIEdgeAgent
import com.sudoplatform.sudodiedgeagent.credentials.exchange.types.CredentialExchange
import com.sudoplatform.sudodiedgeagent.credentials.exchange.types.aries.AriesCredentialExchangeFormatData
import com.sudoplatform.sudodiedgeagent.credentials.types.CredentialSource
import com.sudoplatform.sudodiedgeagentexample.credential.UICredential

/**
 * Wrapper around the Edge Agent [CredentialExchange] with preview/s of the credential/s
 * in exchange expanded into [UICredential]/s, for UI displaying purposes.
 */
sealed interface UICredentialExchange {
    val exchange: CredentialExchange

    companion object {
        /**
         * special constructor of [UICredentialExchange], where it is constructed from
         * a base [CredentialExchange], using the [SudoDIEdgeAgent] to load [UICredential]
         * preview/s of the credential/s being exchanged in the [CredentialExchange].
         */
        suspend fun fromCredentialExchange(
            agent: SudoDIEdgeAgent,
            exchange: CredentialExchange,
        ): UICredentialExchange {
            return when (exchange) {
                is CredentialExchange.Aries -> fromCredentialExchange(agent, exchange)
                is CredentialExchange.OpenId4Vc -> fromCredentialExchange(agent, exchange)
            }
        }

        private suspend fun fromCredentialExchange(
            agent: SudoDIEdgeAgent,
            exchange: CredentialExchange.Aries,
        ): Aries {
            val source = CredentialSource.DidCommConnection(exchange.connectionId)
            val preview = when (val formatData = exchange.formatData) {
                is AriesCredentialExchangeFormatData.Anoncred -> UICredential.Anoncred(
                    id = exchange.credentialExchangeId,
                    source = source,
                    metadata = UICredential.resolveFullAnoncredMetadata(
                        agent,
                        formatData.credentialMetadata,
                    ),
                    credentialAttributes = formatData.credentialAttributes,
                )

                is AriesCredentialExchangeFormatData.AriesLdProof -> UICredential.W3C(
                    id = exchange.credentialExchangeId,
                    source = source,
                    w3cVc = formatData.currentProposedCredential,
                    proofType = formatData.currentProposedProofType,
                )
            }

            return Aries(exchange, preview)
        }

        private suspend fun fromCredentialExchange(
            agent: SudoDIEdgeAgent,
            exchange: CredentialExchange.OpenId4Vc,
        ): OpenId4Vc {
            val source = CredentialSource.OpenId4VcIssuer(exchange.credentialIssuerUrl)
            val previews = exchange.issuedCredentialPreviews.map {
                UICredential.fromFormatData(agent, it, exchange.credentialExchangeId, source)
            }

            return OpenId4Vc(exchange, previews)
        }
    }

    data class Aries(
        override val exchange: CredentialExchange.Aries,
        val preview: UICredential,
    ) : UICredentialExchange

    data class OpenId4Vc(
        override val exchange: CredentialExchange.OpenId4Vc,
        val issuedPreviews: List<UICredential>,
    ) : UICredentialExchange
}
