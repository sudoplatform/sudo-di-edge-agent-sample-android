/*
 * Copyright © 2026 Anonyome Labs, Inc. All rights reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.sudoplatform.sudodiedgeagentexample.proof.exchanges.dcql

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.sudoplatform.sudodiedgeagent.SudoDIEdgeAgent
import com.sudoplatform.sudodiedgeagent.proofs.exchange.types.PresentationCredentials
import com.sudoplatform.sudodiedgeagent.proofs.exchange.types.ProofExchange
import com.sudoplatform.sudodiedgeagent.proofs.exchange.types.RetrievedPresentationCredentials
import com.sudoplatform.sudodiedgeagent.proofs.exchange.types.dcql.CredentialQuery
import com.sudoplatform.sudodiedgeagent.proofs.exchange.types.dcql.DcqlQuery
import com.sudoplatform.sudodiedgeagentexample.credential.UICredential
import com.sudoplatform.sudodiedgeagentexample.ui.theme.SCREEN_PADDING
import com.sudoplatform.sudodiedgeagentexample.utils.NameValueTextColumn
import com.sudoplatform.sudodiedgeagentexample.utils.showToastOnFailure
import com.sudoplatform.sudologging.Logger
import kotlinx.coroutines.launch

/**
 * Encapsulates all relevant details fetched for the UI of this DCQL presentation screen.
 */
data class DcqlPresentationDetails(
    val proofExchange: ProofExchange.OpenId4Vc,
    val request: DcqlQuery,
    val credentialsForQueryId: Map<String, List<UICredential>>,
)

@Composable
fun ProofExchangeDcqlPresentationScreen(
    navController: NavController,
    proofExchangeId: String,
    agent: SudoDIEdgeAgent,
    logger: Logger,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var presentationDetails: DcqlPresentationDetails? by remember { mutableStateOf(null) }
    var isPresenting by remember { mutableStateOf(false) }

    fun present(presentationCredentials: PresentationCredentials) {
        scope.launch {
            isPresenting = true
            runCatching {
                agent.proofs.exchange.presentProof(proofExchangeId, presentationCredentials)
                navController.popBackStack()
            }.showToastOnFailure(context, logger)
            isPresenting = false
        }
    }

    LaunchedEffect(key1 = Unit) {
        runCatching {
            val proofEx = agent.proofs.exchange.getById(proofExchangeId)
                ?: throw Exception("Could not find proof exchange")

            val openId4VcProofEx = proofEx as? ProofExchange.OpenId4Vc
                ?: throw Exception("ProofExchange is not OpenId4Vc")

            val retrievedCredentials =
                agent.proofs.exchange.retrieveCredentialsForProofRequest(proofExchangeId)
                    as RetrievedPresentationCredentials.Dcql

            val uniqueCredIds =
                retrievedCredentials.credentialIdsForQueryId.flatMap { it.value }.toSet()
            val uniqueCreds = uniqueCredIds.map {
                agent.credentials.getById(it) ?: throw Exception("Could not find credential")
            }.map {
                UICredential.fromCredential(agent, it)
            }
            val credentialsForQueryId =
                retrievedCredentials.credentialIdsForQueryId.mapValues { (_, credIds) ->
                    credIds.map { credId -> uniqueCreds.first { it.id == credId } }
                }

            presentationDetails = DcqlPresentationDetails(
                proofExchange = openId4VcProofEx,
                request = openId4VcProofEx.presentationRequest,
                credentialsForQueryId = credentialsForQueryId,
            )
        }.showToastOnFailure(context, logger, "Failed to load proof exchange")
    }

    ProofExchangeDcqlPresentationScreenView(
        presentationDetails,
        present = { present(it) },
        isPresenting,
    )
}

@Composable
fun ProofExchangeDcqlPresentationScreenView(
    presentationDetails: DcqlPresentationDetails?,
    present: (PresentationCredentials) -> Unit,
    isPresenting: Boolean,
) {
    var selectingCredentialForQuery: CredentialQuery? by remember { mutableStateOf(null) }

    val selectedCredentialForQueryId = remember { mutableStateMapOf<String, String>() }

    fun presentSelectedCredentials() {
        present(
            PresentationCredentials.Dcql(
                credentialIdForQueryId = selectedCredentialForQueryId.toMap(),
            ),
        )
    }

    selectingCredentialForQuery?.let { query ->
        val suitableCreds = presentationDetails?.credentialsForQueryId?.get(query.id) ?: emptyList()
        SelectCredentialForDcqlQueryModal(
            onDismissRequest = { selectingCredentialForQuery = null },
            credentialQuery = query,
            suitableCredentials = suitableCreds,
            onSelect = { selectedCredId ->
                selectedCredentialForQueryId[query.id] = selectedCredId
                selectingCredentialForQuery = null
            },
        )
    }

    Column(
        Modifier
            .fillMaxSize()
            .padding(SCREEN_PADDING),
    ) {
        if (isPresenting || presentationDetails == null) {
            Column(
                Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                CircularProgressIndicator(Modifier.padding(vertical = 8.dp))
                if (isPresenting) {
                    Text("Presenting...")
                }
            }
        } else {
            val proofEx = presentationDetails.proofExchange
            val dcqlQuery = presentationDetails.request
            val retrievedCreds = presentationDetails.credentialsForQueryId

            LazyColumn(Modifier.weight(1.0f)) {
                item {
                    Text(
                        text = "Select credentials to present",
                        Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center,
                        fontWeight = FontWeight.Bold,
                    )
                    Column(
                        Modifier
                            .fillMaxWidth()
                            .padding(vertical = 16.dp),
                    ) {
                        NameValueTextColumn("ID", proofEx.proofExchangeId)
                        NameValueTextColumn("From Verifier", proofEx.verifierId)
                    }
                    HorizontalDivider()
                }

                item {
                    Text(
                        text = "Requested Credential Queries",
                        Modifier
                            .fillMaxWidth()
                            .padding(vertical = 16.dp),
                        textAlign = TextAlign.Center,
                        fontWeight = FontWeight.Bold,
                    )
                }

                items(
                    items = retrievedCreds.toList(),
                    key = { (queryId, _) -> queryId },
                ) { (queryId, _) ->
                    val currentQueryId = rememberUpdatedState(queryId)
                    val credentialQuery =
                        dcqlQuery.credentials.first { it.id == currentQueryId.value }
                    val selectedCredentialId = selectedCredentialForQueryId[currentQueryId.value]

                    CredentialQueryCard(
                        credentialQuery = credentialQuery,
                        showCredentialSelection = { selectingCredentialForQuery = credentialQuery },
                        selectedCredentialId = selectedCredentialId,
                    )
                }
            }
            Button(
                onClick = { presentSelectedCredentials() },
                Modifier.fillMaxWidth(),
            ) {
                Text(text = "Present")
            }
        }
    }
}

@Composable
private fun CredentialQueryCard(
    credentialQuery: CredentialQuery,
    showCredentialSelection: () -> Unit,
    selectedCredentialId: String?,
) {
    Card(Modifier.padding(vertical = 4.dp)) {
        Column(Modifier.padding(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1.0f)) {
                    Text(
                        text = credentialQuery.id,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = formatMetaDescription(credentialQuery),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Button(onClick = { showCredentialSelection() }) {
                    Text(text = "Select")
                }
            }
            Text(text = "Selected: ${selectedCredentialId ?: "None"}")
        }
    }
}

private fun formatMetaDescription(query: CredentialQuery): String {
    return when (val meta = query.formatMeta) {
        is com.sudoplatform.sudodiedgeagent.proofs.exchange.types.dcql.CredentialQueryFormatMeta.SdJwtVc ->
            "SD-JWT VC (vct: ${meta.meta.vctValues.joinToString()})"
        is com.sudoplatform.sudodiedgeagent.proofs.exchange.types.dcql.CredentialQueryFormatMeta.W3cLdp ->
            "W3C LDP VC"
        is com.sudoplatform.sudodiedgeagent.proofs.exchange.types.dcql.CredentialQueryFormatMeta.W3cSdJwt ->
            "W3C SD-JWT VC"
        is com.sudoplatform.sudodiedgeagent.proofs.exchange.types.dcql.CredentialQueryFormatMeta.Unknown ->
            "Unknown (${meta.format})"
    }
}
