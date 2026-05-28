package me.kmozze.expensetracker.handler

import me.kmozze.expensetracker.handler.statehandler.StateHandler
import me.kmozze.expensetracker.model.domain.BotText
import me.kmozze.expensetracker.model.domain.CallbackAnswer
import me.kmozze.expensetracker.model.domain.HandlerResponse
import me.kmozze.expensetracker.model.domain.HandlerResult
import me.kmozze.expensetracker.model.domain.UserCommand
import me.kmozze.expensetracker.model.domain.UserInput
import me.kmozze.expensetracker.model.domain.UserState
import me.kmozze.expensetracker.service.UserSessionService
import org.springframework.stereotype.Component
import kotlin.reflect.KClass

@Component
class DialogueRouter(
    private val userSessionService: UserSessionService,
    private val startCommandHandler: StartCommandHandler,
    private val menuCommandHandler: MenuCommandHandler,
    private val unknownCommandHandler: UnknownCommandHandler,
    private val errorHandler: ErrorHandler,
    stateHandlers: List<StateHandler>,
) {
    private val handlersByState: Map<KClass<out UserState>, StateHandler> =
        stateHandlers.associateBy { it.supportedStateClass }

    fun process(input: UserInput): HandlerResult {
        return try {
            if (input.command == UserCommand.Start) {
                userSessionService.clear(input.userId)
                return startCommandHandler.handle(input)
            }

            if (input.command == UserCommand.Menu) {
                val currentState = userSessionService.getState(input.userId)
                userSessionService.clear(input.userId)
                return menuCommandHandler.handle(removeReplyKeyboard = currentState !is UserState.Idle)
            }

            val currentState = userSessionService.getState(input.userId)

            if (shouldBlockCallback(input, currentState)) {
                return callbackBlockedResult()
            }

            val handler = handlersByState[currentState::class]

            val result = handler?.handle(input, currentState) ?: unknownCommandHandler.handle(input)

            if (result.nextState != null) {
                userSessionService.setState(input.userId, result.nextState)
            }

            result
        } catch (e: Exception) {
            errorHandler.handle(input.userId, e)
        }
    }

    private fun shouldBlockCallback(
        input: UserInput,
        currentState: UserState,
    ): Boolean =
        input.callbackData != null &&
            currentState !is UserState.Idle

    private fun callbackBlockedResult(): HandlerResult =
        HandlerResult(
            response =
                HandlerResponse(
                    outgoingMessages = emptyList(),
                    callbackAnswer = CallbackAnswer(text = BotText.FinishCurrentDialog, showAlert = true),
                ),
        )
}
