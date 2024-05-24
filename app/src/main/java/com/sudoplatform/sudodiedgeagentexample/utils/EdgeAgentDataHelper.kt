/*
 * Copyright © 2024 Anonyome Labs, Inc. All rights reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.sudoplatform.sudodiedgeagentexample.utils

import com.sudoplatform.sudodiedgeagent.plugins.messagesource.ReceivedMessageMetadata
import java.time.Instant

fun makeReceivedMessageMetadata(): ReceivedMessageMetadata {
    return ReceivedMessageMetadata(
        receivedTime = Instant.now(),
    )
}
