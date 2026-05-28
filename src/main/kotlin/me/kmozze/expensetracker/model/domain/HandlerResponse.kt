package me.kmozze.expensetracker.model.domain

data class HandlerResponse(
    val outgoingMessages: List<OutgoingMessage>,
    val callbackAnswer: CallbackAnswer? = null,
)
