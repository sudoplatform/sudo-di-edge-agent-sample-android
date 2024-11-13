/*
 * Copyright © 2023 Anonyome Labs, Inc. All rights reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.sudoplatform.sudodiedgeagentexample.credential.exchange

import com.sudoplatform.sudodiedgeagent.SudoDIEdgeAgent
import com.sudoplatform.sudodiedgeagent.credentials.exchange.types.CredentialExchange
import com.sudoplatform.sudodiedgeagent.dids.types.CreateDidOptions
import com.sudoplatform.sudodiedgeagent.dids.types.DidInformation
import com.sudoplatform.sudodiedgeagent.dids.types.DidKeyType
import com.sudoplatform.sudodiedgeagent.dids.types.DidMethod
import com.sudoplatform.sudodiedgeagent.dids.types.ListDidsFilters
import com.sudoplatform.sudodiedgeagent.dids.types.ListDidsOptions

/**
 * Attempt to sort a list of [CredentialExchange] in descending chronological order.
 * Sorting is performed by looking for the `~started_timestamp` tag which is appended
 * to the `tags` of new [CredentialExchange] objects by default.
 *
 * If [CredentialExchange] objects have been updated to remove that tag, then this
 * sorting method will be ineffective.
 *
 * @return the sorted list
 */
fun List<CredentialExchange>.trySortByDateDescending(): List<CredentialExchange> {
    return sortedByDescending { it.tags.find { tag -> tag.name == "~started_timestamp" }?.value }
}

/**
 * Creates a DID:KEY of the specific [keyType] if one does not already exists.
 *
 * Returns the new or existing did:key DID
 */
suspend fun idempotentCreateHolderDidKey(agent: SudoDIEdgeAgent, keyType: DidKeyType): String {
    val dids = agent.dids.listAll(ListDidsOptions(ListDidsFilters(method = DidMethod.DID_KEY)))

    val existingDid = dids.firstOrNull { did -> isDidOfKeyType(did, keyType) }

    if (existingDid != null) {
        return existingDid.did
    }

    val newDid = agent.dids.createDid(CreateDidOptions.DidKey(keyType = keyType))
    return newDid.did
}

private fun isDidOfKeyType(did: DidInformation, keyType: DidKeyType): Boolean {
    // FUTURE - this information will be contained in `DidInformation`
    return when (keyType) {
        DidKeyType.ED25519 -> {
            // https://w3c-ccg.github.io/did-method-key/#ed25519-x25519
            did.did.startsWith("did:key:z6Mk")
        }
        DidKeyType.P256 -> {
            // https://w3c-ccg.github.io/did-method-key/#p-256
            did.did.startsWith("did:key:zDn")
        }
    }
}
