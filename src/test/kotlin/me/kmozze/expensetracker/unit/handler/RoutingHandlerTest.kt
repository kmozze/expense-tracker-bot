package me.kmozze.expensetracker.unit.handler

import io.mockk.mockk
import me.kmozze.expensetracker.adapter.callback.CallbackData
import me.kmozze.expensetracker.handler.DialogueRouter
import me.kmozze.expensetracker.handler.ErrorHandler
import me.kmozze.expensetracker.handler.MenuCommandHandler
import me.kmozze.expensetracker.handler.StartCommandHandler
import me.kmozze.expensetracker.handler.UnknownCommandHandler
import me.kmozze.expensetracker.handler.statehandler.StateHandler
import me.kmozze.expensetracker.model.domain.BotAction
import me.kmozze.expensetracker.model.domain.BotText
import me.kmozze.expensetracker.model.domain.ExpenseDraft
import me.kmozze.expensetracker.model.domain.HandlerResponse
import me.kmozze.expensetracker.model.domain.HandlerResult
import me.kmozze.expensetracker.model.domain.Money
import me.kmozze.expensetracker.model.domain.OutgoingMessage
import me.kmozze.expensetracker.model.domain.UserInput
import me.kmozze.expensetracker.model.domain.UserState
import me.kmozze.expensetracker.service.UserSessionService
import me.kmozze.expensetracker.support.makeUserInput
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.util.UUID
import kotlin.reflect.KClass

class RoutingHandlerTest {
    private val userSessionService = UserSessionService()
    private val startCommandHandler: StartCommandHandler = mockk()
    private val menuCommandHandler = MenuCommandHandler()
    private val unknownCommandHandler = UnknownCommandHandler()
    private val errorHandler = ErrorHandler()

    @Test
    fun `routes awaiting category selection states with different payloads to the same handler`() {
        val userId = 42L
        val awaitingCategoryHandler = RecordingStateHandler(UserState.AwaitingCategorySelection::class)
        val idleHandler = RecordingStateHandler(UserState.Idle::class)
        val firstState =
            UserState.AwaitingCategorySelection(
                ExpenseDraft(Money.of(BigDecimal("500")), "такси"),
            )
        val secondState =
            UserState.AwaitingCategorySelection(
                ExpenseDraft(Money.of(BigDecimal("750")), "обед"),
            )

        val router =
            routerWith(
                idleHandler,
                awaitingCategoryHandler,
            )

        val firstInput =
            makeUserInput(
                userId = userId,
                chatId = 1L,
                text = "Еда",
            )
        val secondInput =
            makeUserInput(
                userId = userId,
                chatId = 1L,
                text = "Транспорт",
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

    @Test
    fun `menu command from active dialog clears current state and returns main menu`() {
        val userId = 43L
        val awaitingCategoryState =
            UserState.AwaitingCategorySelection(
                ExpenseDraft(Money.of(BigDecimal("500")), "такси"),
            )
        val router = routerWith(RecordingStateHandler(UserState.AwaitingCategorySelection::class))

        userSessionService.setState(userId, awaitingCategoryState)
        val result =
            router.process(
                makeUserInput(
                    userId = userId,
                    chatId = 1L,
                    text = "/menu",
                ),
            )

        assertThat(result.response.outgoingMessages)
            .containsExactly(
                OutgoingMessage(
                    text = BotText.MainMenuInfo,
                    actions = listOf(BotAction.RemoveReplyKeyboard),
                ),
                OutgoingMessage(
                    text = BotText.MainMenuActions,
                    actions = listOf(BotAction.ShowMainMenu),
                ),
            )
        assertThat(result.nextState).isEqualTo(UserState.Idle)
        assertThat(userSessionService.getState(userId)).isEqualTo(UserState.Idle)
    }

    @Test
    fun `menu command from idle returns menu info and actions`() {
        val userId = 47L
        val router = routerWith(RecordingStateHandler(UserState.Idle::class))

        val result =
            router.process(
                makeUserInput(
                    userId = userId,
                    chatId = 1L,
                    text = "/menu",
                ),
            )

        assertThat(result.response.outgoingMessages)
            .containsExactly(
                OutgoingMessage(
                    text = BotText.MainMenuInfo,
                    actions = emptyList(),
                ),
                OutgoingMessage(
                    text = BotText.MainMenuActions,
                    actions = listOf(BotAction.ShowMainMenu),
                ),
            )
        assertThat(result.nextState).isEqualTo(UserState.Idle)
        assertThat(userSessionService.getState(userId)).isEqualTo(UserState.Idle)
    }

    @Test
    fun `menu callback during active dialog returns callback answer without routing`() {
        val userId = 44L
        val awaitingCategoryHandler = RecordingStateHandler(UserState.AwaitingCategorySelection::class)
        val awaitingCategoryState =
            UserState.AwaitingCategorySelection(
                ExpenseDraft(Money.of(BigDecimal("500")), "такси"),
            )
        val router = routerWith(awaitingCategoryHandler)

        userSessionService.setState(userId, awaitingCategoryState)
        val result =
            router.process(
                makeUserInput(
                    userId = userId,
                    chatId = 1L,
                    callbackData = CallbackData.menuViewExpenses(),
                ),
            )

        assertThat(result.response.outgoingMessages).isEmpty()
        assertThat(result.response.callbackAnswer?.text).isEqualTo(BotText.FinishCurrentDialog)
        assertThat(result.response.callbackAnswer?.showAlert).isTrue()
        assertThat(userSessionService.getState(userId)).isEqualTo(awaitingCategoryState)
        assertThat(awaitingCategoryHandler.calls).isEmpty()
    }

    @Test
    fun `card action callback during active dialog returns callback answer without routing`() {
        val userId = 45L
        val awaitingInputHandler = RecordingStateHandler(UserState.AwaitingExpenseInput::class)
        val router = routerWith(awaitingInputHandler)

        userSessionService.setState(userId, UserState.AwaitingExpenseInput)
        val result =
            router.process(
                makeUserInput(
                    userId = userId,
                    chatId = 1L,
                    callbackData = CallbackData.deleteExpense(UUID.randomUUID()),
                ),
            )

        assertThat(result.response.outgoingMessages).isEmpty()
        assertThat(result.response.callbackAnswer?.text).isEqualTo(BotText.FinishCurrentDialog)
        assertThat(result.response.callbackAnswer?.showAlert).isTrue()
        assertThat(userSessionService.getState(userId)).isEqualTo(UserState.AwaitingExpenseInput)
        assertThat(awaitingInputHandler.calls).isEmpty()
    }

    @Test
    fun `menu callback during manual date input returns callback answer without routing`() {
        val userId = 46L
        val awaitingManualDateHandler = RecordingStateHandler(UserState.AwaitingExpenseManualDateInput::class)
        val currentState =
            UserState.AwaitingExpenseManualDateInput(
                expenseDraft = ExpenseDraft(Money.of(BigDecimal("500")), "такси"),
                categoryName = "Транспорт",
            )
        val router = routerWith(awaitingManualDateHandler)

        userSessionService.setState(userId, currentState)
        val result =
            router.process(
                makeUserInput(
                    userId = userId,
                    chatId = 1L,
                    callbackData = CallbackData.menuStatistics(),
                ),
            )

        assertThat(result.response.outgoingMessages).isEmpty()
        assertThat(result.response.callbackAnswer?.text).isEqualTo(BotText.FinishCurrentDialog)
        assertThat(result.response.callbackAnswer?.showAlert).isTrue()
        assertThat(userSessionService.getState(userId)).isEqualTo(currentState)
        assertThat(awaitingManualDateHandler.calls).isEmpty()
    }

    private fun routerWith(vararg stateHandlers: StateHandler): DialogueRouter =
        DialogueRouter(
            userSessionService = userSessionService,
            startCommandHandler = startCommandHandler,
            menuCommandHandler = menuCommandHandler,
            unknownCommandHandler = unknownCommandHandler,
            errorHandler = errorHandler,
            stateHandlers = stateHandlers.toList(),
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
                        outgoingMessages =
                            listOf(
                                OutgoingMessage(
                                    text = BotText.FeatureInProgress,
                                    actions = emptyList(),
                                ),
                            ),
                    ),
            )
        }
    }

    private data class Call(
        val input: UserInput,
        val currentState: UserState,
    )
}
