/*
 * Copyright © 2023 Anonyome Labs, Inc. All rights reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.sudoplatform.sudodiedgeagentexample.proof.exchanges

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
import androidx.compose.material3.Divider
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
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.sudoplatform.sudodiedgeagent.SudoDIEdgeAgent
import com.sudoplatform.sudodiedgeagent.credentials.types.Credential
import com.sudoplatform.sudodiedgeagent.credentials.types.CredentialAttribute
import com.sudoplatform.sudodiedgeagent.proofs.exchange.types.PredicateType
import com.sudoplatform.sudodiedgeagent.proofs.exchange.types.PresentationAttributeGroup
import com.sudoplatform.sudodiedgeagent.proofs.exchange.types.PresentationCredentials
import com.sudoplatform.sudodiedgeagent.proofs.exchange.types.PresentationPredicate
import com.sudoplatform.sudodiedgeagent.proofs.exchange.types.ProofExchange
import com.sudoplatform.sudodiedgeagent.proofs.exchange.types.ProofExchangeInitiator
import com.sudoplatform.sudodiedgeagent.proofs.exchange.types.ProofExchangeState
import com.sudoplatform.sudodiedgeagent.proofs.exchange.types.RetrievedAttributeGroupCredentials
import com.sudoplatform.sudodiedgeagent.proofs.exchange.types.RetrievedCredentials
import com.sudoplatform.sudodiedgeagent.proofs.exchange.types.RetrievedPredicateCredentials
import com.sudoplatform.sudodiedgeagentexample.ui.theme.SCREEN_PADDING
import com.sudoplatform.sudodiedgeagentexample.ui.theme.SudoDIEdgeAgentExampleTheme
import com.sudoplatform.sudodiedgeagentexample.utils.NameValueTextColumn
import com.sudoplatform.sudodiedgeagentexample.utils.showToastOnFailure
import com.sudoplatform.sudologging.Logger
import kotlinx.coroutines.launch

/**
 * Encapsulates all relevant details fetched for the UI of this presentation screen.
 */
data class PresentationDetails(
    val proofExchange: ProofExchange,
    val retrievedCredentials: RetrievedCredentials,
    val credentialIdToAttributes: Map<String, List<CredentialAttribute>>,
)

@Composable
fun ProofExchangePresentationScreen(
    navController: NavController,
    proofExchangeId: String,
    agent: SudoDIEdgeAgent,
    logger: Logger,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    /** Full set of details about this presentation, fetched on initialization */
    var presentationDetails: PresentationDetails? by remember { mutableStateOf(null) }
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
     * map the credential's ID to the list of [CredentialAttribute] that belong to it.
     */
    suspend fun loadAttributesOfAllRetrievedCredentials(
        retrievedCredentials: RetrievedCredentials,
    ): Map<String, List<CredentialAttribute>> {
        val allSuitableCredentialIds =
            retrievedCredentials.requestedAttributes.flatMap { it.credentialIds } +
                retrievedCredentials.requestedPredicates.flatMap { it.credentialIds }

        // remove duplicate credentialIds
        val allUniqueCredentialIds = allSuitableCredentialIds.toSet()

        val credentialIdToAttributes = mutableMapOf<String, List<CredentialAttribute>>()
        allUniqueCredentialIds.forEach {
            val credential = agent.credentials.getById(it)
            val attributes = credential?.credentialAttributes ?: listOf()
            credentialIdToAttributes[it] = attributes
        }

        return credentialIdToAttributes
    }

    /**
     * When the composable initializes, load and set the full [PresentationDetails]
     * by fetching data from the [SudoDIEdgeAgent]. Relevant data is fetched according
     * to the [proofExchangeId] that is passed to this composable.
     */
    LaunchedEffect(key1 = Unit) {
        runCatching {
            val proofEx = agent.proofs.exchange.getById(proofExchangeId)
                ?: throw Exception("Could not find proof exchange")
            val retrievedCredentials =
                agent.proofs.exchange.retrieveCredentialsForProofRequest(proofExchangeId)

            val credentialIdToAttributes =
                loadAttributesOfAllRetrievedCredentials(retrievedCredentials)

            presentationDetails =
                PresentationDetails(proofEx, retrievedCredentials, credentialIdToAttributes)
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
 * [ProofExchange] that is the subject of this screen, as well as the [RetrievedCredentials]
 * for the [ProofExchange]. [RetrievedCredentials] contains an item for each requested
 * presentation item (attribute/s or predicate).
 *
 * Each of these [RetrievedCredentials] items are displayed as cards in a list, showing
 * the details of what is being requested, and a button to "Select". Clicking "Select"
 * will show the [SelectCredentialForItemModal] modal, where the user can see and
 * select a credential from the list of appropriate credentials. The selected credential
 * is then used when presenting.
 *
 * The intention of this screen is to provide a UI for constructing a complete
 * [PresentationCredentials] object, containing entries for each requested item in the
 * [RetrievedCredentials] object. After the [PresentationCredentials] is fully constructed,
 * the "Present" button is enabled and clicking it will send a presentation back to
 * the verifier.
 *
 * UI will display a loading spinner until [PresentationDetails] becomes non-null.
 */
@Composable
fun ProofExchangePresentationScreenView(
    presentationDetails: PresentationDetails?,
    present: (PresentationCredentials) -> Unit,
    isPresenting: Boolean,
) {
    /**
     * When not null, indicates that the user is in the process of selecting a credential
     * of the item.
     */
    var selectingCredentialForItem: RetrievedCredentialsForItem? by remember {
        mutableStateOf(null)
    }

    /**
     * Construct a map of [PresentationAttributeGroup]s as the user selects credentials for
     * each item. This map is used to construct the [PresentationCredentials] when presenting.
     */
    val presentationAttributes = remember {
        mutableStateMapOf<String, PresentationAttributeGroup>()
    }

    /**
     * Construct a map of [PresentationPredicate]s as the user selects credentials for
     * each item. This map is used to construct the [PresentationCredentials] when presenting.
     */
    val presentationPredicates = remember {
        mutableStateMapOf<String, PresentationPredicate>()
    }

    /**
     * The presentation is determined as 'ready to present' once the user has selected
     * credentials for all requested items within [RetrievedCredentials].
     */
    val allAttributesSelected =
        presentationDetails?.retrievedCredentials?.requestedAttributes?.all {
            presentationAttributes.contains(it.groupIdentifier)
        } ?: false
    val allPredicatesSelected =
        presentationDetails?.retrievedCredentials?.requestedPredicates?.all {
            presentationPredicates.contains(it.predicateIdentifier)
        } ?: false
    val readyToPresent = allAttributesSelected && allPredicatesSelected

    /**
     * Present the credentials that have been selected. Constructing a
     * [PresentationCredentials] from the maps this composable has constructed.
     */
    fun presentSelectedCredentials() {
        val presentationCredentials = PresentationCredentials(
            requestedAttributes = presentationAttributes.toMap(),
            requestedPredicates = presentationPredicates.toMap(),
        )
        present(presentationCredentials)
    }

    /**
     * For a given [item] from the passed in [RetrievedCredentials], mark the
     * [selectedCredId] as the credential ID selected for the item. This is put into
     * the maps within this composable. These maps are then used to construct a
     * [PresentationCredentials] when presenting.
     */
    fun selectCredentialForItem(item: RetrievedCredentialsForItem, selectedCredId: String) {
        when (item) {
            is RetrievedCredentialsForItem.AttributeGroup -> presentationAttributes[item.item.groupIdentifier] =
                PresentationAttributeGroup(selectedCredId, revealed = true)

            is RetrievedCredentialsForItem.Predicate -> presentationPredicates[item.item.predicateIdentifier] =
                PresentationPredicate(selectedCredId)
        }
    }

    /**
     * If a [RetrievedCredentialsForItem] item has been selected, then show a
     * [SelectCredentialForItemModal] modal for this item, where the user can
     * see appropriate credentials for the item.
     */
    selectingCredentialForItem?.let { selectingForItem ->
        SelectCredentialForItemModal(
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
            val proofEx = presentationDetails.proofExchange
            val retrievedCreds = presentationDetails.retrievedCredentials

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
                    Divider()
                }

                if (retrievedCreds.requestedAttributes.isNotEmpty()) {
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
                        items = retrievedCreds.requestedAttributes,
                        key = { it.groupIdentifier },
                    ) { item ->
                        val currentItem = rememberUpdatedState(item)
                        val selectedCredentialId =
                            presentationAttributes[currentItem.value.groupIdentifier]?.credentialId
                        val generalItem =
                            RetrievedCredentialsForItem.AttributeGroup(currentItem.value)

                        RequestedItemCard(
                            item = generalItem,
                            showCredentialSelection = {
                                selectingCredentialForItem = generalItem
                            },
                            selectedCredentialId = selectedCredentialId,
                        )
                    }
                }

                if (retrievedCreds.requestedPredicates.isNotEmpty()) {
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
                        items = retrievedCreds.requestedPredicates,
                        key = { it.predicateIdentifier },
                    ) { item ->
                        val currentItem = rememberUpdatedState(item)
                        val selectedCredentialId =
                            presentationPredicates[currentItem.value.predicateIdentifier]?.credentialId
                        val generalItem = RetrievedCredentialsForItem.Predicate(currentItem.value)

                        RequestedItemCard(
                            item = generalItem,
                            showCredentialSelection = {
                                selectingCredentialForItem = generalItem
                            },
                            selectedCredentialId = selectedCredentialId,
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
                    text = "Select credentials for all items to present",
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
    item: RetrievedCredentialsForItem,
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

@Preview(showBackground = true)
@Composable
private fun DefaultPreview() {
    SudoDIEdgeAgentExampleTheme {
        ProofExchangePresentationScreenView(
            presentationDetails = PresentationDetails(
                ProofExchange(
                    proofExchangeId = "proofEx1",
                    connectionId = "conn1",
                    initiator = ProofExchangeInitiator.EXTERNAL,
                    state = ProofExchangeState.REQUEST,
                    errorMessage = null,
                    listOf(),
                ),
                retrievedCredentials = RetrievedCredentials(
                    requestedAttributes = listOf(
                        RetrievedAttributeGroupCredentials(
                            "1",
                            groupAttributes = listOf("dob"),
                            credentialIds = listOf("cred1", "cred2"),
                        ),
                        RetrievedAttributeGroupCredentials(
                            "2",
                            groupAttributes = listOf("dob", "gpa"),
                            credentialIds = listOf("cred1", "cred2"),
                        ),
                    ),
                    requestedPredicates = listOf(
                        RetrievedPredicateCredentials(
                            "3",
                            attributeName = "dob",
                            predicateType = PredicateType.LESS_THAN_OR_EQUAL,
                            predicateValue = 2000_09_21,
                            credentialIds = listOf("cred1", "cred2"),
                        ),
                        RetrievedPredicateCredentials(
                            "4",
                            attributeName = "gpa",
                            predicateType = PredicateType.GREATER_THAN_OR_EQUAL,
                            predicateValue = 4,
                            credentialIds = listOf(),
                        ),
                    ),
                ),
                credentialIdToAttributes = mapOf(
                    "cred1" to listOf(
                        CredentialAttribute("dob", "val1"),
                        CredentialAttribute("gpa", "val2"),
                    ),
                    "cred2" to listOf(
                        CredentialAttribute("dob", "val3"),
                        CredentialAttribute("gpa", "val4"),
                    ),
                ),
            ),
            {},
            false,
        )
    }
}
