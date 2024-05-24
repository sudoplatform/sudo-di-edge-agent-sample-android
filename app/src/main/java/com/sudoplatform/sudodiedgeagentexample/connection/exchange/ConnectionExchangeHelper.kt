/*
 * Copyright © 2023 Anonyome Labs, Inc. All rights reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.sudoplatform.sudodiedgeagentexample.connection.exchange

import com.sudoplatform.sudodiedgeagent.SudoDIEdgeAgent
import com.sudoplatform.sudodiedgeagent.connections.exchange.types.ConnectionExchange
import com.sudoplatform.sudodiedgeagent.connections.exchange.types.ConnectionExchangeUpdate
import com.sudoplatform.sudodiedgeagent.types.RecordTag

/**
 * Attempt to sort a list of [ConnectionExchange] in descending chronological order.
 * Sorting is performed by looking for the `~started_timestamp` tag which is appended
 * to the `tags` of new [ConnectionExchange] objects by default.
 *
 * If [ConnectionExchange] objects have been updated to remove that tag, then this
 * sorting method will be ineffective.
 *
 * @return the sorted list
 */
fun List<ConnectionExchange>.trySortByDateDescending(): List<ConnectionExchange> {
    return sortedByDescending { it.tags.find { tag -> tag.name == "~started_timestamp" }?.value }
}

private const val INVITER_RELAY_ENDPOINT_TAG_NAME = "relay_endpoint"

/**
 * Helper function to apply custom metadata to a [ConnectionExchange] via the agent's tagging system.
 * The additional tag added to record is the [relayEndpoint] that was used by the inviter
 * when first creating the invitation (such that it can be fetch via [extractInviterRelayEndpointMetadata]
 * and re-used).
 */
suspend fun ConnectionExchange.applyInviterRelayEndpointMetadata(
    agent: SudoDIEdgeAgent,
    relayEndpoint: String,
) {
    agent.connections.exchange.updateConnectionExchange(
        connectionExchangeId,
        ConnectionExchangeUpdate(
            tags = tags + RecordTag(INVITER_RELAY_ENDPOINT_TAG_NAME, relayEndpoint),
        ),
    )
}

/**
 * Fetches the custom relay endpoint metadata tag on the [ConnectionExchange] that was added
 * via [applyInviterRelayEndpointMetadata]. Returning null if not found.
 */
fun ConnectionExchange.extractInviterRelayEndpointMetadata(): String? {
    return tags.find { it.name == INVITER_RELAY_ENDPOINT_TAG_NAME }?.value
}
