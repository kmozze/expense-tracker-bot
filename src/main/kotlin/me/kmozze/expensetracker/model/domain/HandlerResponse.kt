package me.kmozze.expensetracker.model.domain

import me.kmozze.expensetracker.model.domain.Action
import me.kmozze.expensetracker.model.domain.Message

data class HandlerResponse(
    val message: Message,
    val actions: List<Action>,
)
