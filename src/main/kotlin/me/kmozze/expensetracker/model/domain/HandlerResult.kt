package me.kmozze.expensetracker.model.domain

data class HandlerResult(
    val response: HandlerResponse,
    val nextState: UserState? = null,
)
