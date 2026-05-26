package me.kmozze.expensetracker.model.domain

sealed class ResponseDelivery {
    data object SendNewMessage : ResponseDelivery()

    data class EditMessage(
        val messageId: Int,
    ) : ResponseDelivery()
}

internal fun ResponseDelivery.messageIdOrNull(): Int? =
    when (this) {
        is ResponseDelivery.EditMessage -> messageId
        ResponseDelivery.SendNewMessage -> null
    }
