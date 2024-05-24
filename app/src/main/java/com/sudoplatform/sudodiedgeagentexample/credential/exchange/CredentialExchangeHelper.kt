/*
 * Copyright © 2023 Anonyome Labs, Inc. All rights reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.sudoplatform.sudodiedgeagentexample.credential.exchange

import com.sudoplatform.sudodiedgeagent.SudoDIEdgeAgent
import com.sudoplatform.sudodiedgeagent.credentials.exchange.types.CredentialExchange
import com.sudoplatform.sudodiedgeagent.dids.types.CreateDidOptions
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
 * Creates an Ed25519 DID:KEY if one does not already exists.
 *
 * Returns the new or existing did:key DID
 */
suspend fun idempotentCreateHolderDidKey(agent: SudoDIEdgeAgent): String {
    val dids = agent.dids.listAll(ListDidsOptions(ListDidsFilters(method = DidMethod.DID_KEY)))

    val existingDid = dids.firstOrNull()

    if (existingDid != null) {
        return existingDid.did
    }

    val newDid = agent.dids.createDid(CreateDidOptions.DidKey(keyType = DidKeyType.ED25519))
    return newDid.did
}
