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
 * Get an existing holder DID owned by the [SudoDIEdgeAgent] which meets the required
 * [allowedMethods] & [allowedKeyTypes] criteria, or create a new one.
 */
suspend fun idempotentCreateAppropriateHolderDid(
    agent: SudoDIEdgeAgent,
    allowedMethods: List<DidMethod>,
    allowedKeyTypes: List<DidKeyType>,
): String {
    val dids = agent.dids.listAll(
        ListDidsOptions(
            ListDidsFilters(
                allowedDidMethods = allowedMethods,
                allowedKeyTypes = allowedKeyTypes,
            ),
        ),
    )

    // return if exists
    dids.firstOrNull()?.let {
        return it.did
    }

    val newDid = createAppropriateHolderDid(agent, allowedMethods, allowedKeyTypes)
    return newDid.did
}

private suspend fun createAppropriateHolderDid(
    agent: SudoDIEdgeAgent,
    allowedMethods: List<DidMethod>,
    allowedKeyTypes: List<DidKeyType>,
): DidInformation {
    val method = allowedMethods.firstOrNull() ?: throw Exception("No suitable DID Method")
    val keyType = allowedKeyTypes.firstOrNull() ?: throw Exception("No suitable Key type")

    val options = when (method) {
        DidMethod.DID_KEY -> CreateDidOptions.DidKey(keyType)
        DidMethod.DID_JWK -> CreateDidOptions.DidJwk(keyType)
    }

    return agent.dids.createDid(options)
}
