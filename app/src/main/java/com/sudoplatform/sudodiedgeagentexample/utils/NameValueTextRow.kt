/*
 * Copyright © 2023 Anonyome Labs, Inc. All rights reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.sudoplatform.sudodiedgeagentexample.utils

import android.annotation.SuppressLint
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

/**
 * Helper UI element for displaying a name and value pair in a row arrangement, where the
 * [name] has bold text.
 */
@Composable
fun NameValueTextRow(
    name: String,
    value: String,
    @SuppressLint("ModifierParameter") modifier: Modifier = Modifier.padding(
        vertical = 2.dp,
    ),
    handleValueTextOverflow: Boolean = false,
) {
    val maxLines = if (handleValueTextOverflow) { 1 } else { Int.MAX_VALUE }
    Row(modifier = modifier) {
        Text(text = "$name: ", fontWeight = FontWeight.Bold)
        Text(text = value, maxLines = maxLines, overflow = TextOverflow.Ellipsis)
    }
}
