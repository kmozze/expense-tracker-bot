package me.kmozze.expensetracker.handler

import me.kmozze.expensetracker.model.domain.bot.BotText
import me.kmozze.expensetracker.model.domain.bot.HandlerResponse
import me.kmozze.expensetracker.model.domain.bot.UserInput
import org.springframework.stereotype.Component

@Component
class UnknownCommandHandler {
    fun handle(input: UserInput): HandlerResponse =
        handlerResponse(
            message =
                outgoingMessage(
                    text = BotText.UnknownCommand,
                    actions = emptyList(),
                ),
        )
}
