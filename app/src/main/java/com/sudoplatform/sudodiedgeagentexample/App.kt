/*
 * Copyright © 2023 Anonyome Labs, Inc. All rights reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.sudoplatform.sudodiedgeagentexample

import android.app.Application
import android.content.Context
import android.net.Uri
import com.sudoplatform.sudodiedgeagent.SudoDIEdgeAgent
import com.sudoplatform.sudodiedgeagent.configuration.AgentConfiguration
import com.sudoplatform.sudodiedgeagent.configuration.PeerConnectionConfiguration
import com.sudoplatform.sudologging.AndroidUtilsLogDriver
import com.sudoplatform.sudologging.LogLevel
import com.sudoplatform.sudologging.Logger
import java.io.File

class App : Application() {
    val logger = Logger("SudoDIEdgeAgentExample", AndroidUtilsLogDriver(LogLevel.DEBUG))

    /**
     * Globally held reference to a single agent instance, acting as the main entry
     * point for the Edge Agent SDK.
     */
    lateinit var agent: SudoDIEdgeAgent

    /**
     * Instance of a [SingleSudoManager] held globally in this application to manage and
     * make use of the single sudo that is being used.
     */
    lateinit var sudoManager: SingleSudoManager

    /**
     * Set of pending deep links captured by the app/intent which have not handled yet.
     * This may include `openid://` related deeplinks for credential & proof exchanges.
     */
    var pendingDeepLinks: MutableList<Uri> = mutableListOf()

    companion object {
        private const val LEDGER_GENESIS_FILE_NAME = "ledger_genesis.json"
    }

    override fun onCreate() {
        super.onCreate()

        sudoManager = SingleSudoManager(this, logger)

        initializeAgent()
    }

    /**
     * Initializes the global agent instance for this Application.
     */
    private fun initializeAgent() {
        /**
         * Use the default genesis file from within the assets of this app.
         *
         * Note that this is done for quick demonstration purposes, copying a file from assets
         * on every app launch is not efficient. Production applications may wish manage their files
         * more efficiently.
         */
        val genesisFile = persistFileFromAssets(this, LEDGER_GENESIS_FILE_NAME)

        /** Configure the agent to use this genesis file as it's "Ledger" and give the agent a public-facing label */
        val agentConfiguration = AgentConfiguration(
            genesisFiles = listOf(genesisFile),
            peerConnectionConfiguration = PeerConnectionConfiguration("Sudo Agent Android"),
        )

        /** construct the agent via convenience builder */
        agent = SudoDIEdgeAgent.builder().setContext(this).setAgentConfiguration(agentConfiguration)
            .setLogger(logger).build()
    }
}

/**
 * Copy an assets file into the files directory and return the [File] reference.
 * Will replace any existing file with the same name.
 */
fun persistFileFromAssets(context: Context, fileName: String): File =
    File(context.filesDir, fileName)
        .also {
            if (it.exists()) {
                it.delete()
            }
            it.outputStream().use { cache ->
                context.assets.open(fileName).use { inputStream ->
                    inputStream.copyTo(cache)
                }
            }
        }
