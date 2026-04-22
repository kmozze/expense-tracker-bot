package me.kmozze.expensetracker.handler

import me.kmozze.expensetracker.model.domain.HandlerResult
import me.kmozze.expensetracker.model.domain.UserInput

interface UserInputHandler {
    fun handle(input: UserInput): HandlerResult
}
