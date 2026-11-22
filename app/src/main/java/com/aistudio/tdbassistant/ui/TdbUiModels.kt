package com.aistudio.tdbassistant.ui

import com.aistudio.tdbassistant.domain.MessagingChannel

data class PendingContactsRequest(
    val channel: MessagingChannel = MessagingChannel.WHATSAPP,
    val recipient: String,
    val message: String,
    val isCall: Boolean = false
)

data class PendingMessageConfirmation(
    val channel: MessagingChannel,
    val recipient: String,
    val message: String,
    val awaitingMessageInput: Boolean = false
)

data class PendingCallConfirmation(
    val target: String
)

data class FocusTimerState(
    val isRunning: Boolean = false,
    val isBreak: Boolean = false,
    val focusMinutes: Int = 25,
    val breakMinutes: Int = 5,
    val remainingSeconds: Int = 25 * 60,
    val linkedGoalTitle: String? = null,
    val linkedGoalId: String? = null
) {
    val totalSeconds: Int
        get() = (if (isBreak) breakMinutes else focusMinutes).coerceAtLeast(1) * 60

    val progress: Float
        get() = 1f - (remainingSeconds.toFloat() / totalSeconds.toFloat()).coerceIn(0f, 1f)
}
