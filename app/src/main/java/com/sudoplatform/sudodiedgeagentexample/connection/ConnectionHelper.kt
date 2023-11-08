/*
 * Copyright © 2023 Anonyome Labs, Inc. All rights reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.sudoplatform.sudodiedgeagentexample.connection

import com.sudoplatform.sudodiedgeagent.connections.types.Connection

/**
 * Attempt to sort a list of [Connection] in descending chronological order.
 * Sorting is performed by looking for the `~created_timestamp` tag which is appended
 * to the `tags` of new [Connection] objects by default.
 *
 * If [Connection] objects have been updated to remove that tag, then this
 * sorting method will be ineffective.
 *
 * @return the sorted list
 */
fun List<Connection>.trySortByDateDescending(): List<Connection> {
    return sortedByDescending { it.tags.find { tag -> tag.name == "~created_timestamp" }?.value }
}
