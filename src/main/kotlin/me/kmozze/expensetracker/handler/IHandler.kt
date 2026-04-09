package me.kmozze.expensetracker.handler

import me.kmozze.expensetracker.model.domain.HandlerResponse
import me.kmozze.expensetracker.model.domain.UserInput

interface IHandler {
    fun handle(input: UserInput): HandlerResponse
}
