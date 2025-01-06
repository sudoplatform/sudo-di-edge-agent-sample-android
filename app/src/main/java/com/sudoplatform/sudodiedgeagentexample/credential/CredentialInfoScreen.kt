/*
 * Copyright © 2023 Anonyome Labs, Inc. All rights reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.sudoplatform.sudodiedgeagentexample.credential

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.sudoplatform.sudodiedgeagent.SudoDIEdgeAgent
import com.sudoplatform.sudodiedgeagent.credentials.types.Credential
import com.sudoplatform.sudodiedgeagentexample.ui.theme.SCREEN_PADDING
import com.sudoplatform.sudodiedgeagentexample.ui.theme.SudoDIEdgeAgentExampleTheme
import com.sudoplatform.sudodiedgeagentexample.utils.PreviewDataHelper
import com.sudoplatform.sudodiedgeagentexample.utils.showToastOnFailure
import com.sudoplatform.sudologging.Logger

@Composable
fun CredentialInfoScreen(
    credentialId: String,
    agent: SudoDIEdgeAgent,
    logger: Logger,
) {
    val context = LocalContext.current

    var credential: UICredential? by remember { mutableStateOf(null) }

    /**
     * When this composable initializes, load the [UICredential] from the ID that was
     * passed in, and load any additional required data.
     * Displaying an error toast if the [Credential] cannot be found in the
     * agent (should be logically impossible/unlikely), or if construction of the
     * [UICredential] fails (i.e. fails to resolve additional data).
     */
    LaunchedEffect(key1 = Unit) {
        runCatching {
            val loadedCred = agent.credentials.getById(credentialId)
                ?: throw Exception("Could not find credential")

            credential = UICredential.fromCredential(agent, loadedCred)
        }.showToastOnFailure(context, logger, "Failed to load credential")
    }

    CredentialInfoScreenView(credential)
}

/**
 * UI for the "Credential Info Screen". Shows the details of a given [Credential].
 *
 * UI will display a loading spinner until [credential] becomes non-null.
 */
@Composable
fun CredentialInfoScreenView(
    credential: UICredential?,
) {
    Column(
        Modifier
            .fillMaxSize()
            .padding(SCREEN_PADDING),
    ) {
        LazyColumn(Modifier.weight(1.0f)) {
            item {
                if (credential == null) {
                    Column(
                        Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                    ) {
                        CircularProgressIndicator(Modifier.padding(vertical = 8.dp))
                    }
                } else {
                    when (credential) {
                        is UICredential.Anoncred -> AnoncredCredentialInfoColumn(
                            Modifier.weight(1.0f),
                            credential = credential,
                        )

                        is UICredential.W3C -> W3cCredentialInfoColumn(
                            credential = credential,
                        )

                        is UICredential.SdJwtVc -> SdJwtCredentialInfoColumn(
                            Modifier.weight(1.0f),
                            credential = credential,
                        )
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun PreviewAnoncred() {
    SudoDIEdgeAgentExampleTheme {
        CredentialInfoScreenView(
            credential = PreviewDataHelper.dummyUICredentialAnoncred(),
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun PreviewW3c() {
    SudoDIEdgeAgentExampleTheme {
        CredentialInfoScreenView(
            credential = PreviewDataHelper.dummyUICredentialW3C(),
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun PreviewSdJwt() {
    SudoDIEdgeAgentExampleTheme {
        CredentialInfoScreenView(
            credential = PreviewDataHelper.dummyUICredentialSdJwt(),
        )
    }
}
