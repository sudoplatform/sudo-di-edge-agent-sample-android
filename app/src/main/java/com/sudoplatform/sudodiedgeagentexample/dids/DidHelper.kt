/*
 * Copyright © 2023 Anonyome Labs, Inc. All rights reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.sudoplatform.sudodiedgeagentexample.dids

import com.sudoplatform.sudodiedgeagent.SudoDIEdgeAgent
import com.sudoplatform.sudodiedgeagent.dids.types.DidInformation
import com.sudoplatform.sudodiedgeagent.dids.types.DidKeyType
import com.sudoplatform.sudodiedgeagent.dids.types.DidUpdate
import com.sudoplatform.sudodiedgeagent.types.RecordTag

/**
 * Attempt to sort a list of [DidInformation] in descending chronological order.
 * Sorting is performed by looking for the `~created_timestamp` tag which is appended
 * to the `tags` of new [DidInformation] objects by default.
 *
 * If [DidInformation] objects have been updated to remove that tag, then this
 * sorting method will be ineffective.
 *
 * @return the sorted list
 */
fun List<DidInformation>.trySortByDateDescending(): List<DidInformation> {
    return sortedByDescending { it.tags.find { tag -> tag.name == "~created_timestamp" }?.value }
}

private const val ALIAS_TAG_NAME = "alias"

/**
 * Custom application logic to apply a user-defined human readable "alias" to a DID.
 * Assignment is done using the Edge Agent's record tagging ability. The given [did]
 * is updated using the [agent] to have an alias record tag with the given [alias].
 *
 * Then on subsequent interactions with [DidInformation], the [alias] can be
 * recovered via the [DidInformation.tags].
 */
suspend fun assignAliasToDid(agent: SudoDIEdgeAgent, did: DidInformation, alias: String) {
    val newTags = did.tags.toMutableList()
    newTags.add(RecordTag(ALIAS_TAG_NAME, alias))
    agent.dids.updateDid(did.did, DidUpdate(tags = newTags))
}

/** Get the human readable DID method for this [DidInformation] */
fun DidInformation.method(): String {
    return when (methodData) {
        is DidInformation.MethodData.DidJwk -> "did:jwk"
        is DidInformation.MethodData.DidKey -> "did:key"
    }
}

/** Get the key type of key which backs this [DidInformation] */
fun DidInformation.keyType(): DidKeyType {
    return when (val data = methodData) {
        is DidInformation.MethodData.DidJwk -> data.keyType
        is DidInformation.MethodData.DidKey -> data.keyType
    }
}

/**
 * Get the assigned "alias" from the [DidInformation], if any.
 *
 * Aliases are assigned via [assignAliasToDid].
 */
fun DidInformation.alias(): String? {
    return tags.find { it.name == ALIAS_TAG_NAME }?.value
}
