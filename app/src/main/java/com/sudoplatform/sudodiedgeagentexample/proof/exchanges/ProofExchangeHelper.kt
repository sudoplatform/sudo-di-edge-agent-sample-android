/*
 * Copyright © 2023 Anonyome Labs, Inc. All rights reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.sudoplatform.sudodiedgeagentexample.proof.exchanges

import com.sudoplatform.sudodiedgeagent.proofs.exchange.types.ProofExchange
import com.sudoplatform.sudodiedgeagent.proofs.exchange.types.anoncred.AnoncredPredicateType
import com.sudoplatform.sudodiedgeagent.proofs.exchange.types.anoncred.AnoncredProofRequestAttributeGroupInfo
import com.sudoplatform.sudodiedgeagent.proofs.exchange.types.anoncred.AnoncredProofRequestPredicateInfo
import com.sudoplatform.sudodiedgeagent.proofs.exchange.types.dif.StringOrNumber

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
 * Helper class to encapsulate the two categories of retrieved anoncred credentials
 *
 * Unioned together for UI simplicity sake
 */
sealed class RetrievedCredentialsForAnoncredItem(
    val itemReferent: String,
    val suitableCredentialIds: List<String>,
) {
    class AttributeGroup(
        val group: AnoncredProofRequestAttributeGroupInfo,
        itemReferent: String,
        suitableCredentialIds: List<String>,
    ) :
        RetrievedCredentialsForAnoncredItem(itemReferent, suitableCredentialIds)

    class Predicate(
        val predicate: AnoncredProofRequestPredicateInfo,
        itemReferent: String,
        suitableCredentialIds: List<String>,
    ) : RetrievedCredentialsForAnoncredItem(itemReferent, suitableCredentialIds)

    /**
     * Return the list of attributes that this item is requesting
     */
    fun requestedAttributeNames(): List<String> {
        return when (this) {
            is AttributeGroup -> group.groupAttributes
            is Predicate -> listOf(predicate.attributeName)
        }
    }

    /**
     * Return a String description to be used for UI purposes, describing what is being requested
     */
    fun description(): String {
        return when (this) {
            is AttributeGroup -> group.groupAttributes.joinToString()
            is Predicate -> predicate.textDescription()
        }
    }
}

/**
 * Create a human text description for the predicate
 */
private fun AnoncredProofRequestPredicateInfo.textDescription(): String {
    val op = when (predicateType) {
        AnoncredPredicateType.GREATER_THAN_OR_EQUAL -> ">="
        AnoncredPredicateType.GREATER_THAN -> ">"
        AnoncredPredicateType.LESS_THAN_OR_EQUAL -> "<="
        AnoncredPredicateType.LESS_THAN -> "<"
    }

    return "$attributeName $op $predicateValue"
}

fun StringOrNumber.asString(): String {
    return when (this) {
        is StringOrNumber.Number -> value.toString()
        is StringOrNumber.StringValue -> value
    }
}
