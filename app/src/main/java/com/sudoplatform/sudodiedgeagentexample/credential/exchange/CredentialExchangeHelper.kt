/*
 * Copyright © 2023 Anonyome Labs, Inc. All rights reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.sudoplatform.sudodiedgeagentexample.credential.exchange

import com.sudoplatform.sudodiedgeagent.credentials.exchange.types.CredentialExchange

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
