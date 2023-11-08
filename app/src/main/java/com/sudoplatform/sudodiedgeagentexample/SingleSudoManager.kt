/*
 * Copyright © 2023 Anonyome Labs, Inc. All rights reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.sudoplatform.sudodiedgeagentexample

import android.content.Context
import android.net.Uri
import com.sudoplatform.sudodiedgeagent.plugins.messagesource.MessageSource
import com.sudoplatform.sudodiedgeagentexample.relay.SudoDIRelayMessageSource
import com.sudoplatform.sudodirelay.SudoDIRelayClient
import com.sudoplatform.sudoentitlements.SudoEntitlementsClient
import com.sudoplatform.sudokeymanager.KeyManager
import com.sudoplatform.sudokeymanager.KeyManagerFactory
import com.sudoplatform.sudologging.Logger
import com.sudoplatform.sudoprofiles.ListOption
import com.sudoplatform.sudoprofiles.Sudo
import com.sudoplatform.sudoprofiles.SudoProfilesClient
import com.sudoplatform.sudouser.SudoUserClient
import com.sudoplatform.sudouser.TESTAuthenticationProvider
import java.util.UUID

/**
 * Abstraction for initializing and using Sudo Platform SDK dependencies which are necessary
 * to use the Sudo DI Relay as a [MessageSource] for the Edge Agent; [SudoDIRelayMessageSource].
 *
 * This sample app is registering and using a single [Sudo] in order to be granted access to the
 * Sudo DI Relay. More advanced applications may wish to utilize multiple [Sudo]s in order to
 * compartmentalize user personas.
 *
 * See here: https://docs.sudoplatform.com/guides/sudos
 *
 * However, for simplicity sake, the bare minimum [Sudo] functionality is being used here in order
 * to keep the focus on the usage of our Edge Agent SDK.
 */
class SingleSudoManager(
    private val context: Context,
    private val logger: Logger,
) {
    /** Client for managing user accounts with Sudo Platform */
    private val sudoUserClient =
        SudoUserClient.builder(context).setNamespace("edge-agent-android-example").setLogger(logger)
            .build()

    /** Client for managing a user's [Sudo]s */
    private val sudoProfilesClient =
        SudoProfilesClient.builder(context, sudoUserClient, Uri.fromFile(context.cacheDir))
            .setLogger(logger).build()

    /** Key Manager used for [SudoUserClient] authentication */
    private val keyManager = KeyManagerFactory(context).createAndroidKeyManager() as KeyManager

    /**
     * Client used to manage the entitlements that a user has.
     * E.g. the entitlement to create a Relay endpoint.
     */
    private val sudoEntitlementsClient = SudoEntitlementsClient.builder()
        .setContext(context)
        .setSudoUserClient(sudoUserClient)
        .setLogger(logger)
        .build()

    /**
     * Client used for managing the relay endpoints owned by a user.
     * Management importantly includes:
     * * creating and deleting "Postboxes" (publicly reachable endpoints used for DI communications)
     * * listing and delete "Messages" that have been received by Postboxes
     */
    val relayClient = SudoDIRelayClient.builder().setContext(context).setLogger(logger)
        .setSudoUserClient(sudoUserClient).build()

    /**
     * A pre-built [MessageSource] implementation that the Edge Agent can use in order
     * to continuously collect and receive messages from the user's relay.
     */
    val relayMessageSource = SudoDIRelayMessageSource(relayClient, logger)

    /** Lazily populated instance of the [Sudo] that is being used */
    private var sudo: Sudo? = null

    /**
     * Lazily populated instance of an ownership proof token for the [Sudo] being used.
     * Particularly, this is an opaque token that the [Sudo] creates to authenticate
     * relay postbox creation.
     */
    private var sudoOwnershipProofToken: String? = null

    companion object {
        private const val TEST_REGISTER_KEY_FILE_NAME = "register_key.private"
        private const val TEST_REGISTER_KEY_ID_FILE_NAME = "register_key.id"

        /** Audience of the ownership token the [Sudo] creates: [sudoOwnershipProofToken] */
        private const val POSTBOX_CREATE_AUDIENCE = "sudoplatform.relay.postbox"
    }

    /**
     * Use the [SudoUserClient] to register an account or sign back in.
     * If the [SudoUserClient] has not registered a user, it will register a new user using
     * the [TESTAuthenticationProvider] authentication method.
     *
     * See here: https://docs.sudoplatform.com/guides/users/registration#test-registration-keys
     */
    suspend fun signInUser() {
        if (sudoUserClient.isSignedIn()) {
            // already signed in: redeem entitlements
            sudoEntitlementsClient.redeemEntitlements()
            return
        }
        if (sudoUserClient.isRegistered()) {
            // already registered: sign in and redeem entitlements
            sudoUserClient.signInWithKey()
            sudoEntitlementsClient.redeemEntitlements()
            return
        }

        // not signed in, nor registered: register, sign in and redeem entitlements

        val privateKey =
            context.assets.open(TEST_REGISTER_KEY_FILE_NAME).bufferedReader().readText().trim()
        val keyId =
            context.assets.open(TEST_REGISTER_KEY_ID_FILE_NAME).bufferedReader().readText().trim()

        val authProvider = TESTAuthenticationProvider(
            "testRegisterAudience",
            privateKey = privateKey,
            publicKey = null,
            keyManager = keyManager,
            keyId = keyId,
        )

        sudoUserClient.registerWithAuthenticationProvider(authProvider, "dummy_rid")
        sudoUserClient.signInWithKey()
        sudoEntitlementsClient.redeemEntitlements()
    }

    /**
     * Lazily get the [Sudo] being used by this [SingleSudoManager]. This method will
     * return the cached sudo ([sudo]), else get the first sudo from the backend, else
     * create a new sudo and cache the result ([sudo]).
     */
    private suspend fun getSudo(): Sudo {
        sudo?.let {
            logger.info("Global sudo already exists: $it")
            return it
        }

        val existingSudos = sudoProfilesClient.listSudos(ListOption.REMOTE_ONLY)
        existingSudos.firstOrNull()?.let {
            logger.info("Setting global sudo as existing sudo: $it")
            sudo = it
            return it
        }

        val newSudo = Sudo(UUID.randomUUID().toString())
        newSudo.label = "John Smith"

        sudoProfilesClient.createSudo(newSudo)

        logger.info("Setting global sudo as new sudo: $newSudo")
        sudo = newSudo
        return newSudo
    }

    /**
     * Lazily get the ownership proof token that the [sudo] uses for authenticating
     * relay postbox creation.
     */
    suspend fun getSudoOwnershipProofToken(): String {
        sudoOwnershipProofToken?.let {
            logger.info("global ownership token already exists: $it")
            return it
        }

        val sudo = getSudo()
        val token = sudoProfilesClient.getOwnershipProof(sudo, POSTBOX_CREATE_AUDIENCE)

        logger.info("created new global ownership proof token for postbox creation: $token")
        sudoOwnershipProofToken = token
        return token
    }

    /**
     * deregister the sudo user from the sudo platform backend, and reset the states of any
     * related clients.
     */
    suspend fun deregisterAndReset() {
        sudoProfilesClient.reset()
        sudoUserClient.deregister()
        sudoUserClient.reset()

        sudo = null
        sudoOwnershipProofToken = null
    }
}
