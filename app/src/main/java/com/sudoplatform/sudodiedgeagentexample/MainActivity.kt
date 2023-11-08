/*
 * Copyright © 2023 Anonyome Labs, Inc. All rights reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.sudoplatform.sudodiedgeagentexample

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.annotation.OptIn
import androidx.camera.core.ExperimentalGetImage
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.sudoplatform.sudodiedgeagent.SudoDIEdgeAgent
import com.sudoplatform.sudodiedgeagentexample.connection.ConnectionsScreen
import com.sudoplatform.sudodiedgeagentexample.connection.exchange.ConnectionExchangeScreen
import com.sudoplatform.sudodiedgeagentexample.connection.exchange.ConnectionInvitationScannerScreen
import com.sudoplatform.sudodiedgeagentexample.credential.CredentialInfoScreen
import com.sudoplatform.sudodiedgeagentexample.credential.CredentialsScreen
import com.sudoplatform.sudodiedgeagentexample.credential.exchange.CredentialExchangeInfoScreen
import com.sudoplatform.sudodiedgeagentexample.credential.exchange.CredentialExchangeScreen
import com.sudoplatform.sudodiedgeagentexample.home.HomeScreen
import com.sudoplatform.sudodiedgeagentexample.proof.exchanges.ProofExchangePresentationScreen
import com.sudoplatform.sudodiedgeagentexample.proof.exchanges.ProofExchangeScreen
import com.sudoplatform.sudodiedgeagentexample.register.RegisterScreen
import com.sudoplatform.sudodiedgeagentexample.ui.theme.SudoDIEdgeAgentExampleTheme
import com.sudoplatform.sudologging.Logger

object Routes {
    const val REGISTER = "register"
    const val HOME = "home"
    const val CONNECTION_EXCHANGES = "connExs"
    const val CONNECTION_INVITATION_SCANNER = "connInvitationScanner"
    const val CONNECTIONS = "conns"
    const val CREDENTIAL_EXCHANGES = "credExs"
    const val CREDENTIAL_EXCHANGE_INFO = "credExInfo"
    const val CREDENTIALS = "creds"
    const val CREDENTIAL_INFO = "credInfo"
    const val PROOF_EXCHANGES = "proofExs"
    const val PROOF_EXCHANGE_PRESENTATION = "proofExPresentation"
}

class MainActivity : ComponentActivity() {

    private lateinit var app: App

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        app = this.application as App

        setContent {
            SudoDIEdgeAgentExampleTheme {
                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background,
                ) {
                    MainNavigation(app.agent, app.sudoManager, app.logger)
                }
            }
        }
    }
}

@OptIn(ExperimentalGetImage::class)
@Composable
fun MainNavigation(
    agent: SudoDIEdgeAgent,
    sudoManager: SingleSudoManager,
    logger: Logger,
) {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = Routes.REGISTER) {
        composable(Routes.REGISTER) {
            RegisterScreen(
                navController = navController,
                agent = agent,
                sudoManager = sudoManager,
                logger = logger,
            )
        }
        composable(Routes.HOME) {
            HomeScreen(
                navController = navController,
                agent = agent,
                sudoManager = sudoManager,
                logger = logger,
            )
        }
        composable(Routes.CONNECTION_EXCHANGES) {
            ConnectionExchangeScreen(
                navController = navController,
                agent = agent,
                sudoManager = sudoManager,
                logger = logger,
            )
        }
        composable(Routes.CONNECTION_INVITATION_SCANNER) {
            ConnectionInvitationScannerScreen(
                navController = navController,
                agent = agent,
                sudoManager = sudoManager,
                logger = logger,
            )
        }
        composable(Routes.CONNECTIONS) {
            ConnectionsScreen(
                agent = agent,
                logger = logger,
            )
        }
        composable(Routes.CREDENTIAL_EXCHANGES) {
            CredentialExchangeScreen(
                navController = navController,
                agent = agent,
                logger = logger,
            )
        }
        composable("${Routes.CREDENTIAL_EXCHANGE_INFO}/{credExId}") { backStackEntry ->
            val credExId = backStackEntry.arguments?.getString("credExId")
            checkNotNull(credExId)

            CredentialExchangeInfoScreen(
                navController = navController,
                credentialExchangeId = credExId,
                agent = agent,
                logger = logger,
            )
        }
        composable(Routes.CREDENTIALS) {
            CredentialsScreen(
                navController = navController,
                agent = agent,
                logger = logger,
            )
        }
        composable("${Routes.CREDENTIAL_INFO}/{credId}") { backStackEntry ->
            val credId = backStackEntry.arguments?.getString("credId")
            checkNotNull(credId)

            CredentialInfoScreen(
                credentialId = credId,
                agent = agent,
                logger = logger,
            )
        }
        composable(Routes.PROOF_EXCHANGES) {
            ProofExchangeScreen(
                navController = navController,
                agent = agent,
                logger = logger,
            )
        }
        composable("${Routes.PROOF_EXCHANGE_PRESENTATION}/{proofExId}") { backStackEntry ->
            val proofExId = backStackEntry.arguments?.getString("proofExId")
            checkNotNull(proofExId)

            ProofExchangePresentationScreen(
                navController = navController,
                proofExchangeId = proofExId,
                agent = agent,
                logger = logger,
            )
        }
    }
}
