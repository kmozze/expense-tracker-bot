package me.kmozze.expensetracker.model.domain.bot

data class HandlerResponse(
    val outgoingMessages: List<OutgoingMessage>,
    val callbackAnswer: CallbackAnswer? = null,
    val nextState: UserState? = null,
)
