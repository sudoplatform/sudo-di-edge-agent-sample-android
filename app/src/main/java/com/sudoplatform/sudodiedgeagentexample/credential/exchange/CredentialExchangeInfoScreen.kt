/*
 * Copyright © 2023 Anonyome Labs, Inc. All rights reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.sudoplatform.sudodiedgeagentexample.credential.exchange

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.sudoplatform.sudodiedgeagent.SudoDIEdgeAgent
import com.sudoplatform.sudodiedgeagent.credentials.exchange.types.AcceptCredentialOfferConfiguration
import com.sudoplatform.sudodiedgeagent.credentials.exchange.types.CredentialExchange
import com.sudoplatform.sudodiedgeagent.credentials.exchange.types.CredentialExchangeInitiator
import com.sudoplatform.sudodiedgeagent.credentials.exchange.types.CredentialExchangeState
import com.sudoplatform.sudodiedgeagent.credentials.exchange.types.aries.AcceptAriesCredentialOfferFormatSpecificConfiguration
import com.sudoplatform.sudodiedgeagent.credentials.exchange.types.aries.AriesCredentialExchangeFormatData
import com.sudoplatform.sudodiedgeagent.credentials.exchange.types.openid4vc.OpenId4VcAllowedHolderBindingMethods
import com.sudoplatform.sudodiedgeagent.credentials.exchange.types.openid4vc.OpenId4VcAuthorizeConfiguration
import com.sudoplatform.sudodiedgeagent.credentials.exchange.types.openid4vc.OpenId4VcBindingMethod
import com.sudoplatform.sudodiedgeagent.credentials.exchange.types.openid4vc.OpenId4VcCredentialConfiguration
import com.sudoplatform.sudodiedgeagent.credentials.exchange.types.openid4vc.RequiredAuthorization
import com.sudoplatform.sudodiedgeagent.credentials.exchange.types.openid4vc.TxCodeRequirement
import com.sudoplatform.sudodiedgeagent.credentials.types.AnoncredV1CredentialMetadata
import com.sudoplatform.sudodiedgeagent.credentials.types.JsonLdProofType
import com.sudoplatform.sudodiedgeagent.dids.types.DidKeyType
import com.sudoplatform.sudodiedgeagent.dids.types.DidMethod
import com.sudoplatform.sudodiedgeagentexample.credential.AnoncredCredentialInfoColumn
import com.sudoplatform.sudodiedgeagentexample.credential.SdJwtCredentialInfoColumn
import com.sudoplatform.sudodiedgeagentexample.credential.UICredential
import com.sudoplatform.sudodiedgeagentexample.credential.W3cCredentialInfoColumn
import com.sudoplatform.sudodiedgeagentexample.ui.theme.SCREEN_PADDING
import com.sudoplatform.sudodiedgeagentexample.ui.theme.SudoDIEdgeAgentExampleTheme
import com.sudoplatform.sudodiedgeagentexample.utils.PreviewDataHelper
import com.sudoplatform.sudodiedgeagentexample.utils.showToastOnFailure
import com.sudoplatform.sudologging.Logger
import kotlinx.coroutines.launch

@Composable
fun CredentialExchangeInfoScreen(
    navController: NavController,
    credentialExchangeId: String,
    agent: SudoDIEdgeAgent,
    logger: Logger,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var credentialExchange: UICredentialExchange? by remember { mutableStateOf(null) }
    var isAcceptingCredential by remember { mutableStateOf(false) }

    /**
     * Attempt to accept this aries based [CredentialExchange] in the offer state.
     *
     * On success, navigate one screen backwards.
     */
    fun acceptAriesCredential() {
        scope.launch {
            isAcceptingCredential = true
            runCatching {
                val holderDid = idempotentCreateAppropriateHolderDid(
                    agent,
                    allowedMethods = listOf(DidMethod.DID_KEY),
                    allowedKeyTypes = listOf(DidKeyType.ED25519),
                )

                agent.credentials.exchange.acceptOffer(
                    credentialExchangeId,
                    AcceptCredentialOfferConfiguration.Aries(
                        autoStoreCredential = true,
                        formatSpecificConfiguration = AcceptAriesCredentialOfferFormatSpecificConfiguration.AriesLdProofVc(
                            overrideCredentialSubjectId = holderDid,
                        ),
                    ),
                )
                navController.popBackStack()
            }.showToastOnFailure(context, logger, "Failed to accept credential offer")
            isAcceptingCredential = false
        }
    }

    /**
     * Attempt to authorize this openid4vc [CredentialExchange] in the unauthorized state.
     * Authorization is done via pre-authorization code flow, using the provided transaction
     * code (a.k.a. PIN) if one was required.
     *
     * On success, the `credentialExchange` is updated to the next state (ready to accept)
     */
    fun authorizeExchange(txCode: String?) = scope.launch {
        isAcceptingCredential = true
        runCatching {
            val updatedCredentialExchange = agent.credentials.exchange.openid4vc.authorizeExchange(
                credentialExchangeId,
                OpenId4VcAuthorizeConfiguration.WithPreAuthorization(
                    txCode,
                ),
            )
            credentialExchange =
                UICredentialExchange.fromCredentialExchange(agent, updatedCredentialExchange)
        }.showToastOnFailure(context, logger, "Failed to authorize exchange")
        isAcceptingCredential = false
    }

    /**
     * Attempt to accept this openid4vc based [CredentialExchange] in the authorized state.
     * Accepts the openid4vc offer of the particular [configurationId].
     *
     * On success, the `credentialExchange` is updated to the next state (ready to store)
     */
    fun acceptOpenId4VcCredential(configurationId: String) = scope.launch {
        isAcceptingCredential = true
        runCatching {
            val allowedBindingMethods = when (val ex = credentialExchange?.exchange) {
                is CredentialExchange.OpenId4Vc -> {
                    ex.offeredCredentialConfigurations[configurationId]?.allowedBindingMethods
                        ?: throw Exception("Could not find openid4vc credential configuration")
                }

                else -> throw Exception("Exchange is not openid4vc based")
            }
            val holderDid =
                idempotentCreateAppropriateHolderDid(
                    agent,
                    allowedMethods = allowedBindingMethods.allowedDidMethods,
                    allowedKeyTypes = allowedBindingMethods.allowedKeyTypes,
                )

            val updatedCredentialExchange = agent.credentials.exchange.acceptOffer(
                credentialExchangeId,
                AcceptCredentialOfferConfiguration.OpenId4Vc(
                    autoStoreCredential = false,
                    credentialConfigurationId = configurationId,
                    holderBinding = OpenId4VcBindingMethod.WithDid(holderDid),
                ),
            )
            // will update the displayed cred ex into the ISSUED state, with cred previews
            credentialExchange =
                UICredentialExchange.fromCredentialExchange(agent, updatedCredentialExchange)
        }.showToastOnFailure(context, logger, "Failed to authorize exchange")
        isAcceptingCredential = false
    }

    /**
     * Attempt to store the [CredentialExchange] of this screen.
     *
     * On success, navigate one screen backwards.
     */
    fun storeCredential() = scope.launch {
        isAcceptingCredential = true
        runCatching {
            agent.credentials.exchange.storeCredential(credentialExchangeId)
            navController.popBackStack()
        }.showToastOnFailure(context, logger, "Failed to store credential")
        isAcceptingCredential = false
    }

    /**
     * When this composable initializes, load the [CredentialExchange] from the ID that was
     * passed in. Displaying an error toast if the [CredentialExchange] cannot be found in the
     * agent (should be logically impossible/unlikely).
     */
    LaunchedEffect(key1 = Unit) {
        runCatching {
            val loadedCredEx = agent.credentials.exchange.getById(credentialExchangeId)
                ?: throw Exception("Could not find credential exchange")
            credentialExchange = UICredentialExchange.fromCredentialExchange(agent, loadedCredEx)
        }.showToastOnFailure(context, logger, "Failed to load credential exchange")
    }

    CredentialExchangeInfoScreenView(
        credentialExchange,
        acceptAriesCredential = { acceptAriesCredential() },
        authorizeExchange = { tx -> authorizeExchange(tx) },
        acceptOpenId4VcCredential = { configId -> acceptOpenId4VcCredential(configId) },
        storeCredential = { storeCredential() },
        isAcceptingCredential,
    )
}

/**
 * UI for the "Credential Exchange Info Screen". Shows the details of a given [UICredentialExchange].
 *
 * UI will display a loading spinner until [UICredentialExchange] becomes non-null.
 *
 * UI details depend on type of [CredentialExchange] -
 * [CredentialExchange.Aries] or [CredentialExchange.OpenId4Vc].
 * (see [CredentialExchangeInfoScreenViewAriesContent] & [CredentialExchangeInfoScreenViewOpenId4VcContent])
 */
@Composable
fun CredentialExchangeInfoScreenView(
    credentialExchange: UICredentialExchange?,
    acceptAriesCredential: () -> Unit,
    authorizeExchange: (String?) -> Unit,
    acceptOpenId4VcCredential: (String) -> Unit,
    storeCredential: () -> Unit,
    isAccepting: Boolean,
) {
    Column(
        Modifier
            .fillMaxSize()
            .padding(SCREEN_PADDING),
    ) {
        if (isAccepting || credentialExchange == null) {
            Column(
                Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                CircularProgressIndicator(Modifier.padding(vertical = 8.dp))
                if (isAccepting) {
                    Text("Accepting...")
                }
            }
        } else {
            when (credentialExchange) {
                is UICredentialExchange.Aries -> CredentialExchangeInfoScreenViewAriesContent(
                    credentialExchange = credentialExchange,
                    acceptCredential = acceptAriesCredential,
                )

                is UICredentialExchange.OpenId4Vc -> CredentialExchangeInfoScreenViewOpenId4VcContent(
                    credentialExchange = credentialExchange,
                    authorizeExchange = authorizeExchange,
                    acceptCredential = acceptOpenId4VcCredential,
                    storeCredential = storeCredential,
                )
            }
        }
    }
}

/**
 * Aries content for the "Credential Exchange Info Screen".
 * Shows the details of a given aries-based [UICredentialExchange].
 *
 * When the [CredentialExchange] is in the [CredentialExchangeState.Aries.OFFER] state, then display
 * an "Accept" button, which upon clicking will inform the issuer that the agent would like to accept
 * the offer.
 */
@Composable
private fun ColumnScope.CredentialExchangeInfoScreenViewAriesContent(
    credentialExchange: UICredentialExchange.Aries,
    acceptCredential: () -> Unit,
) {
    when (val preview = credentialExchange.preview) {
        is UICredential.Anoncred -> AnoncredCredentialInfoColumn(
            Modifier.weight(1.0f),
            credential = preview,
        )

        is UICredential.W3C -> W3cCredentialInfoColumn(Modifier.weight(1.0f), credential = preview)
        is UICredential.SdJwtVc -> SdJwtCredentialInfoColumn(
            Modifier.weight(1.0f),
            credential = preview,
        )
    }

    if (credentialExchange.exchange.state == CredentialExchangeState.Aries.OFFER) {
        Button(
            onClick = { acceptCredential() },
            Modifier.fillMaxWidth(),
        ) {
            Text(text = "Accept")
        }
    }
}

/**
 * OpenId4Vc content for the "Credential Exchange Info Screen".
 * Shows the details of a given openid4vc-based [CredentialExchange].
 *
 * When the [CredentialExchange] is in the [CredentialExchangeState.OpenId4Vc.UNAUTHORIZED] state,
 * then display an "Authorize" button, with an input for a issuer defined TX-Code/PIN (if one is
 * required). Clicking this will authorize this agent with the issuer's backend.
 *
 * When the [CredentialExchange] is in the [CredentialExchangeState.OpenId4Vc.AUTHORIZED] state,
 * then display the credential configurations which could be accepted, and an "accept" button for
 * each. Clicking this will request a credential of that configuration from the issuer.
 *
 * If the [CredentialExchange] has credentials ready to store
 * ([CredentialExchange.OpenId4Vc.issuedCredentialPreviews]), then display those credential previews
 * and display a "store" button. Clicking this will store the received credential.
 */
@Composable
private fun ColumnScope.CredentialExchangeInfoScreenViewOpenId4VcContent(
    credentialExchange: UICredentialExchange.OpenId4Vc,
    authorizeExchange: (String?) -> Unit,
    acceptCredential: (String) -> Unit,
    storeCredential: () -> Unit,
) {
    var inputTxCode by remember { mutableStateOf("") }

    when (val issuedCredential = credentialExchange.issuedPreviews.firstOrNull()) {
        // is in final state, just needs storage
        is UICredential -> {
            when (issuedCredential) {
                is UICredential.Anoncred -> AnoncredCredentialInfoColumn(
                    Modifier.weight(1.0f),
                    credential = issuedCredential,
                )

                is UICredential.W3C -> W3cCredentialInfoColumn(
                    Modifier.weight(1.0f),
                    credential = issuedCredential,
                )

                is UICredential.SdJwtVc -> SdJwtCredentialInfoColumn(
                    Modifier.weight(1.0f),
                    credential = issuedCredential,
                )
            }

            Button(
                onClick = { storeCredential() },
                Modifier.fillMaxWidth(),
            ) {
                Text(text = "Store")
            }
        }

        null -> {
            LazyColumn(Modifier.weight(1.0f)) {
                item {
                    Text(
                        text = "Offered Credentials",
                        Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center,
                        fontWeight = FontWeight.Bold,
                    )
                }

                items(
                    items = credentialExchange.exchange.offeredCredentialConfigurations.entries.toList(),
                    key = { it.key },
                    itemContent = { item ->
                        val currentItem = rememberUpdatedState(item)
                        Text(text = "${currentItem.value.key}:", fontWeight = FontWeight.Bold)
                        OpenId4VcCredentialConfigurationView(
                            issuerDisplay = credentialExchange.exchange.credentialIssuerDisplay
                                ?: emptyList(),
                            configuration = currentItem.value.value,
                        )
                        Button(
                            modifier = Modifier.fillMaxWidth(),
                            onClick = { acceptCredential(currentItem.value.key) },
                            enabled = credentialExchange.exchange.state == CredentialExchangeState.OpenId4Vc.AUTHORIZED,
                        ) {
                            Text(text = "Accept")
                        }
                    },
                )
            }

            HorizontalDivider(Modifier.padding(vertical = 12.dp))

            // unauthorized -> authorized flow
            if (credentialExchange.exchange.state == CredentialExchangeState.OpenId4Vc.UNAUTHORIZED) {
                val preAuth =
                    (credentialExchange.exchange.requiredAuthorization as RequiredAuthorization.PreAuthorized)
                preAuth.txCodeRequired?.let {
                    TextField(
                        modifier = Modifier.fillMaxWidth(),
                        value = inputTxCode,
                        onValueChange = { inputTxCode = it },
                        placeholder = { Text(text = "Enter PIN from issuer...") },
                        singleLine = true,
                    )
                }

                Button(
                    onClick = { authorizeExchange(inputTxCode.ifBlank { null }) },
                    Modifier.fillMaxWidth(),
                ) {
                    Text(text = "Authorize")
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun DefaultPreview() {
    SudoDIEdgeAgentExampleTheme {
        CredentialExchangeInfoScreenView(
            credentialExchange = UICredentialExchange.Aries(
                exchange = CredentialExchange.Aries(
                    "credEx1",
                    null,
                    null,
                    listOf(),
                    CredentialExchangeState.Aries.OFFER,
                    "conn1",
                    CredentialExchangeInitiator.EXTERNAL,
                    AriesCredentialExchangeFormatData.Anoncred(
                        AnoncredV1CredentialMetadata("", ""),
                        emptyList(),
                    ),
                ),
                preview = PreviewDataHelper.dummyUICredentialAnoncred(),
            ),
            {},
            {},
            {},
            {},
            false,
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun DefaultPreview2() {
    SudoDIEdgeAgentExampleTheme {
        CredentialExchangeInfoScreenView(
            credentialExchange = UICredentialExchange.Aries(
                exchange = CredentialExchange.Aries(
                    "credEx1",
                    null,
                    null,
                    listOf(),
                    CredentialExchangeState.Aries.OFFER,
                    "conn1",
                    CredentialExchangeInitiator.EXTERNAL,
                    AriesCredentialExchangeFormatData.AriesLdProof(
                        currentProposedProofType = JsonLdProofType.ED25519_SIGNATURE2018,
                        currentProposedCredential = PreviewDataHelper.dummyW3CCredential(),
                    ),
                ),
                preview = PreviewDataHelper.dummyUICredentialW3C(),
            ),
            {},
            {},
            {},
            {},
            false,
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun DefaultPreview3() {
    SudoDIEdgeAgentExampleTheme {
        CredentialExchangeInfoScreenView(
            credentialExchange = UICredentialExchange.OpenId4Vc(
                exchange = CredentialExchange.OpenId4Vc(
                    "credEx1",
                    null,
                    null,
                    listOf(),
                    CredentialExchangeState.OpenId4Vc.UNAUTHORIZED,
                    credentialIssuerUrl = "https://issuer.foo",
                    credentialIssuerDisplay = null,
                    requiredAuthorization = RequiredAuthorization.PreAuthorized(
                        TxCodeRequirement(
                            null,
                            null,
                        ),
                    ),
                    offeredCredentialConfigurations = mapOf(
                        "UniversityDegreeSdJwt" to OpenId4VcCredentialConfiguration.SdJwtVc(
                            display = null,
                            vct = "UniversityDegree",
                            claims = mapOf(),
                            allowedBindingMethods = OpenId4VcAllowedHolderBindingMethods(
                                emptyList(),
                                emptyList(),
                            ),
                        ),
                    ),
                    issuedCredentialPreviews = listOf(),
                ),
                issuedPreviews = emptyList(),
            ),
            {},
            {},
            {},
            {},
            false,
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun DefaultPreview4() {
    SudoDIEdgeAgentExampleTheme {
        CredentialExchangeInfoScreenView(
            credentialExchange = UICredentialExchange.OpenId4Vc(
                exchange = CredentialExchange.OpenId4Vc(
                    "credEx1",
                    null,
                    null,
                    listOf(),
                    CredentialExchangeState.OpenId4Vc.ISSUED,
                    credentialIssuerUrl = "https://issuer.foo",
                    credentialIssuerDisplay = null,
                    requiredAuthorization = RequiredAuthorization.PreAuthorized(
                        TxCodeRequirement(
                            null,
                            null,
                        ),
                    ),
                    offeredCredentialConfigurations = mapOf(
                        "UniversityDegreeSdJwt" to OpenId4VcCredentialConfiguration.SdJwtVc(
                            display = null,
                            vct = "UniversityDegree",
                            claims = mapOf(),
                            allowedBindingMethods = OpenId4VcAllowedHolderBindingMethods(
                                emptyList(),
                                emptyList(),
                            ),
                        ),
                    ),
                    issuedCredentialPreviews = listOf(),
                ),
                issuedPreviews = listOf(PreviewDataHelper.dummyUICredentialSdJwt()),
            ),
            {},
            {},
            {},
            {},
            false,
        )
    }
}
