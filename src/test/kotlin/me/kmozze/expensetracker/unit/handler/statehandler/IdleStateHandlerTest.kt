package me.kmozze.expensetracker.unit.handler.statehandler

import io.mockk.every
import io.mockk.junit5.MockKExtension
import io.mockk.mockk
import io.mockk.verify
import me.kmozze.expensetracker.handler.statehandler.IdleStateHandler
import me.kmozze.expensetracker.model.domain.BotAction
import me.kmozze.expensetracker.model.domain.BotText
import me.kmozze.expensetracker.model.domain.Money
import me.kmozze.expensetracker.model.domain.ResponseDelivery
import me.kmozze.expensetracker.model.domain.UserCommand
import me.kmozze.expensetracker.model.domain.UserInput
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

        assertThat(
            result.response.outgoingMessages
                .single()
                .text,
        ).isEqualTo(BotText.AddExpenseInstructions)
        assertThat(
            result.response.outgoingMessages
                .single()
                .actions,
        ).isEmpty()
        assertThat(result.nextState).isEqualTo(UserState.AwaitingExpenseInput)
    }

    @ParameterizedTest(name = "command: {0}")
    @MethodSource("featureInProgressCommands")
    fun `menu commands return feature in progress`(command: UserCommand) {
        val result =
            handler.handle(
                input = makeInput(command),
                currentState = UserState.Idle,
            )

        assertThat(
            result.response.outgoingMessages
                .single()
                .text,
        ).isEqualTo(BotText.FeatureInProgress)
        assertThat(
            result.response.outgoingMessages
                .single()
                .actions,
        ).isEmpty()
        assertThat(result.nextState).isEqualTo(UserState.Idle)
    }

    @ParameterizedTest(name = "command: {0}")
    @MethodSource("unsupportedCommands")
    fun `unsupported commands keep idle state`(command: UserCommand) {
        val result =
            handler.handle(
                input = makeInput(command),
                currentState = UserState.Idle,
            )

        assertThat(
            result.response.outgoingMessages
                .single()
                .text,
        ).isEqualTo(BotText.UnknownCommand)
        assertThat(
            result.response.outgoingMessages
                .single()
                .actions,
        ).containsExactly(BotAction.ShowMainMenu)
        assertThat(result.nextState).isEqualTo(UserState.Idle)
    }

    @ParameterizedTest(name = "command: {0}")
    @MethodSource("staleCallbackCommands")
    fun `stale callback commands keep idle state with expired selection message`(command: UserCommand) {
        val result =
            handler.handle(
                input = makeInput(command),
                currentState = UserState.Idle,
            )

        assertThat(
            result.response.outgoingMessages
                .single()
                .text,
        ).isEqualTo(BotText.SelectionExpired)
        assertThat(
            result.response.outgoingMessages
                .single()
                .actions,
        ).containsExactly(BotAction.ShowMainMenu)
        assertThat(result.nextState).isEqualTo(UserState.Idle)
    }

    @Test
    fun `request expense deletion edits card into deletion confirmation`() {
        every { expenseService.findExpenseForUser(USER_ID, EXPENSE_ID) } returns EXPENSE
        every { categoryService.findCategoryForUser(CATEGORY_ID, USER_ID) } returns CATEGORY

        val result =
            handler.handle(
                input = makeInput(UserCommand.RequestExpenseDeletion(EXPENSE_ID), callbackMessageId = MESSAGE_ID),
                currentState = UserState.Idle,
            )

        assertThat(
            result.response.outgoingMessages
                .single()
                .text,
        ).isEqualTo(
            BotText.ExpenseDeletionConfirmation(
                amount = EXPENSE_AMOUNT,
                categoryName = CATEGORY.name,
                expenseDate = EXPENSE_DATE,
                description = EXPENSE_DESCRIPTION,
            ),
        )
        assertThat(
            result.response.outgoingMessages
                .single()
                .actions,
        ).containsExactly(BotAction.ShowExpenseDeletionConfirmation(EXPENSE_ID))
        assertThat(
            result.response.outgoingMessages
                .single()
                .delivery,
        ).isEqualTo(ResponseDelivery.EditMessage(MESSAGE_ID))
        assertThat(result.nextState).isEqualTo(UserState.Idle)
        verify(exactly = 1) { expenseService.findExpenseForUser(USER_ID, EXPENSE_ID) }
        verify(exactly = 1) { categoryService.findCategoryForUser(CATEGORY_ID, USER_ID) }
    }

    @Test
    fun `request expense edit opens field selection`() {
        every { expenseService.findExpenseForUser(USER_ID, EXPENSE_ID) } returns EXPENSE
        every { categoryService.findCategoryForUser(CATEGORY_ID, USER_ID) } returns CATEGORY

        val result =
            handler.handle(
                input = makeInput(UserCommand.RequestExpenseEdit(EXPENSE_ID), callbackMessageId = MESSAGE_ID),
                currentState = UserState.Idle,
            )

        assertThat(
            result.response.outgoingMessages[0]
                .text,
        ).isEqualTo(
            BotText.ExpenseEditable(
                amount = EXPENSE_AMOUNT,
                categoryName = CATEGORY.name,
                expenseDate = EXPENSE_DATE,
                description = EXPENSE_DESCRIPTION,
            ),
        )
        assertThat(
            result.response.outgoingMessages[0]
                .actions,
        ).containsExactly(BotAction.ClearInlineKeyboard)
        assertThat(
            result.response.outgoingMessages[0]
                .delivery,
        ).isEqualTo(ResponseDelivery.EditMessage(MESSAGE_ID))
        assertThat(
            result.response.outgoingMessages[1]
                .text,
        ).isEqualTo(BotText.EditExpenseFieldSelection)
        assertThat(
            result.response.outgoingMessages[1]
                .actions,
        ).containsExactly(BotAction.ShowExpenseEditFieldSelection)
        assertThat(result.nextState)
            .isEqualTo(UserState.AwaitingExpenseEditFieldSelection(expenseId = EXPENSE_ID))
        verify(exactly = 1) { expenseService.findExpenseForUser(USER_ID, EXPENSE_ID) }
        verify(exactly = 1) { categoryService.findCategoryForUser(CATEGORY_ID, USER_ID) }
    }

    @Test
    fun `request expense edit returns unavailable when expense is missing`() {
        every { expenseService.findExpenseForUser(USER_ID, EXPENSE_ID) } returns null

        val result =
            handler.handle(
                input = makeInput(UserCommand.RequestExpenseEdit(EXPENSE_ID), callbackMessageId = MESSAGE_ID),
                currentState = UserState.Idle,
            )

        assertThat(
            result.response.outgoingMessages
                .single()
                .text,
        ).isEqualTo(BotText.ExpenseUnavailable)
        assertThat(
            result.response.outgoingMessages
                .single()
                .actions,
        ).containsExactly(BotAction.ClearInlineKeyboard)
        assertThat(
            result.response.outgoingMessages
                .single()
                .delivery,
        ).isEqualTo(ResponseDelivery.EditMessage(MESSAGE_ID))
        assertThat(result.nextState).isEqualTo(UserState.Idle)
        verify(exactly = 1) { expenseService.findExpenseForUser(USER_ID, EXPENSE_ID) }
    }

    @Test
    fun `cancel expense deletion restores card actions`() {
        every { expenseService.findExpenseForUser(USER_ID, EXPENSE_ID) } returns EXPENSE
        every { categoryService.findCategoryForUser(CATEGORY_ID, USER_ID) } returns CATEGORY

        val result =
            handler.handle(
                input = makeInput(UserCommand.CancelExpenseDeletion(EXPENSE_ID), callbackMessageId = MESSAGE_ID),
                currentState = UserState.Idle,
            )

        assertThat(
            result.response.outgoingMessages
                .single()
                .text,
        ).isEqualTo(
            BotText.ExpenseSaved(
                amount = EXPENSE_AMOUNT,
                categoryName = CATEGORY.name,
                expenseDate = EXPENSE_DATE,
                description = EXPENSE_DESCRIPTION,
            ),
        )
        assertThat(
            result.response.outgoingMessages
                .single()
                .actions,
        ).containsExactly(BotAction.ShowExpenseCardActions(EXPENSE_ID))
        assertThat(
            result.response.outgoingMessages
                .single()
                .delivery,
        ).isEqualTo(ResponseDelivery.EditMessage(MESSAGE_ID))
        assertThat(result.nextState).isEqualTo(UserState.Idle)
        verify(exactly = 1) { expenseService.findExpenseForUser(USER_ID, EXPENSE_ID) }
        verify(exactly = 1) { categoryService.findCategoryForUser(CATEGORY_ID, USER_ID) }
    }

    @Test
    fun `confirm expense deletion deletes expense and clears inline keyboard`() {
        every { expenseService.deleteExpenseForUser(USER_ID, EXPENSE_ID) } returns true

        val result =
            handler.handle(
                input = makeInput(UserCommand.ConfirmExpenseDeletion(EXPENSE_ID), callbackMessageId = MESSAGE_ID),
                currentState = UserState.Idle,
            )

        assertThat(
            result.response.outgoingMessages
                .single()
                .text,
        ).isEqualTo(BotText.ExpenseDeleted)
        assertThat(
            result.response.outgoingMessages
                .single()
                .actions,
        ).containsExactly(BotAction.ClearInlineKeyboard)
        assertThat(
            result.response.outgoingMessages
                .single()
                .delivery,
        ).isEqualTo(ResponseDelivery.EditMessage(MESSAGE_ID))
        assertThat(result.nextState).isEqualTo(UserState.Idle)
        verify(exactly = 1) { expenseService.deleteExpenseForUser(USER_ID, EXPENSE_ID) }
    }

    @Test
    fun `request expense deletion returns unavailable when expense is missing`() {
        every { expenseService.findExpenseForUser(USER_ID, EXPENSE_ID) } returns null

        val result =
            handler.handle(
                input = makeInput(UserCommand.RequestExpenseDeletion(EXPENSE_ID), callbackMessageId = MESSAGE_ID),
                currentState = UserState.Idle,
            )

        assertThat(
            result.response.outgoingMessages
                .single()
                .text,
        ).isEqualTo(BotText.ExpenseUnavailable)
        assertThat(
            result.response.outgoingMessages
                .single()
                .actions,
        ).containsExactly(BotAction.ClearInlineKeyboard)
        assertThat(
            result.response.outgoingMessages
                .single()
                .delivery,
        ).isEqualTo(ResponseDelivery.EditMessage(MESSAGE_ID))
        assertThat(result.nextState).isEqualTo(UserState.Idle)
        verify(exactly = 1) { expenseService.findExpenseForUser(USER_ID, EXPENSE_ID) }
    }

    @Test
    fun `confirm expense deletion returns unavailable when expense was not deleted`() {
        every { expenseService.deleteExpenseForUser(USER_ID, EXPENSE_ID) } returns false

        val result =
            handler.handle(
                input = makeInput(UserCommand.ConfirmExpenseDeletion(EXPENSE_ID), callbackMessageId = MESSAGE_ID),
                currentState = UserState.Idle,
            )

        assertThat(
            result.response.outgoingMessages
                .single()
                .text,
        ).isEqualTo(BotText.ExpenseUnavailable)
        assertThat(
            result.response.outgoingMessages
                .single()
                .actions,
        ).containsExactly(BotAction.ClearInlineKeyboard)
        assertThat(
            result.response.outgoingMessages
                .single()
                .delivery,
        ).isEqualTo(ResponseDelivery.EditMessage(MESSAGE_ID))
        assertThat(result.nextState).isEqualTo(UserState.Idle)
        verify(exactly = 1) { expenseService.deleteExpenseForUser(USER_ID, EXPENSE_ID) }
    }

    private fun makeInput(
        command: UserCommand,
        callbackMessageId: Int? = null,
    ): UserInput =
        makeUserInput(
            userId = USER_ID,
            chatId = CHAT_ID,
            callbackMessageId = callbackMessageId,
            command = command,
        )

    private companion object {
        const val USER_ID = 123L
        const val CHAT_ID = 456L
        const val MESSAGE_ID = 789
        const val EXPENSE_DESCRIPTION = "такси"
        val EXPENSE_ID: UUID = UUID.fromString("00000000-0000-0000-0000-000000000001")
        val CATEGORY_ID: UUID = UUID.fromString("00000000-0000-0000-0000-000000000002")
        val EXPENSE_AMOUNT: Money = Money.of(BigDecimal("500.00"))
        val EXPENSE_DATE: LocalDate = LocalDate.parse("2026-05-24")
        val CATEGORY: Category =
            Category(
                id = CATEGORY_ID,
                name = "Транспорт",
                userId = USER_ID,
            )
        val EXPENSE: Expense =
            Expense(
                id = EXPENSE_ID,
                categoryId = CATEGORY_ID,
                amount = EXPENSE_AMOUNT,
                userId = USER_ID,
                expenseDate = EXPENSE_DATE,
                description = EXPENSE_DESCRIPTION,
            )

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
                UserCommand.InvalidExpenseAction,
            )
    }
}
