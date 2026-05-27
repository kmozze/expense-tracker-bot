package me.kmozze.expensetracker.handler

import me.kmozze.expensetracker.model.domain.BotText
import me.kmozze.expensetracker.model.domain.HandlerResponse
import me.kmozze.expensetracker.model.domain.HandlerResult
import me.kmozze.expensetracker.model.domain.UserInput
import org.springframework.stereotype.Component

@Component
class UnknownCommandHandler {
    fun handle(input: UserInput): HandlerResult =
        HandlerResult(
            response =
                HandlerResponse(
                    text = BotText.UnknownCommand,
                    actions = listOf(),
                ),
        )
}
