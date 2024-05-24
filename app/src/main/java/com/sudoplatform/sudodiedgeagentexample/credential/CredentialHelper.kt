/*
 * Copyright © 2023 Anonyome Labs, Inc. All rights reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.sudoplatform.sudodiedgeagentexample.credential

import com.sudoplatform.sudodiedgeagent.credentials.types.AnoncredV1CredentialAttribute
import com.sudoplatform.sudodiedgeagent.credentials.types.Credential

/**
 * Sort a list of [AnoncredV1CredentialAttribute]s alphabetically using
 * their [AnoncredV1CredentialAttribute.name] property.
 *
 * @return the sorted list
 */
fun List<AnoncredV1CredentialAttribute>.sortAlphabetically(): List<AnoncredV1CredentialAttribute> {
    return sortedBy { it.name }
}

/**
 * Attempt to sort a list of [Credential] in descending chronological order.
 * Sorting is performed by looking for the `~created_timestamp` tag which is appended
 * to the `tags` of new [Credential] objects by default.
 *
 * If [Credential] objects have been updated to remove that tag, then this
 * sorting method will be ineffective.
 *
 * @return the sorted list
 */
fun List<Credential>.trySortByDateDescending(): List<Credential> {
    return sortedByDescending { it.tags.find { tag -> tag.name == "~created_timestamp" }?.value }
}
