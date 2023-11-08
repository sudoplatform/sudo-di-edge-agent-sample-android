/*
 * Copyright © 2023 Anonyome Labs, Inc. All rights reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.sudoplatform.sudodiedgeagentexample.utils

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DismissDirection
import androidx.compose.material3.DismissValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.SwipeToDismiss
import androidx.compose.material3.rememberDismissState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * Helper card for rendering cards that can be swiped to delete. Where [onDelete] is called
 * when a swipe to delete action is invoked. Used for items in list views for this app.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SwipeToDeleteCard(onDelete: () -> Unit, content: @Composable ColumnScope.() -> Unit) {
    val dismissState = rememberDismissState(
        positionalThreshold = {
            150.dp.toPx()
        },
        confirmValueChange = {
            val wasDismissed = it == DismissValue.DismissedToEnd
            if (wasDismissed) {
                onDelete()
            }
            wasDismissed
        },
    )
    SwipeToDismiss(
        state = dismissState,
        directions = setOf(DismissDirection.StartToEnd),
        background = {
            val bgColor by animateColorAsState(
                when (dismissState.targetValue) {
                    DismissValue.DismissedToEnd -> Color.Red
                    else -> Color.LightGray
                },
                label = "Swipe background colour",
            )
            val iconColor by animateColorAsState(
                when (dismissState.targetValue) {
                    DismissValue.DismissedToEnd -> Color.White
                    else -> Color.Black
                },
                label = "Swipe delete icon colour",
            )
            Box(
                Modifier
                    .fillMaxSize()
                    .background(bgColor, CardDefaults.shape),
                contentAlignment = Alignment.CenterStart,
            ) {
                Icon(
                    Icons.Filled.Delete,
                    modifier = Modifier.padding(start = 8.dp),
                    contentDescription = "Delete item",
                    tint = iconColor,
                )
            }
        },
        dismissContent = {
            Card(
                Modifier
                    .fillMaxWidth(),
            ) {
                content()
            }
        },
    )
}
