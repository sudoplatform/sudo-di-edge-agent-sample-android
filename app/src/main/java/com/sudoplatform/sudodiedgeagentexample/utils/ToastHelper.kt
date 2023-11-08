/*
 * Copyright © 2023 Anonyome Labs, Inc. All rights reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.sudoplatform.sudodiedgeagentexample.utils

import android.content.Context
import android.widget.Toast
import com.sudoplatform.sudologging.Logger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Globally hold a single (lazily initialized) toast. A single toast is used to avoid
 * the UI being flooded with multiple stacking toasts (in cases where toasts are being
 * rapidly shown).
 */
private var singleToast: Toast? = null

/**
 * Show the provided [text] in a Toast on the screen using the main dispatcher
 * in the provided [context].
 */
suspend fun showToast(text: String, context: Context, duration: Int = Toast.LENGTH_LONG) {
    withContext(Dispatchers.Main) {
        singleToast?.let {
            it.cancel()
            it.setText(text)
            it.duration = duration
            it.show()
            return@withContext
        }

        val newToast = Toast.makeText(context, text, duration)
        newToast.show()
        singleToast = newToast
    }
}

/**
 * Given a [Result], if the result is a failure, display a toast with the given [errorMessage]
 * and string exception/throwable of the failure.
 */
suspend fun <R> Result<R>.showToastOnFailure(
    context: Context,
    logger: Logger,
    errorMessage: String = "",
    duration: Int = Toast.LENGTH_SHORT,
) {
    onFailure { e ->
        logger.error("$errorMessage: $e")
        showToast("$errorMessage: $e", context, duration)
    }
}
