/*
 * Copyright © 2023 Anonyome Labs, Inc. All rights reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.sudoplatform.sudodiedgeagentexample.register

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.sudoplatform.sudodiedgeagent.SudoDIEdgeAgent
import com.sudoplatform.sudodiedgeagent.wallet.WalletModule
import com.sudoplatform.sudodiedgeagent.wallet.types.WalletConfiguration
import com.sudoplatform.sudodiedgeagentexample.R
import com.sudoplatform.sudodiedgeagentexample.Routes
import com.sudoplatform.sudodiedgeagentexample.SingleSudoManager
import com.sudoplatform.sudodiedgeagentexample.ui.theme.SCREEN_PADDING
import com.sudoplatform.sudodiedgeagentexample.ui.theme.SudoDIEdgeAgentExampleTheme
import com.sudoplatform.sudodiedgeagentexample.utils.showToast
import com.sudoplatform.sudodiedgeagentexample.utils.showToastOnFailure
import com.sudoplatform.sudologging.Logger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** ID of the wallet to use */
private const val WALLET_ID = "wallet"

/**
 * Passphrase of the wallet to use.
 *
 * NOTE: hardcoded wallet passphrases should NOT be used in a production application.
 * This passphrase is used to encrypt the sensitive data inside the wallet storage.
 * Ideally the passphrase should be a long random string, which, for instance, could be managed
 * by the application and stored in the device secure enclave. It is recommended that application
 * developers protect this passphrase behind some other authentication mechanism, such as
 * a user's biometrics.
 */
private const val WALLET_PASSPHRASE = "S3cur3P@ssPhr@se!"

@Composable
fun RegisterScreen(
    navController: NavController,
    agent: SudoDIEdgeAgent,
    sudoManager: SingleSudoManager,
    logger: Logger,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var isLoading by remember { mutableStateOf(false) }

    /**
     * Register/Sign-in a Sudo Platform user (for sake of Sudo DI Relay usage) and
     * create/open the edge agent's wallet.
     */
    fun signInAndUnlockWallet() = scope.launch {
        isLoading = true

        try {
            withContext(Dispatchers.IO) {
                sudoManager.signInUser()
            }
            val config = WalletConfiguration(WALLET_ID, WALLET_PASSPHRASE)
            if (!agent.wallet.exists(WALLET_ID)) {
                agent.wallet.create(config)
            }
            agent.wallet.open(config)

            isLoading = false
            navController.navigate(Routes.HOME)
            return@launch
        } catch (e: Exception) {
            when (e) {
                is WalletModule.OpenException.WalletAlreadyOpenException -> {
                    isLoading = false
                    navController.navigate(Routes.HOME)
                    return@launch
                }
            }
            logger.error("Failed to sign in or unlock wallet: $e")
            showToast("Failed to sign in or unlock wallet: $e", context)
        }

        isLoading = false
    }

    /**
     * Reset the app state by shutting down the agent (stop, unsubscribe and close), deleting
     * the wallet, and de-registering the sudo platform user. Each throwing step wrapped in a
     * `runCatching` to not prevent subsequent reset steps from proceeding.
     */
    fun reset() = scope.launch {
        isLoading = true

        agent.stop()
        agent.unsubscribeAll()

        runCatching {
            agent.wallet.close()
        }
        runCatching {
            agent.wallet.delete(WalletConfiguration(WALLET_ID, WALLET_PASSPHRASE))
        }.showToastOnFailure(context, logger)

        // deregister and reset sudo clients
        runCatching {
            sudoManager.deregisterAndReset()
        }.showToastOnFailure(context, logger)

        isLoading = false
    }

    RegisterScreenView(
        unlock = { signInAndUnlockWallet() },
        reset = { reset() },
        isLoading = isLoading,
    )
}

/**
 * UI for the "Register screen". Acts as the landing screen for the app, where
 * users can navigate onwards to the "Home screen" by registering/signing in and
 * creating/opening their agent's wallet.
 */
@Composable
fun RegisterScreenView(
    unlock: () -> Unit,
    reset: () -> Unit,
    isLoading: Boolean,
) {
    var showResetConfirmation by remember { mutableStateOf(false) }

    // show the reset confirmation alert dialog if being requested
    if (showResetConfirmation) {
        AlertDialog(
            onDismissRequest = { showResetConfirmation = false },
            confirmButton = {
                TextButton(onClick = {
                    reset()
                    showResetConfirmation = false
                }) {
                    Text(text = "Accept")
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetConfirmation = false }) {
                    Text(text = "Cancel")
                }
            },
            title = { Text(text = "Confirm Reset") },
            text = { Text(text = "Resetting will clear all data, are you sure?") },
        )
    }

    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
        TextButton(
            onClick = { showResetConfirmation = true },
            enabled = !isLoading,
        ) {
            Text("Reset")
        }
    }
    Column(
        Modifier
            .fillMaxSize()
            .padding(horizontal = SCREEN_PADDING),
    ) {
        Image(
            modifier = Modifier.padding(top = 128.dp),
            painter = painterResource(id = R.drawable.sudoplatform),
            contentDescription = "Sudo Platform Logo",
        )
        Text(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            text = "Sudo DI Edge Agent Example App",
            textAlign = TextAlign.Center,
        )

        Column(
            Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            if (isLoading) {
                CircularProgressIndicator()
            } else {
                Button(
                    onClick = { unlock() },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(text = "Unlock")
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun DefaultPreview() {
    SudoDIEdgeAgentExampleTheme {
        RegisterScreenView(
            unlock = {},
            reset = {},
            isLoading = false,
        )
    }
}
