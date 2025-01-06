/*
 * Copyright © 2024 Anonyome Labs, Inc. All rights reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.sudoplatform.sudodiedgeagentexample.proof.exchanges.dif

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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.sudoplatform.sudodiedgeagent.SudoDIEdgeAgent
import com.sudoplatform.sudodiedgeagent.proofs.exchange.types.PresentationCredentials
import com.sudoplatform.sudodiedgeagent.proofs.exchange.types.ProofExchange
import com.sudoplatform.sudodiedgeagent.proofs.exchange.types.ProofExchangeInitiator
import com.sudoplatform.sudodiedgeagent.proofs.exchange.types.ProofExchangeState
import com.sudoplatform.sudodiedgeagent.proofs.exchange.types.RetrievedPresentationCredentials
import com.sudoplatform.sudodiedgeagent.proofs.exchange.types.aries.AriesProofExchangeFormatData
import com.sudoplatform.sudodiedgeagent.proofs.exchange.types.dif.Constraints
import com.sudoplatform.sudodiedgeagent.proofs.exchange.types.dif.InputDescriptorV2
import com.sudoplatform.sudodiedgeagent.proofs.exchange.types.dif.PresentationDefinitionV1
import com.sudoplatform.sudodiedgeagent.proofs.exchange.types.dif.PresentationDefinitionV2
import com.sudoplatform.sudodiedgeagentexample.credential.UICredential
import com.sudoplatform.sudodiedgeagentexample.proof.exchanges.getPresentationDefinitionV2
import com.sudoplatform.sudodiedgeagentexample.ui.theme.SCREEN_PADDING
import com.sudoplatform.sudodiedgeagentexample.ui.theme.SudoDIEdgeAgentExampleTheme
import com.sudoplatform.sudodiedgeagentexample.utils.NameValueTextColumn
import com.sudoplatform.sudodiedgeagentexample.utils.PreviewDataHelper
import com.sudoplatform.sudodiedgeagentexample.utils.showToastOnFailure
import com.sudoplatform.sudologging.Logger
import kotlinx.coroutines.launch

/**
 * Encapsulates all relevant details fetched for the UI of this presentation screen.
 */
data class DifPresentationDetails(
    val proofExchange: ProofExchange,
    val request: PresentationDefinitionV2,
    val credentialsForRequestedDescriptors: Map<String, List<UICredential>>,
)

@Composable
fun ProofExchangeDifPresentationScreen(
    navController: NavController,
    proofExchangeId: String,
    agent: SudoDIEdgeAgent,
    logger: Logger,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    /** Full set of details about this presentation, fetched on initialization */
    var presentationDetails: DifPresentationDetails? by remember { mutableStateOf(null) }
    var isPresenting by remember { mutableStateOf(false) }

    /**
     * Attempt to accept this [ProofExchange] in the request state. Uses the
     * [PresentationCredentials] that is constructed by the [ProofExchangeDifPresentationScreenView].
     *
     * On success, navigate one screen backwards.
     */
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

    /**
     * When the composable initializes, load and set the full [DifPresentationDetails]
     * by fetching data from the [SudoDIEdgeAgent]. Relevant data is fetched according
     * to the [proofExchangeId] that is passed to this composable.
     */
    LaunchedEffect(key1 = Unit) {
        runCatching {
            val proofEx = agent.proofs.exchange.getById(proofExchangeId)
                ?: throw Exception("Could not find proof exchange")

            val request = proofEx.getPresentationDefinitionV2()
                ?: throw Exception("ProofExchange is not DIF-based")

            val retrievedCredentials =
                agent.proofs.exchange.retrieveCredentialsForProofRequest(proofExchangeId) as RetrievedPresentationCredentials.Dif

            val uniqueCredIds =
                retrievedCredentials.credentialsForRequestedDescriptors.flatMap { it.value }.toSet()
            val uniqueCreds = uniqueCredIds.map {
                agent.credentials.getById(it) ?: throw Exception("Could not find credential")
            }.map {
                UICredential.fromCredential(agent, it)
            }
            val credentialsForRequestedDescriptors =
                retrievedCredentials.credentialsForRequestedDescriptors.mapValues { (_, credIds) ->
                    credIds.map { credId -> uniqueCreds.first { it.id == credId } }
                }

            presentationDetails =
                DifPresentationDetails(
                    proofExchange = proofEx,
                    request = request,
                    credentialsForRequestedDescriptors = credentialsForRequestedDescriptors,
                )
        }.showToastOnFailure(context, logger, "Failed to load proof exchange")
    }

    ProofExchangeDifPresentationScreenView(
        presentationDetails,
        present = { present(it) },
        isPresenting,
    )
}

/**
 * UI for the "Proof Exchange Presentation Screen". Displays base details of the
 * [ProofExchange] that is the subject of this screen, as well as the [RetrievedPresentationCredentials]
 * for the [ProofExchange]. [RetrievedPresentationCredentials] contains an item for each requested
 * presentation item (referent), mapped to the suitable credentials.
 *
 * Each of these [RetrievedPresentationCredentials] items are displayed as cards in a list, showing
 * the details of what is being requested, and a button to "Select". Clicking "Select"
 * will show the [SelectCredentialForDifItemModal] modal, where the user can see and
 * select a credential from the list of appropriate credentials. The selected credential
 * is then used when presenting.
 *
 * The intention of this screen is to provide a UI for constructing a complete
 * [PresentationCredentials] object, containing entries for each requested item in the
 * [RetrievedPresentationCredentials] object. After the [PresentationCredentials] is fully constructed,
 * the "Present" button is enabled and clicking it will send a presentation back to
 * the verifier.
 *
 * UI will display a loading spinner until [DifPresentationDetails] becomes non-null.
 */
@Composable
fun ProofExchangeDifPresentationScreenView(
    presentationDetails: DifPresentationDetails?,
    present: (PresentationCredentials) -> Unit,
    isPresenting: Boolean,
) {
    /**
     * When not null, indicates that the user is in the process of selecting a credential
     * of the item.
     */
    var selectingCredentialForDescriptor: InputDescriptorV2? by remember {
        mutableStateOf(null)
    }

    val presentationCredentialsForDescriptors = remember {
        mutableStateMapOf<String, String>()
    }

    /**
     * Present the credentials that have been selected. Constructing a
     * [PresentationCredentials] from the maps this composable has constructed.
     */
    fun presentSelectedCredentials() {
        val presentationCredentials = PresentationCredentials.Dif(
            credentialsForDescriptors = presentationCredentialsForDescriptors,
        )
        present(presentationCredentials)
    }

    /**
     * Continues constructing the UI's `presentationCredentialsForDescriptors` state by
     * setting the desired [selectedCredId] as the chosen credential for the given [descriptorId].
     */
    fun selectCredentialForInputDescriptorId(descriptorId: String, selectedCredId: String) {
        presentationCredentialsForDescriptors[descriptorId] = selectedCredId
    }

    /**
     * When `selectingCredentialForDescriptor` becomes non-null, the UI is requesting that
     * the modal for selecting a credential for a given descriptor is displayed. The non-null
     * value of selectingCredentialForDescriptor contains the descriptorId that the UI should
     * show a credential selection modal for.
     */
    selectingCredentialForDescriptor?.let { selectingForDescriptor ->
        val suitableCredsForDescriptor =
            presentationDetails?.credentialsForRequestedDescriptors?.get(selectingForDescriptor.id)
                ?: emptyList()
        SelectCredentialForDifItemModal(
            onDismissRequest = { selectingCredentialForDescriptor = null },
            descriptor = selectingForDescriptor,
            onSelect = { selectedCredId ->
                selectCredentialForInputDescriptorId(
                    descriptorId = selectingForDescriptor.id,
                    selectedCredId = selectedCredId,
                )
                // done selecting: set to null to hide the modal again
                selectingCredentialForDescriptor = null
            },
            suitableCredentials = suitableCredsForDescriptor,
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
            val difPresDef = presentationDetails.request
            val retrievedCreds = presentationDetails.credentialsForRequestedDescriptors

            LazyColumn(Modifier.weight(1.0f)) {
                item {
                    Text(
                        text = "Select credentials to present",
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
                        NameValueTextColumn("ID", proofEx.proofExchangeId)
                        when (proofEx) {
                            is ProofExchange.Aries -> NameValueTextColumn(
                                "From Connection",
                                proofEx.connectionId,
                            )

                            is ProofExchange.OpenId4Vc -> NameValueTextColumn(
                                "From Verifier",
                                proofEx.verifierId,
                            )
                        }
                    }
                    HorizontalDivider()
                }

                item {
                    Text(
                        text = "Requested Input Descriptors",
                        Modifier
                            .fillMaxWidth()
                            .padding(vertical = 16.dp),
                        textAlign = TextAlign.Center,
                        fontWeight = FontWeight.Bold,
                    )
                }

                items(
                    items = retrievedCreds.toList(),
                    key = { (inputDescriptorId, _) -> inputDescriptorId },
                ) { credForReferentEntry ->
                    val currentItem = rememberUpdatedState(credForReferentEntry)
                    val inputDescriptor =
                        difPresDef.inputDescriptors.first { it.id == currentItem.value.first }

                    val selectedCredentialId =
                        presentationCredentialsForDescriptors[currentItem.value.first]

                    RequestedItemCard(
                        inputDescriptor = inputDescriptor,
                        showCredentialSelection = {
                            selectingCredentialForDescriptor = inputDescriptor
                        },
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

/**
 * An item [Card] for displaying the details of an item being requested for presentation.
 * Card will show a text description of what is being requested, along with a "Select"
 * button.
 */
@Composable
private fun RequestedItemCard(
    inputDescriptor: InputDescriptorV2,
    showCredentialSelection: () -> Unit,
    selectedCredentialId: String?,
) {
    Card(Modifier.padding(vertical = 4.dp)) {
        Column(Modifier.padding(8.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1.0f)) {
                    Text(
                        text = inputDescriptor.name ?: inputDescriptor.id,
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

@Preview(showBackground = true)
@Composable
private fun DefaultPreview() {
    SudoDIEdgeAgentExampleTheme {
        ProofExchangeDifPresentationScreenView(
            presentationDetails = DifPresentationDetails(
                ProofExchange.Aries(
                    proofExchangeId = "proofEx1",
                    connectionId = "conn1",
                    initiator = ProofExchangeInitiator.EXTERNAL,
                    state = ProofExchangeState.Aries.REQUEST,
                    formatData = AriesProofExchangeFormatData.Dif(
                        PresentationDefinitionV1(
                            id = "1",
                            name = "Dummy",
                            purpose = "dummy",
                            inputDescriptors = listOf(),
                            submissionRequirements = listOf(),
                        ),
                    ),
                    errorMessage = null,
                    tags = listOf(),
                ),
                request = PresentationDefinitionV2(
                    id = "1",
                    name = "Presentation Definition",
                    purpose = "Please present",
                    inputDescriptors = listOf(
                        InputDescriptorV2(
                            "1",
                            name = "Proof of Residency",
                            purpose = "Prove you are a resident",
                            constraints = Constraints(
                                limitDisclosure = null,
                                statuses = null,
                                subjectIsIssuer = null,
                                isHolder = listOf(),
                                sameSubject = listOf(),
                                fields = listOf(),
                            ),
                        ),
                    ),
                    submissionRequirements = listOf(),
                ),
                credentialsForRequestedDescriptors = mapOf(
                    "1" to listOf(PreviewDataHelper.dummyUICredentialW3C()),
                ),
            ),
            {},
            false,
        )
    }
}
