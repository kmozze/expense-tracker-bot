package me.kmozze.expensetracker.unit.handler

import io.mockk.mockk
import me.kmozze.expensetracker.adapter.callback.CallbackData
import me.kmozze.expensetracker.adapter.input.UserCommandParser
import me.kmozze.expensetracker.handler.DialogueRouter
import me.kmozze.expensetracker.handler.ErrorHandler
import me.kmozze.expensetracker.handler.StartCommandHandler
import me.kmozze.expensetracker.handler.UnknownCommandHandler
import me.kmozze.expensetracker.handler.statehandler.StateHandler
import me.kmozze.expensetracker.model.domain.BotMessage
import me.kmozze.expensetracker.model.domain.HandlerResponse
import me.kmozze.expensetracker.model.domain.HandlerResult
import me.kmozze.expensetracker.model.domain.ParsedExpense
import me.kmozze.expensetracker.model.domain.UserInput
import me.kmozze.expensetracker.model.domain.UserState
import me.kmozze.expensetracker.service.UserSessionService
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.util.UUID
import kotlin.reflect.KClass

class RoutingHandlerTest {
    private val userSessionService = UserSessionService()
    private val startCommandHandler: StartCommandHandler = mockk(relaxed = true)
    private val unknownCommandHandler = UnknownCommandHandler()
    private val errorHandler = ErrorHandler()

    @Test
    fun `routes awaiting category selection states with different payloads to the same handler`() {
        val userId = 42L
        val awaitingCategoryHandler = RecordingStateHandler(UserState.AwaitingCategorySelection::class)
        val idleHandler = RecordingStateHandler(UserState.Idle::class)
        val firstState =
            UserState.AwaitingCategorySelection(
                ParsedExpense(BigDecimal("500"), "такси"),
            )
        val secondState =
            UserState.AwaitingCategorySelection(
                ParsedExpense(BigDecimal("750"), "обед"),
            )

        val router =
            routerWith(
                idleHandler,
                awaitingCategoryHandler,
            )

        val firstCategoryId = UUID.fromString("00000000-0000-0000-0000-000000000001")
        val secondCategoryId = UUID.fromString("00000000-0000-0000-0000-000000000002")
        val firstInput =
            userInput(
                userId = userId,
                chatId = 1L,
                callbackData = CallbackData.selectCategory(firstCategoryId),
            )
        val secondInput =
            userInput(
                userId = userId,
                chatId = 1L,
                callbackData = CallbackData.selectCategory(secondCategoryId),
            )

        userSessionService.setState(userId, firstState)
        router.process(firstInput)
        userSessionService.setState(userId, secondState)
        router.process(secondInput)

        assertThat(awaitingCategoryHandler.calls)
            .extracting<UserInput> { it.input }
            .containsExactly(firstInput, secondInput)
        assertThat(awaitingCategoryHandler.calls)
            .extracting<UserState> { it.currentState }
            .containsExactly(firstState, secondState)
        assertThat(idleHandler.calls).isEmpty()
    }

    private fun routerWith(vararg stateHandlers: StateHandler): DialogueRouter =
        DialogueRouter(
            userSessionService = userSessionService,
            startCommandHandler = startCommandHandler,
            unknownCommandHandler = unknownCommandHandler,
            errorHandler = errorHandler,
            stateHandlers = stateHandlers.toList(),
        )

    private fun userInput(
        userId: Long,
        chatId: Long,
        text: String? = null,
        callbackData: String? = null,
    ): UserInput =
        UserInput(
            userId = userId,
            chatId = chatId,
            text = text,
            callbackData = callbackData,
            command = UserCommandParser.parse(text = text, callbackData = callbackData),
        )

    private class RecordingStateHandler(
        override val supportedStateClass: KClass<out UserState>,
    ) : StateHandler {
        val calls = mutableListOf<Call>()

        override fun handle(
            input: UserInput,
            currentState: UserState,
        ): HandlerResult {
            calls += Call(input, currentState)

            return HandlerResult(
                response =
                    HandlerResponse(
                        message = BotMessage.FeatureInProgress,
                        actions = emptyList(),
                    ),
            )
        }
    }

    private data class Call(
        val input: UserInput,
        val currentState: UserState,
    )
}
