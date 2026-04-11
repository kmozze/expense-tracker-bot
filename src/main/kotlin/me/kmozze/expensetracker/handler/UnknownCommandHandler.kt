package me.kmozze.expensetracker.handler

import me.kmozze.expensetracker.model.domain.HandlerResponse
import me.kmozze.expensetracker.model.domain.Message
import me.kmozze.expensetracker.model.domain.UserInput
import org.springframework.stereotype.Component

@Component
class UnknownCommandHandler : IHandler {
    override fun handle(input: UserInput): HandlerResponse =
        HandlerResponse(
            message = Message.UnknownCommand,
            actions = listOf(),
        )
}
