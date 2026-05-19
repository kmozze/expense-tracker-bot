package me.kmozze.expensetracker.model.domain

data class HandlerResponse(
    val message: BotMessage,
    val actions: List<BotAction>,
)
