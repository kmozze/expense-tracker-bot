package me.kmozze.expensetracker.handler.statehandler.common

import me.kmozze.expensetracker.model.domain.bot.HandlerResponse
import me.kmozze.expensetracker.model.domain.bot.UserInput
import me.kmozze.expensetracker.model.domain.bot.UserState
import kotlin.reflect.KClass

interface StateHandler {
    val supportedStateClass: KClass<out UserState>

    fun handle(
        input: UserInput,
        currentState: UserState,
    ): HandlerResponse
}
