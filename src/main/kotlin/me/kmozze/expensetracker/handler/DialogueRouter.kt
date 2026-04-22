package me.kmozze.expensetracker.handler

import me.kmozze.expensetracker.handler.statehandler.StateHandler
import me.kmozze.expensetracker.model.domain.HandlerResult
import me.kmozze.expensetracker.model.domain.UserInput
import me.kmozze.expensetracker.model.domain.UserState
import me.kmozze.expensetracker.service.UserSessionService
import org.springframework.stereotype.Component
import kotlin.reflect.KClass

@Component
class DialogueRouter(
    private val userSessionService: UserSessionService,
    private val startCommandHandler: StartCommandHandler,
    private val unknownCommandHandler: UnknownCommandHandler,
    private val errorHandler: ErrorHandler,
    stateHandlers: List<StateHandler>,
) {
    private val handlersByState: Map<KClass<out UserState>, StateHandler> =
        stateHandlers.associateBy { it.supportedStateClass }

    fun process(input: UserInput): HandlerResult {
        return try {
            if (input.text?.equals("/start", ignoreCase = true) == true) {
                userSessionService.clear(input.userId)
                return startCommandHandler.handle(input)
            }

            val currentState = userSessionService.getState(input.userId)

            val handler = handlersByState[currentState::class] ?: unknownCommandHandler

            val result = handler.handle(input)

            if (result.nextState != null) {
                userSessionService.setState(input.userId, result.nextState)
            }

            result
        } catch (e: Exception) {
            errorHandler.handle(input.userId, e)
        }
    }
}
