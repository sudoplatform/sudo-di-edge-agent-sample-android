/*
 * Copyright © 2024 Anonyome Labs, Inc. All rights reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.sudoplatform.sudodiedgeagentexample.utils

import android.graphics.Bitmap
import android.graphics.Color
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.tooling.preview.Preview
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import com.sudoplatform.sudodiedgeagentexample.ui.theme.SCREEN_PADDING
import com.sudoplatform.sudodiedgeagentexample.ui.theme.SudoDIEdgeAgentExampleTheme

/**
 * Displays a QR code for [content] at the desired [size].
 *
 * The QR code will render in the background before displaying. If this takes any amount of time, a circular progress
 * indicator will display until the QR code is rendered.
 */
@Composable
fun QrCodeImage(content: String) {
    fun getQrCodeBitmap(content: String): ImageBitmap {
        val size = 1028
        val hints = hashMapOf<EncodeHintType, Int>().also {
            it[EncodeHintType.MARGIN] = 1
        }
        val bits = QRCodeWriter().encode(content, BarcodeFormat.QR_CODE, size, size, hints)
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.RGB_565).also {
            for (x in 0 until size) {
                for (y in 0 until size) {
                    it.setPixel(
                        x,
                        y,
                        if (bits[x, y]) Color.BLACK else Color.WHITE,
                    )
                }
            }
        }

        return bitmap.asImageBitmap()
    }

    Image(
        modifier = Modifier.fillMaxWidth(),
        bitmap = getQrCodeBitmap(content),
        contentDescription = "QR Code",
    )
}

@Preview(showBackground = true)
@Composable
private fun Preview() {
    SudoDIEdgeAgentExampleTheme {
        Column(
            Modifier
                .fillMaxSize()
                .padding(SCREEN_PADDING),
        ) {
            QrCodeImage(content = "sample sample sample sample")
        }
    }
}
