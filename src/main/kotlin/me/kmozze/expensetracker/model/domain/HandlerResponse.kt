package me.kmozze.expensetracker.model.domain

data class HandlerResponse(
    val outgoingMessages: List<OutgoingMessage>,
    val callbackAnswer: CallbackAnswer? = null,
) {
    constructor(
        text: BotText,
        actions: List<BotAction>,
        delivery: ResponseDelivery = ResponseDelivery.SendNewMessage,
        callbackAnswer: CallbackAnswer? = null,
    ) : this(
        outgoingMessages = listOf(OutgoingMessage(text = text, actions = actions, delivery = delivery)),
        callbackAnswer = callbackAnswer,
    )

    val text: BotText
        get() = singleOutgoingMessage().text

    val actions: List<BotAction>
        get() = singleOutgoingMessage().actions

    val delivery: ResponseDelivery
        get() = singleOutgoingMessage().delivery

    private fun singleOutgoingMessage(): OutgoingMessage = outgoingMessages.single()
}
