/*
 * Copyright © 2023 Anonyome Labs, Inc. All rights reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.sudoplatform.sudodiedgeagentexample.connection.exchange

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.util.Base64
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Popup
import androidx.core.content.ContextCompat
import androidx.navigation.NavController
import com.sudoplatform.sudodiedgeagent.SudoDIEdgeAgent
import com.sudoplatform.sudodiedgeagent.connections.exchange.types.ConnectionExchange
import com.sudoplatform.sudodiedgeagent.connections.exchange.types.ConnectionExchangeState
import com.sudoplatform.sudodiedgeagent.subscriptions.AgentEventSubscriber
import com.sudoplatform.sudodiedgeagent.types.Routing
import com.sudoplatform.sudodiedgeagentexample.SingleSudoManager
import com.sudoplatform.sudodiedgeagentexample.relay.SudoDIRelayMessageSource
import com.sudoplatform.sudodiedgeagentexample.ui.theme.SCREEN_PADDING
import com.sudoplatform.sudodiedgeagentexample.utils.showToast
import com.sudoplatform.sudodiedgeagentexample.utils.showToastOnFailure
import com.sudoplatform.sudodirelay.SudoDIRelayClient
import com.sudoplatform.sudodirelay.types.Postbox
import com.sudoplatform.sudologging.Logger
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * UI for a connection invitation scanner screen. Displays a QR Code scanner designed for
 * scanning Connection Protocol 0160 invitations. On successful scan, the invitation details
 * are displayed within an alert dialog, along with an option to accept or reject the connection.
 *
 * see here: https://github.com/hyperledger/aries-rfcs/tree/main/features/0160-connection-protocol#0-invitation-to-connect
 */
@Composable
fun ConnectionInvitationScannerScreen(
    navController: NavController,
    agent: SudoDIEdgeAgent,
    sudoManager: SingleSudoManager,
    logger: Logger,
) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    var incomingInvite: ConnectionExchange? by remember { mutableStateOf(null) }

    /** Single access mutex to ensure that only 1 scanned QR code is processed at a time */
    val processingScannedInvitationLock = Mutex()
    var isAcceptingInvitation by remember { mutableStateOf(false) }

    /**
     * When the composable initializes, subscribe to agent events for connection updates.
     * If the connection update is for a connection entering the [ConnectionExchangeState.INVITATION]
     * state (i.e. just created), then set the `incomingInvite` state to this new [ConnectionExchange],
     * in turn, this state change will trigger the [AcceptInvitationAlertDialog] to display.
     *
     * When the composable disposes, unsubscribe from the agent events.
     */
    DisposableEffect(key1 = Unit) {
        val subscriptionId = agent.subscribeToAgentEvents(object : AgentEventSubscriber {
            override fun connectionExchangeStateChanged(connectionExchange: ConnectionExchange) {
                if (connectionExchange.state != ConnectionExchangeState.INVITATION) return
                incomingInvite = connectionExchange
            }
        })

        onDispose {
            agent.unsubscribeToAgentEvents(subscriptionId)
        }
    }

    /**
     * Handler for processing the scanned QR Code string. The string is expected to
     * be in the format encoding of an invitation URL as described in the 0160 Connection Protocol
     * RFC: https://github.com/hyperledger/aries-rfcs/tree/main/features/0160-connection-protocol#0-invitation-to-connect.
     *
     * Particularly, this involves parsing the [scannedString] as a Uri, then extracting the `c_i`
     * query parameter, then base64 decoding the query parameter value to retrieved the bytes of
     * the invitation message. The invitation message is then passed into the agent for processing
     * via [SudoDIEdgeAgent.receiveMessage].
     */
    fun tryProcessInvitationMessageFromQrScan(scannedString: String) = scope.launch {
        processingScannedInvitationLock.withLock {
            // only process if not another connection invitation pending
            if (incomingInvite != null) return@launch

            runCatching {
                val scannedUri = Uri.parse(scannedString)
                val base64Invitation = scannedUri.getQueryParameter("c_i")
                    ?: throw Exception("Missing 'c_i' query parameter")

                val invitation = Base64.decode(base64Invitation, Base64.URL_SAFE)

                agent.receiveMessage(invitation)
            }.showToastOnFailure(
                context,
                logger,
                "Failed to process QR Code as connection invitation",
            )
        }
    }

    /**
     * Attempt to accept a [ConnectionExchange] in the invitation state by its ID.
     *
     * This involves creating a [Postbox] with the [SudoDIRelayClient], then using the
     * [SudoDIRelayMessageSource] to translate the [Postbox] into a [Routing], which
     * is then used to inform the connection peer how they should communicate to this
     * agent via the relay service.
     */
    fun acceptConnection(id: String) = scope.launch {
        showToast("Accepting connection", context)
        runCatching {
            isAcceptingInvitation = true

            val token = sudoManager.getSudoOwnershipProofToken()
            val postbox =
                sudoManager.relayClient.createPostbox(id, token)

            val routing = SudoDIRelayMessageSource.routingFromPostbox(postbox)

            agent.connections.exchange.acceptConnection(id, routing)

            showToast("Connection accepted", context)
            isAcceptingInvitation = false
            incomingInvite = null
            navController.popBackStack()
        }.showToastOnFailure(context, logger, "Failed to accept connection")

        isAcceptingInvitation = false
    }

    /**
     * Handler for the 'rejection' of a connection invitation. Rejection is done by
     * simply deleting the [ConnectionExchange]. Alternatively, the connection invitation
     * can just be ignored, however that will result in a [ConnectionExchange] left pending
     * within the agent.
     */
    fun rejectConnection(id: String) = scope.launch {
        runCatching {
            agent.connections.exchange.deleteById(id)
        }.showToastOnFailure(context, logger, "Failed to delete connection")
        incomingInvite = null
    }

    // if an incoming invitation is set to non-null via the subscription, then display the
    // alert dialog.
    incomingInvite?.let { connEx ->
        AcceptInvitationAlertDialog(
            connEx,
            onAccept = {
                acceptConnection(connEx.connectionExchangeId)
            },
            isLoading = isAcceptingInvitation,
            onClose = {
                rejectConnection(connEx.connectionExchangeId)
            },
        )
    }

    /**
     * Camera permission and lifecycle management for sake of QR Code scanner.
     */
    // --------------------------------------------------
    val lifecycleOwner = LocalLifecycleOwner.current
    val cameraProviderFeature = remember {
        ProcessCameraProvider.getInstance(context)
    }

    var hasCamPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.CAMERA,
            ) == PackageManager.PERMISSION_GRANTED,
        )
    }
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { granted ->
            hasCamPermission = granted
        },
    )
    LaunchedEffect(key1 = Unit) {
        launcher.launch(Manifest.permission.CAMERA)
    }
    // --------------------------------------------------

    // show camera scanner if not currently in the process of accepting an invitation.
    if (!isAcceptingInvitation) {
        Column(
            Modifier
                .fillMaxSize()
                .padding(SCREEN_PADDING),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Row(Modifier.fillMaxWidth(0.7f)) {
                if (hasCamPermission) {
                    AndroidView(
                        factory = { context ->
                            val previewView = PreviewView(context)
                            val preview = Preview.Builder().build()
                            val selector = CameraSelector.Builder()
                                .requireLensFacing(CameraSelector.LENS_FACING_BACK)
                                .build()
                            preview.setSurfaceProvider(previewView.surfaceProvider)
                            val imageAnalysis = ImageAnalysis.Builder()
                                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                                .build()
                            imageAnalysis.setAnalyzer(
                                ContextCompat.getMainExecutor(context),
                                QrCodeAnalyser(
                                    onScannedString = { data ->
                                        tryProcessInvitationMessageFromQrScan(data)
                                    },
                                ),
                            )
                            try {
                                cameraProviderFeature.get().bindToLifecycle(
                                    lifecycleOwner,
                                    selector,
                                    preview,
                                    imageAnalysis,
                                )
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                            previewView
                        },
                    )
                } else {
                    Text(text = "Could not access camera.. check permissions")
                }
            }
            Text(
                text = "Scan a connection invitation to continue..",
                Modifier.padding(vertical = 32.dp),
            )
        }
    }
}

/**
 * [AlertDialog] which displays the details of an incoming [ConnectionExchange] (i.e. invitation).
 * Displays the option to accept or reject the connection.
 */
@Composable
private fun AcceptInvitationAlertDialog(
    incomingConnEx: ConnectionExchange,
    onClose: () -> Unit,
    onAccept: () -> Unit,
    isLoading: Boolean,
) {
    if (isLoading) {
        Popup(alignment = Alignment.Center, onDismissRequest = {}) {
            CircularProgressIndicator()
        }
        return
    }

    AlertDialog(
        onDismissRequest = onClose,
        confirmButton = {
            TextButton(onClick = onAccept) {
                Text(text = "Accept")
            }
        },
        dismissButton = {
            TextButton(onClick = onClose) {
                Text(text = "Cancel")
            }
        },
        title = { Text(text = "Incoming Invitation") },
        text = { Text(text = "From label: ${incomingConnEx.theirLabel}") },
    )
}
