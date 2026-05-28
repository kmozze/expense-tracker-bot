package me.kmozze.expensetracker.model.domain

sealed class ResponseDelivery {
    data object SendNewMessage : ResponseDelivery()

    data class EditMessage(
        val messageId: Int,
    ) : ResponseDelivery()
}
