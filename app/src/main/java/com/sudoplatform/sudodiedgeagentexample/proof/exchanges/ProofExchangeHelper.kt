/*
 * Copyright © 2023 Anonyome Labs, Inc. All rights reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.sudoplatform.sudodiedgeagentexample.proof.exchanges

import com.sudoplatform.sudodiedgeagent.proofs.exchange.types.PredicateType
import com.sudoplatform.sudodiedgeagent.proofs.exchange.types.ProofExchange
import com.sudoplatform.sudodiedgeagent.proofs.exchange.types.RetrievedAttributeGroupCredentials
import com.sudoplatform.sudodiedgeagent.proofs.exchange.types.RetrievedCredentials
import com.sudoplatform.sudodiedgeagent.proofs.exchange.types.RetrievedPredicateCredentials

/**
 * Attempt to sort a list of [ProofExchange] in descending chronological order.
 * Sorting is performed by looking for the `~started_timestamp` tag which is appended
 * to the `tags` of new [ProofExchange] objects by default.
 *
 * If [ProofExchange] objects have been updated to remove that tag, then this
 * sorting method will be ineffective.
 *
 * @return the sorted list
 */
fun List<ProofExchange>.trySortByDateDescending(): List<ProofExchange> {
    return sortedByDescending { it.tags.find { tag -> tag.name == "~started_timestamp" }?.value }
}

/**
 * Helper class to encapsulate the two categories of retrieved credentials within
 * [RetrievedCredentials]: [RetrievedAttributeGroupCredentials] & [RetrievedPredicateCredentials].
 *
 * Unioned together for UI simplicity sake
 */
sealed class RetrievedCredentialsForItem {
    class AttributeGroup(val item: RetrievedAttributeGroupCredentials) :
        RetrievedCredentialsForItem()

    class Predicate(val item: RetrievedPredicateCredentials) : RetrievedCredentialsForItem()

    /**
     * Return the list of credential IDs that are suitable for this item
     */
    fun suitableCredentialIds(): List<String> {
        return when (this) {
            is AttributeGroup -> item.credentialIds
            is Predicate -> item.credentialIds
        }
    }

    /**
     * Return the list of attributes that this item is requesting
     */
    fun requestedAttributeNames(): List<String> {
        return when (this) {
            is AttributeGroup -> item.groupAttributes
            is Predicate -> listOf(item.attributeName)
        }
    }

    /**
     * Return a String description to be used for UI purposes, describing what is being requested
     */
    fun description(): String {
        return when (this) {
            is AttributeGroup -> item.groupAttributes.joinToString()
            is Predicate -> item.textDescription()
        }
    }
}

/**
 * Create a human text description for the predicate
 */
private fun RetrievedPredicateCredentials.textDescription(): String {
    val op = when (predicateType) {
        PredicateType.GREATER_THAN_OR_EQUAL -> ">="
        PredicateType.GREATER_THAN -> ">"
        PredicateType.LESS_THAN_OR_EQUAL -> "<="
        PredicateType.LESS_THAN -> "<"
    }

    return "$attributeName $op $predicateValue"
}
