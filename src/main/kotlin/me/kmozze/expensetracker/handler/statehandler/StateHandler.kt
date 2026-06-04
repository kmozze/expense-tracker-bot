package me.kmozze.expensetracker.handler.statehandler

import me.kmozze.expensetracker.model.domain.HandlerResponse
import me.kmozze.expensetracker.model.domain.UserInput
import me.kmozze.expensetracker.model.domain.UserState
import kotlin.reflect.KClass

interface StateHandler {
    val supportedStateClass: KClass<out UserState>

    fun handle(
        input: UserInput,
        currentState: UserState,
    ): HandlerResponse
}
