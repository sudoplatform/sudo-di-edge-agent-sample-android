/*
 * Copyright © 2023 Anonyome Labs, Inc. All rights reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.sudoplatform.sudodiedgeagentexample.utils

import androidx.compose.runtime.snapshots.SnapshotStateList

/**
 * Helper extension to replace the entire current list items with the [newList].
 */
fun <T> SnapshotStateList<T>.swapList(newList: List<T>) {
    clear()
    addAll(newList)
}
