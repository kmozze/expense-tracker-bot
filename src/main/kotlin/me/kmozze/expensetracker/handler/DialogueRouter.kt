package me.kmozze.expensetracker.handler

import me.kmozze.expensetracker.handler.statehandler.StateHandler
import me.kmozze.expensetracker.model.domain.HandlerResult
import me.kmozze.expensetracker.model.domain.UserInput
import me.kmozze.expensetracker.model.domain.UserState
import me.kmozze.expensetracker.service.UserSessionService
import org.springframework.stereotype.Component

@Component
class DialogueRouter(
    private val userSessionService: UserSessionService,
    private val startCommandHandler: StartCommandHandler,
    private val unknownCommandHandler: UnknownCommandHandler,
    private val errorHandler: ErrorHandler,
    stateHandlers: List<StateHandler>,
) {
    private val handlersByState: Map<UserState, StateHandler> =
        stateHandlers.associateBy { it.supportedState }

    fun process(input: UserInput): HandlerResult {
        return try {
            if (input.text?.equals("/start", ignoreCase = true) == true) {
                userSessionService.clear(input.userId)
                return startCommandHandler.handle(input)
            }

            val currentState = userSessionService.getCurrentState(input.userId)

            val handler: UserInputHandler = handlersByState[currentState] ?: unknownCommandHandler

            val result = handler.handle(input)

            userSessionService.setNextState(input.userId, result.nextState)

            result
        } catch (e: Exception) {
            // ловим всё одним catch
            errorHandler.handle(input.userId, e)
        }
    }

    fun commitSuccessfulSend(userId: Long) {
        userSessionService.applyNextState(userId)
    }
}
