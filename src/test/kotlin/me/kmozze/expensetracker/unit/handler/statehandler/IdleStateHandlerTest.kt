package me.kmozze.expensetracker.unit.handler.statehandler

import io.mockk.confirmVerified
import io.mockk.every
import io.mockk.junit5.MockKExtension
import io.mockk.mockk
import io.mockk.verify
import me.kmozze.expensetracker.handler.statehandler.IdleStateHandler
import me.kmozze.expensetracker.model.domain.BotAction
import me.kmozze.expensetracker.model.domain.BotMessage
import me.kmozze.expensetracker.model.domain.ExpenseDateSelection
import me.kmozze.expensetracker.model.domain.Money
import me.kmozze.expensetracker.model.domain.ResponseDelivery
import me.kmozze.expensetracker.model.domain.UserCommand
import me.kmozze.expensetracker.model.domain.UserState
import me.kmozze.expensetracker.model.entity.Category
import me.kmozze.expensetracker.model.entity.Expense
import me.kmozze.expensetracker.service.CategoryService
import me.kmozze.expensetracker.service.ExpenseService
import me.kmozze.expensetracker.support.makeUserInput
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import java.math.BigDecimal
import java.time.LocalDate
import java.util.UUID
import java.util.stream.Stream

@ExtendWith(MockKExtension::class)
class IdleStateHandlerTest {
    private val expenseService: ExpenseService = mockk()
    private val categoryService: CategoryService = mockk()
    private lateinit var handler: IdleStateHandler

    @BeforeEach
    fun setUp() {
        handler =
            IdleStateHandler(
                expenseService = expenseService,
                categoryService = categoryService,
            )
    }

    @Test
    fun `add expense command starts expense input`() {
        val result =
            handler.handle(
                input = makeInput(UserCommand.AddExpense),
                currentState = UserState.Idle,
            )

        assertThat(result.response.message).isEqualTo(BotMessage.AddExpenseInstructions)
        assertThat(result.response.actions).containsExactly(BotAction.ShowMainMenu)
        assertThat(result.nextState).isEqualTo(UserState.AwaitingExpenseInput)
        confirmVerified(expenseService, categoryService)
    }

    @ParameterizedTest(name = "command: {0}")
    @MethodSource("featureInProgressCommands")
    fun `menu commands return feature in progress`(command: UserCommand) {
        val result =
            handler.handle(
                input = makeInput(command),
                currentState = UserState.Idle,
            )

        assertThat(result.response.message).isEqualTo(BotMessage.FeatureInProgress)
        assertThat(result.response.actions).containsExactly(BotAction.ShowMainMenu)
        assertThat(result.nextState).isEqualTo(UserState.Idle)
        confirmVerified(expenseService, categoryService)
    }

    @ParameterizedTest(name = "command: {0}")
    @MethodSource("unsupportedCommands")
    fun `unsupported commands keep idle state`(command: UserCommand) {
        val result =
            handler.handle(
                input = makeInput(command),
                currentState = UserState.Idle,
            )

        assertThat(result.response.message).isEqualTo(BotMessage.UnknownCommand)
        assertThat(result.response.actions).containsExactly(BotAction.ShowMainMenu)
        assertThat(result.nextState).isEqualTo(UserState.Idle)
        confirmVerified(expenseService, categoryService)
    }

    @ParameterizedTest(name = "command: {0}")
    @MethodSource("staleCallbackCommands")
    fun `stale callback commands keep idle state with expired selection message`(command: UserCommand) {
        val result =
            handler.handle(
                input = makeInput(command),
                currentState = UserState.Idle,
            )

        assertThat(result.response.message).isEqualTo(BotMessage.SelectionExpired)
        assertThat(result.response.actions).containsExactly(BotAction.ShowMainMenu)
        assertThat(result.nextState).isEqualTo(UserState.Idle)
        confirmVerified(expenseService, categoryService)
    }

    @Test
    fun `expense deletion request opens confirmation for owned expense`() {
        val expense = expense()
        val category = category()
        every { expenseService.findExpenseForUser(USER_ID, EXPENSE_ID) } returns expense
        every { categoryService.findCategoryForUser(CATEGORY_ID, USER_ID) } returns category

        val result =
            handler.handle(
                input =
                    makeUserInput(
                        userId = USER_ID,
                        chatId = CHAT_ID,
                        callbackData = "callback",
                        callbackMessageId = CARD_MESSAGE_ID,
                        command = UserCommand.RequestExpenseDeletion(EXPENSE_ID),
                    ),
                currentState = UserState.Idle,
            )

        assertThat(result.response.message)
            .isEqualTo(
                BotMessage.ExpenseSaved(
                    amount = EXPENSE_AMOUNT,
                    categoryName = CATEGORY_NAME,
                    expenseDate = EXPENSE_DATE,
                    description = EXPENSE_DESCRIPTION,
                    showDeletionConfirmation = true,
                ),
            )
        assertThat(result.response.actions).containsExactly(BotAction.ShowExpenseDeletionConfirmation(EXPENSE_ID))
        assertThat(result.response.delivery).isEqualTo(ResponseDelivery.EditMessage(CARD_MESSAGE_ID))
        assertThat(result.nextState)
            .isEqualTo(
                UserState.AwaitingExpenseDeletionConfirmation(
                    expenseId = EXPENSE_ID,
                    cardMessageId = CARD_MESSAGE_ID,
                ),
            )
        verify(exactly = 1) { expenseService.findExpenseForUser(USER_ID, EXPENSE_ID) }
        verify(exactly = 1) { categoryService.findCategoryForUser(CATEGORY_ID, USER_ID) }
        confirmVerified(expenseService, categoryService)
    }

    @Test
    fun `missing expense deletion request returns unavailable message`() {
        every { expenseService.findExpenseForUser(USER_ID, EXPENSE_ID) } returns null

        val result =
            handler.handle(
                input =
                    makeUserInput(
                        userId = USER_ID,
                        chatId = CHAT_ID,
                        callbackData = "callback",
                        callbackMessageId = CARD_MESSAGE_ID,
                        command = UserCommand.RequestExpenseDeletion(EXPENSE_ID),
                    ),
                currentState = UserState.Idle,
            )

        assertThat(result.response.message).isEqualTo(BotMessage.ExpenseUnavailable)
        assertThat(result.response.actions).containsExactly(BotAction.ClearInlineKeyboard)
        assertThat(result.response.delivery).isEqualTo(ResponseDelivery.EditMessage(CARD_MESSAGE_ID))
        assertThat(result.nextState).isEqualTo(UserState.Idle)
        verify(exactly = 1) { expenseService.findExpenseForUser(USER_ID, EXPENSE_ID) }
        confirmVerified(expenseService, categoryService)
    }

    private fun makeInput(command: UserCommand) =
        makeUserInput(
            userId = USER_ID,
            chatId = CHAT_ID,
            command = command,
        )

    private companion object {
        const val USER_ID = 123L
        const val CHAT_ID = 456L
        const val CARD_MESSAGE_ID = 789
        const val CATEGORY_NAME = "Еда"
        const val EXPENSE_DESCRIPTION = "такси"
        val CATEGORY_ID: UUID = UUID.fromString("00000000-0000-0000-0000-000000000001")
        val EXPENSE_ID: UUID = UUID.fromString("00000000-0000-0000-0000-000000000101")
        val EXPENSE_AMOUNT: Money = Money.of(BigDecimal("500.00"))
        val EXPENSE_DATE: LocalDate = LocalDate.parse("2026-05-24")

        @JvmStatic
        fun featureInProgressCommands(): Stream<UserCommand> =
            Stream.of(
                UserCommand.ViewExpenses,
                UserCommand.Categories,
                UserCommand.Statistics,
            )

        @JvmStatic
        fun unsupportedCommands(): Stream<UserCommand> =
            Stream.of(
                UserCommand.Unsupported,
                UserCommand.PlainText("500 такси"),
            )

        @JvmStatic
        fun staleCallbackCommands(): Stream<UserCommand> =
            Stream.of(
                UserCommand.Cancel,
                UserCommand.SelectCategory(UUID.fromString("00000000-0000-0000-0000-000000000001")),
                UserCommand.SelectExpenseDate(ExpenseDateSelection.TODAY),
                UserCommand.ConfirmExpenseDeletion(UUID.fromString("00000000-0000-0000-0000-000000000101")),
                UserCommand.CancelExpenseDeletion(UUID.fromString("00000000-0000-0000-0000-000000000101")),
                UserCommand.InvalidCategorySelection,
                UserCommand.InvalidExpenseDateSelection,
                UserCommand.InvalidExpenseDeletion,
            )
    }

    private fun expense(): Expense =
        Expense(
            id = EXPENSE_ID,
            categoryId = CATEGORY_ID,
            amount = EXPENSE_AMOUNT,
            userId = USER_ID,
            expenseDate = EXPENSE_DATE,
            description = EXPENSE_DESCRIPTION,
        )

    private fun category(): Category =
        Category(
            id = CATEGORY_ID,
            name = CATEGORY_NAME,
            userId = USER_ID,
        )
}
