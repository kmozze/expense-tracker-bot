package me.kmozze.expensetracker.handler.statehandler

import me.kmozze.expensetracker.handler.UserInputHandler
import me.kmozze.expensetracker.model.domain.HandlerResult
import me.kmozze.expensetracker.model.domain.UserInput
import me.kmozze.expensetracker.model.domain.UserState
import kotlin.reflect.KClass

interface StateHandler : UserInputHandler {
    val supportedStateClass: KClass<out UserState>

    override fun handle(input: UserInput): HandlerResult =
        throw IllegalStateException("${this::class.simpleName} requires current user state")

    fun handle(
        input: UserInput,
        currentState: UserState,
    ): HandlerResult = handle(input)
}
