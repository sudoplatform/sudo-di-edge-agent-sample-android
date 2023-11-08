/*
 * Copyright © 2023 Anonyome Labs, Inc. All rights reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.sudoplatform.sudodiedgeagentexample.connection.exchange

import com.sudoplatform.sudodiedgeagent.connections.exchange.types.ConnectionExchange

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
