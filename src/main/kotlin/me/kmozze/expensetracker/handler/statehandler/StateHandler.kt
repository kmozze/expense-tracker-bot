package me.kmozze.expensetracker.handler.statehandler

import me.kmozze.expensetracker.handler.UserInputHandler
import me.kmozze.expensetracker.model.domain.UserState
import kotlin.reflect.KClass

interface StateHandler : UserInputHandler {
    val supportedStateClass: KClass<out UserState>
}
