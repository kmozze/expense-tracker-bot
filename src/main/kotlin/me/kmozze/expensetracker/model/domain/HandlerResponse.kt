package me.kmozze.expensetracker.model.domain

data class HandlerResponse(
    val message: Message,
    val actions: List<Action>,
)
