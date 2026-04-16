package me.kmozze.expensetracker.handler.statehandler

import me.kmozze.expensetracker.handler.UserInputHandler
import me.kmozze.expensetracker.model.domain.UserState

interface StateHandler : UserInputHandler {
    val supportedState: UserState
}
