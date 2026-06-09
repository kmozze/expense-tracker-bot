package me.kmozze.expensetracker.model.domain.bot

sealed class ResponseDelivery {
    data object SendNewMessage : ResponseDelivery()

    data class EditMessage(
        val messageId: Int,
    ) : ResponseDelivery()
}
