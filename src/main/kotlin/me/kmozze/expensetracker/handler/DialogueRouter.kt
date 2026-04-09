package me.kmozze.expensetracker.handler

import me.kmozze.expensetracker.model.domain.HandlerResponse
import me.kmozze.expensetracker.model.domain.UserInput
import org.springframework.stereotype.Component

@Component
class DialogueRouter(
    private val startHandler: StartHandler,
    private val unknownCommandHandler: UnknownCommandHandler,
) {
    fun process(input: UserInput): HandlerResponse =
        when {
            input.text.equals("/start", ignoreCase = true) -> {
                startHandler.handle(input)
            }
            else -> unknownCommandHandler.handle(input)
        }
}
