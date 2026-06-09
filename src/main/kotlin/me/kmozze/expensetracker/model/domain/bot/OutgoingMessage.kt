package me.kmozze.expensetracker.model.domain.bot

data class OutgoingMessage(
    val text: BotText,
    val actions: List<BotAction>,
    val delivery: ResponseDelivery = ResponseDelivery.SendNewMessage,
)
