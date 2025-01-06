/*
 * Copyright © 2023 Anonyome Labs, Inc. All rights reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.sudoplatform.sudodiedgeagentexample.proof.exchanges.anoncred

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
import androidx.compose.material3.TextField
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
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.sudoplatform.sudodiedgeagent.SudoDIEdgeAgent
import com.sudoplatform.sudodiedgeagent.credentials.types.AnoncredV1CredentialAttribute
import com.sudoplatform.sudodiedgeagent.credentials.types.Credential
import com.sudoplatform.sudodiedgeagent.credentials.types.CredentialFormatData
import com.sudoplatform.sudodiedgeagent.proofs.exchange.types.AnoncredPresentationAttributeGroup
import com.sudoplatform.sudodiedgeagent.proofs.exchange.types.AnoncredPresentationPredicate
import com.sudoplatform.sudodiedgeagent.proofs.exchange.types.PresentationCredentials
import com.sudoplatform.sudodiedgeagent.proofs.exchange.types.ProofExchange
import com.sudoplatform.sudodiedgeagent.proofs.exchange.types.ProofExchangeInitiator
import com.sudoplatform.sudodiedgeagent.proofs.exchange.types.ProofExchangeState
import com.sudoplatform.sudodiedgeagent.proofs.exchange.types.RetrievedPresentationCredentials
import com.sudoplatform.sudodiedgeagent.proofs.exchange.types.anoncred.AnoncredPredicateType
import com.sudoplatform.sudodiedgeagent.proofs.exchange.types.anoncred.AnoncredProofRequestAttributeGroupInfo
import com.sudoplatform.sudodiedgeagent.proofs.exchange.types.anoncred.AnoncredProofRequestInfo
import com.sudoplatform.sudodiedgeagent.proofs.exchange.types.anoncred.AnoncredProofRequestPredicateInfo
import com.sudoplatform.sudodiedgeagent.proofs.exchange.types.aries.AriesProofExchangeFormatData
import com.sudoplatform.sudodiedgeagentexample.proof.exchanges.RetrievedCredentialsForAnoncredItem
import com.sudoplatform.sudodiedgeagentexample.ui.theme.SCREEN_PADDING
import com.sudoplatform.sudodiedgeagentexample.ui.theme.SudoDIEdgeAgentExampleTheme
import com.sudoplatform.sudodiedgeagentexample.utils.NameValueTextColumn
import com.sudoplatform.sudodiedgeagentexample.utils.showToastOnFailure
import com.sudoplatform.sudologging.Logger
import kotlinx.coroutines.launch

/**
 * Encapsulates all relevant details fetched for the UI of this presentation screen.
 */
private data class AnoncredPresentationDetails(
    val proofExchange: ProofExchange,
    val retrievedCredentials: RetrievedPresentationCredentials.Anoncred,
    val credentialIdToAttributes: Map<String, List<AnoncredV1CredentialAttribute>>,
)

@Composable
fun ProofExchangeAnoncredPresentationScreen(
    navController: NavController,
    proofExchangeId: String,
    agent: SudoDIEdgeAgent,
    logger: Logger,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    /** Full set of details about this presentation, fetched on initialization */
    var presentationDetails: AnoncredPresentationDetails? by remember { mutableStateOf(null) }
    var isPresenting by remember { mutableStateOf(false) }

    /**
     * Attempt to accept this [ProofExchange] in the request state. Uses the
     * [PresentationCredentials] that is constructed by the [ProofExchangePresentationScreenView].
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
     * For all the credential IDs that were retrieved as "appropriate" for this
     * presentation, fetch each unique full [Credential] from the [SudoDIEdgeAgent], and
     * map the credential's ID to the list of [AnoncredV1CredentialAttribute] that belong to it.
     */
    suspend fun loadAttributesOfAllRetrievedCredentials(
        retrievedCredentials: RetrievedPresentationCredentials.Anoncred,
    ): Map<String, List<AnoncredV1CredentialAttribute>> {
        val allSuitableCredentialIds =
            retrievedCredentials.credentialsForRequestedAttributes.flatMap { it.value } +
                retrievedCredentials.credentialsForRequestedPredicates.flatMap { it.value }

        // remove duplicate credentialIds
        val allUniqueCredentialIds = allSuitableCredentialIds.toSet()

        val credentialIdToAttributes = mutableMapOf<String, List<AnoncredV1CredentialAttribute>>()
        allUniqueCredentialIds.forEach {
            val credential = agent.credentials.getById(it)
            val attributes =
                (credential?.formatData as CredentialFormatData.AnoncredV1?)?.credentialAttributes
                    ?: throw Exception("State error, expected Anoncred, but received something else: $credential")
            credentialIdToAttributes[it] = attributes
        }

        return credentialIdToAttributes
    }

    /**
     * When the composable initializes, load and set the full [AnoncredPresentationDetails]
     * by fetching data from the [SudoDIEdgeAgent]. Relevant data is fetched according
     * to the [proofExchangeId] that is passed to this composable.
     */
    LaunchedEffect(key1 = Unit) {
        runCatching {
            val proofEx = agent.proofs.exchange.getById(proofExchangeId)
                ?: throw Exception("Could not find proof exchange")
            val retrievedCredentials =
                agent.proofs.exchange.retrieveCredentialsForProofRequest(proofExchangeId) as RetrievedPresentationCredentials.Anoncred

            val credentialIdToAttributes =
                loadAttributesOfAllRetrievedCredentials(retrievedCredentials)

            presentationDetails =
                AnoncredPresentationDetails(proofEx, retrievedCredentials, credentialIdToAttributes)
        }.showToastOnFailure(context, logger, "Failed to load proof exchange")
    }

    ProofExchangePresentationScreenView(
        presentationDetails,
        present = { present(it) },
        isPresenting,
    )
}

/**
 * UI for the "Proof Exchange Presentation Screen". Displays base details of the
 * [ProofExchange] that is the subject of this screen, as well as the [RetrievedPresentationCredentials]
 * for the [ProofExchange]. [RetrievedPresentationCredentials] contains an item for each requested
 * presentation item (attribute/s or predicate).
 *
 * Each of these [RetrievedPresentationCredentials] items are displayed as cards in a list, showing
 * the details of what is being requested, and a button to "Select". Clicking "Select"
 * will show the [SelectCredentialForAnoncredItemModal] modal, where the user can see and
 * select a credential from the list of appropriate credentials. The selected credential
 * is then used when presenting.
 *
 * The intention of this screen is to provide a UI for constructing a complete
 * [PresentationCredentials] object, containing entries for each requested item in the
 * [RetrievedPresentationCredentials] object. After the [PresentationCredentials] is fully constructed,
 * the "Present" button is enabled and clicking it will send a presentation back to
 * the verifier.
 *
 * UI will display a loading spinner until [AnoncredPresentationDetails] becomes non-null.
 */
@Composable
private fun ProofExchangePresentationScreenView(
    presentationDetails: AnoncredPresentationDetails?,
    present: (PresentationCredentials) -> Unit,
    isPresenting: Boolean,
) {
    /**
     * When not null, indicates that the user is in the process of selecting a credential
     * of the item.
     */
    var selectingCredentialForItem: RetrievedCredentialsForAnoncredItem? by remember {
        mutableStateOf(null)
    }

    /**
     * Construct a map of [AnoncredPresentationAttributeGroup]s as the user selects credentials for
     * each item. This map is used to construct the [PresentationCredentials] when presenting.
     */
    val presentationAttributes = remember {
        mutableStateMapOf<String, AnoncredPresentationAttributeGroup>()
    }

    /**
     * Construct a map of [AnoncredPresentationPredicate]s as the user selects credentials for
     * each item. This map is used to construct the [PresentationCredentials] when presenting.
     */
    val presentationPredicates = remember {
        mutableStateMapOf<String, AnoncredPresentationPredicate>()
    }

    /**
     * Construct a map of self attested attributes as the user fills in the details for each item.
     * This map is used to construct the [PresentationCredentials] when presenting.
     */
    val presentationSelfAttestations = remember {
        mutableStateMapOf<String, String>()
    }

    /**
     * The presentation is determined as 'ready to present' once the user has selected
     * credentials for all requested items within [RetrievedPresentationCredentials].
     */
    val allAttributesSelected =
        presentationDetails?.retrievedCredentials?.credentialsForRequestedAttributes?.all {
            presentationAttributes.contains(it.key)
        } ?: false
    val allPredicatesSelected =
        presentationDetails?.retrievedCredentials?.credentialsForRequestedPredicates?.all {
            presentationPredicates.contains(it.key)
        } ?: false
    val allSelfAttestationsMade =
        presentationDetails?.retrievedCredentials?.selfAttestableAttributes?.all {
            presentationSelfAttestations.contains(it)
        } ?: false
    val readyToPresent = allAttributesSelected && allPredicatesSelected && allSelfAttestationsMade

    /**
     * Present the credentials that have been selected, and all self-attestations made.
     * Constructing a [PresentationCredentials] from the maps this composable has constructed.
     */
    fun presentSelectedCredentials() {
        val presentationCredentials = PresentationCredentials.Anoncred(
            credentialsForRequestedAttributes = presentationAttributes,
            credentialsForRequestedPredicates = presentationPredicates,
            selfAttestedAttributes = presentationSelfAttestations,
        )
        present(presentationCredentials)
    }

    /**
     * For a given [item] from the passed in [RetrievedPresentationCredentials], mark the
     * [selectedCredId] as the credential ID selected for the item. This is put into
     * the maps within this composable. These maps are then used to construct a
     * [PresentationCredentials] when presenting.
     */
    fun selectCredentialForItem(item: RetrievedCredentialsForAnoncredItem, selectedCredId: String) {
        when (item) {
            is RetrievedCredentialsForAnoncredItem.AttributeGroup -> presentationAttributes[item.itemReferent] =
                AnoncredPresentationAttributeGroup(selectedCredId, revealed = true)

            is RetrievedCredentialsForAnoncredItem.Predicate -> presentationPredicates[item.itemReferent] =
                AnoncredPresentationPredicate(selectedCredId)
        }
    }

    /**
     * If a [RetrievedCredentialsForAnoncredItem] item has been selected, then show a
     * [SelectCredentialForAnoncredItemModal] modal for this item, where the user can
     * see appropriate credentials for the item.
     */
    selectingCredentialForItem?.let { selectingForItem ->
        SelectCredentialForAnoncredItemModal(
            onDismissRequest = { selectingCredentialForItem = null },
            item = selectingForItem,
            onSelect = { selectedCredId ->
                selectCredentialForItem(
                    selectingForItem,
                    selectedCredId,
                )
                // done selecting: set to null to hide the modal again
                selectingCredentialForItem = null
            },
            attributesByCredentialId = presentationDetails?.credentialIdToAttributes ?: emptyMap(),
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
            val proofEx = presentationDetails.proofExchange as ProofExchange.Aries
            val retrievedCreds = presentationDetails.retrievedCredentials
            val anoncredProofRequest =
                (proofEx.formatData as AriesProofExchangeFormatData.Anoncred).proofRequest

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
                        NameValueTextColumn("From Connection", proofEx.connectionId)
                    }
                    HorizontalDivider()
                }

                if (retrievedCreds.credentialsForRequestedAttributes.isNotEmpty()) {
                    item {
                        Text(
                            text = "Requested Attributes",
                            Modifier
                                .fillMaxWidth()
                                .padding(vertical = 16.dp),
                            textAlign = TextAlign.Center,
                            fontWeight = FontWeight.Bold,
                        )
                    }

                    items(
                        items = retrievedCreds.credentialsForRequestedAttributes.toList(),
                        key = { (referent, _) -> referent },
                    ) { credForReferentEntry ->
                        val currentItem = rememberUpdatedState(credForReferentEntry)
                        val requestedGroup =
                            anoncredProofRequest.requestedAttributes[currentItem.value.first]!!
                        val selectedCredentialId =
                            presentationAttributes[currentItem.value.first]?.credentialId
                        val generalItem = RetrievedCredentialsForAnoncredItem.AttributeGroup(
                            requestedGroup,
                            currentItem.value.first,
                            currentItem.value.second,
                        )

                        RequestedItemCard(
                            item = generalItem,
                            showCredentialSelection = {
                                selectingCredentialForItem = generalItem
                            },
                            selectedCredentialId = selectedCredentialId,
                        )
                    }
                }

                if (retrievedCreds.credentialsForRequestedPredicates.isNotEmpty()) {
                    item {
                        Text(
                            text = "Requested Predicates",
                            Modifier
                                .fillMaxWidth()
                                .padding(vertical = 16.dp),
                            textAlign = TextAlign.Center,
                            fontWeight = FontWeight.Bold,
                        )
                    }

                    items(
                        items = retrievedCreds.credentialsForRequestedPredicates.toList(),
                        key = { (referent, _) -> referent },
                    ) { item ->
                        val currentItem = rememberUpdatedState(item)
                        val requestedPred =
                            anoncredProofRequest.requestedPredicates[currentItem.value.first]!!
                        val selectedCredentialId =
                            presentationAttributes[currentItem.value.first]?.credentialId
                        val generalItem = RetrievedCredentialsForAnoncredItem.Predicate(
                            requestedPred,
                            currentItem.value.first,
                            currentItem.value.second,
                        )

                        RequestedItemCard(
                            item = generalItem,
                            showCredentialSelection = {
                                selectingCredentialForItem = generalItem
                            },
                            selectedCredentialId = selectedCredentialId,
                        )
                    }
                }
                if (retrievedCreds.selfAttestableAttributes.isNotEmpty()) {
                    item {
                        Text(
                            text = "Self Attestable Attributes",
                            Modifier
                                .fillMaxWidth()
                                .padding(vertical = 16.dp),
                            textAlign = TextAlign.Center,
                            fontWeight = FontWeight.Bold,
                        )
                    }

                    items(
                        items = retrievedCreds.selfAttestableAttributes,
                    ) { item ->
                        val currentItem = rememberUpdatedState(newValue = item)
                        val requestedAttribute =
                            anoncredProofRequest.requestedAttributes[currentItem.value]!!
                        val attributeName = requestedAttribute.groupAttributes.first()
                        SelfAttestableItemCard(
                            attributeName,
                            onAttributeValueChange = { attributeValue ->
                                presentationSelfAttestations[currentItem.value] = attributeValue
                            },
                        )
                    }
                }
            }
            Button(
                onClick = { presentSelectedCredentials() },
                Modifier.fillMaxWidth(),
                enabled = readyToPresent,
            ) {
                Text(text = "Present")
            }
            if (!readyToPresent) {
                Text(
                    text = "Select details for all items to present",
                    Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center,
                    fontStyle = FontStyle.Italic,
                )
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
    item: RetrievedCredentialsForAnoncredItem,
    showCredentialSelection: () -> Unit,
    selectedCredentialId: String?,
) {
    Card(Modifier.padding(vertical = 4.dp)) {
        Column(Modifier.padding(8.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1.0f)) {
                    Text(text = item.description())
                }
                Button(onClick = { showCredentialSelection() }) {
                    Text(text = "Select")
                }
            }
            Text(text = "Selected: ${selectedCredentialId ?: "None"}")
        }
    }
}

@Composable
private fun SelfAttestableItemCard(
    attributeName: String,
    onAttributeValueChange: (String) -> Unit,
) {
    var inputText by remember { mutableStateOf("") }

    Card(
        Modifier
            .padding(vertical = 4.dp)
            .fillMaxWidth(),
    ) {
        Column(Modifier.padding(8.dp)) {
            Text(text = "$attributeName:")
            TextField(
                modifier = Modifier.fillMaxWidth(),
                value = inputText,
                onValueChange = { newText ->
                    inputText = newText
                    onAttributeValueChange(newText)
                },
                placeholder = { Text(text = "Enter a value...") },
                singleLine = true,
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun DefaultPreview() {
    SudoDIEdgeAgentExampleTheme {
        ProofExchangePresentationScreenView(
            presentationDetails = AnoncredPresentationDetails(
                ProofExchange.Aries(
                    proofExchangeId = "proofEx1",
                    connectionId = "conn1",
                    initiator = ProofExchangeInitiator.EXTERNAL,
                    state = ProofExchangeState.Aries.REQUEST,
                    formatData = AriesProofExchangeFormatData.Anoncred(
                        AnoncredProofRequestInfo(
                            "ProofReq",
                            "1.0",
                            requestedAttributes = mapOf(
                                "1" to AnoncredProofRequestAttributeGroupInfo(
                                    groupAttributes = listOf("gpa"), null, null,
                                ),
                                "2" to AnoncredProofRequestAttributeGroupInfo(
                                    groupAttributes = listOf("dob", "gpa"), null, null,
                                ),
                                "5" to AnoncredProofRequestAttributeGroupInfo(
                                    groupAttributes = listOf("favourite_color"), null, null,
                                ),
                            ),
                            requestedPredicates = mapOf(
                                "3" to AnoncredProofRequestPredicateInfo(
                                    attributeName = "dob",
                                    predicateType = AnoncredPredicateType.LESS_THAN,
                                    predicateValue = 2000_09_21u, null, null,
                                ),
                                "4" to AnoncredProofRequestPredicateInfo(
                                    attributeName = "gpa",
                                    predicateType = AnoncredPredicateType.GREATER_THAN_OR_EQUAL,
                                    predicateValue = 4u, null, null,
                                ),
                            ),
                            null,
                        ),
                    ),
                    errorMessage = null,
                    tags = listOf(),
                ),
                retrievedCredentials = RetrievedPresentationCredentials.Anoncred(
                    credentialsForRequestedAttributes =
                    mapOf(
                        "1" to listOf("cred1", "cred2"),
                        "2" to listOf("cred1", "cred2"),
                    ),
                    credentialsForRequestedPredicates =
                    mapOf(
                        "3" to listOf("cred1", "cred2"),
                        "4" to listOf(),
                    ),
                    selfAttestableAttributes = listOf(
                        "5",
                    ),
                ),
                credentialIdToAttributes = mapOf(
                    "cred1" to listOf(
                        AnoncredV1CredentialAttribute("dob", "val1"),
                        AnoncredV1CredentialAttribute("gpa", "val2"),
                    ),
                    "cred2" to listOf(
                        AnoncredV1CredentialAttribute("dob", "val3"),
                        AnoncredV1CredentialAttribute("gpa", "val4"),
                    ),
                ),
            ),
            {},
            false,
        )
    }
}
