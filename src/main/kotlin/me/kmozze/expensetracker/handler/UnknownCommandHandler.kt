package me.kmozze.expensetracker.handler

import me.kmozze.expensetracker.model.domain.HandlerResponse
import me.kmozze.expensetracker.model.domain.HandlerResult
import me.kmozze.expensetracker.model.domain.Message
import me.kmozze.expensetracker.model.domain.UserInput
import org.springframework.stereotype.Component

@Component
class UnknownCommandHandler : UserInputHandler {
    override fun handle(input: UserInput): HandlerResult =
        HandlerResult(
            response =
                HandlerResponse(
                    message = Message.UnknownCommand,
                    actions = listOf(),
                ),
        )
}
