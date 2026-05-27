package me.kmozze.expensetracker.unit.handler.statehandler

import io.mockk.confirmVerified
import io.mockk.every
import io.mockk.junit5.MockKExtension
import io.mockk.mockk
import io.mockk.verify
import me.kmozze.expensetracker.exception.BusinessErrorCode
import me.kmozze.expensetracker.handler.statehandler.AwaitingExpenseDateSelectionHandler
import me.kmozze.expensetracker.model.domain.BotAction
import me.kmozze.expensetracker.model.domain.BotMessage
import me.kmozze.expensetracker.model.domain.ExpenseDateSelection
import me.kmozze.expensetracker.model.domain.ExpenseDraft
import me.kmozze.expensetracker.model.domain.Money
import me.kmozze.expensetracker.model.domain.ResponseDelivery
import me.kmozze.expensetracker.model.domain.UserCommand
import me.kmozze.expensetracker.model.domain.UserState
import me.kmozze.expensetracker.model.entity.Expense
import me.kmozze.expensetracker.service.ExpenseService
import me.kmozze.expensetracker.support.makeUserInput
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import java.math.BigDecimal
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.util.UUID

@ExtendWith(MockKExtension::class)
class AwaitingExpenseDateSelectionHandlerTest {
    private val expenseService: ExpenseService = mockk()
    private lateinit var handler: AwaitingExpenseDateSelectionHandler

    @BeforeEach
    fun setUp() {
        handler =
            AwaitingExpenseDateSelectionHandler(
                expenseService = expenseService,
                clock = CLOCK,
            )
    }

    @Test
    fun `today selection saves expense and returns idle`() {
        val completeDraft = EXPENSE_DRAFT_WITH_CATEGORY.copy(expenseDate = TODAY)
        every { expenseService.saveExpense(USER_ID, completeDraft) } returns expense(expenseDate = TODAY)

        val result = handle(UserCommand.SelectExpenseDate(ExpenseDateSelection.TODAY))

        assertThat(result.response.message)
            .isEqualTo(
                BotMessage.ExpenseSaved(
                    amount = EXPENSE_AMOUNT,
                    categoryName = CATEGORY_NAME,
                    expenseDate = TODAY,
                    description = EXPENSE_DESCRIPTION,
                ),
            )
        assertThat(result.response.actions).containsExactly(BotAction.ShowExpenseCardActions(EXPENSE_ID))
        assertThat(result.response.delivery).isEqualTo(ResponseDelivery.EditMessage(CARD_MESSAGE_ID))
        assertThat(result.nextState).isEqualTo(UserState.Idle)
        verify(exactly = 1) { expenseService.saveExpense(USER_ID, completeDraft) }
        confirmVerified(expenseService)
    }

    @Test
    fun `yesterday selection saves expense with previous day`() {
        val completeDraft = EXPENSE_DRAFT_WITH_CATEGORY.copy(expenseDate = YESTERDAY)
        every { expenseService.saveExpense(USER_ID, completeDraft) } returns expense(expenseDate = YESTERDAY)

        val result = handle(UserCommand.SelectExpenseDate(ExpenseDateSelection.YESTERDAY))

        assertThat(result.response.message)
            .isEqualTo(
                BotMessage.ExpenseSaved(
                    amount = EXPENSE_AMOUNT,
                    categoryName = CATEGORY_NAME,
                    expenseDate = YESTERDAY,
                    description = EXPENSE_DESCRIPTION,
                ),
            )
        assertThat(result.nextState).isEqualTo(UserState.Idle)
        assertThat(result.response.delivery).isEqualTo(ResponseDelivery.EditMessage(CARD_MESSAGE_ID))
        verify(exactly = 1) { expenseService.saveExpense(USER_ID, completeDraft) }
        confirmVerified(expenseService)
    }

    @Test
    fun `manual selection asks user to enter date`() {
        val result = handle(UserCommand.SelectExpenseDate(ExpenseDateSelection.MANUAL))

        assertThat(result.response.message)
            .isEqualTo(
                BotMessage.EnterExpenseDateManually(
                    amount = EXPENSE_AMOUNT,
                    categoryName = CATEGORY_NAME,
                    description = EXPENSE_DESCRIPTION,
                ),
            )
        assertThat(result.response.actions).containsExactly(BotAction.ShowCancel)
        assertThat(result.response.delivery).isEqualTo(ResponseDelivery.EditMessage(CARD_MESSAGE_ID))
        assertThat(result.nextState)
            .isEqualTo(
                UserState.AwaitingExpenseManualDateInput(
                    expenseDraft = EXPENSE_DRAFT_WITH_CATEGORY,
                    cardMessageId = CARD_MESSAGE_ID,
                ),
            )
        confirmVerified(expenseService)
    }

    @Test
    fun `invalid date callback keeps date selection open with error`() {
        val result = handle(UserCommand.InvalidExpenseDateSelection)

        assertThat(result.response.message).isEqualTo(BotMessage.Error(BusinessErrorCode.INVALID_EXPENSE_DATE_SELECTION))
        assertThat(result.response.actions).containsExactly(BotAction.ShowExpenseDateSelection)
        assertThat(result.response.delivery).isEqualTo(ResponseDelivery.EditMessage(CARD_MESSAGE_ID))
        assertThat(result.nextState).isEqualTo(AWAITING_EXPENSE_DATE_SELECTION)
        confirmVerified(expenseService)
    }

    @Test
    fun `cancel returns idle without saving expense`() {
        val result = handle(UserCommand.Cancel)

        assertThat(result.response.message).isEqualTo(BotMessage.ExpenseCanceled)
        assertThat(result.response.actions).containsExactly(BotAction.ClearInlineKeyboard)
        assertThat(result.response.delivery).isEqualTo(ResponseDelivery.EditMessage(CARD_MESSAGE_ID))
        assertThat(result.nextState).isEqualTo(UserState.Idle)
        confirmVerified(expenseService)
    }

    private fun handle(command: UserCommand) =
        handler.handle(
            input =
                makeUserInput(
                    userId = USER_ID,
                    chatId = CHAT_ID,
                    callbackData = "callback",
                    callbackMessageId = CARD_MESSAGE_ID,
                    command = command,
                ),
            currentState = AWAITING_EXPENSE_DATE_SELECTION,
        )

    private fun expense(expenseDate: LocalDate): Expense =
        Expense(
            id = EXPENSE_ID,
            categoryId = CATEGORY_ID,
            amount = EXPENSE_AMOUNT,
            userId = USER_ID,
            expenseDate = expenseDate,
            description = EXPENSE_DESCRIPTION,
        )

    private companion object {
        const val USER_ID = 123L
        const val CHAT_ID = 456L
        const val CARD_MESSAGE_ID = 789
        const val CATEGORY_NAME = "Транспорт"
        const val EXPENSE_DESCRIPTION = "такси"
        val CATEGORY_ID: UUID = UUID.fromString("00000000-0000-0000-0000-000000000001")
        val EXPENSE_ID: UUID = UUID.fromString("00000000-0000-0000-0000-000000000101")
        val EXPENSE_AMOUNT: Money = Money.of(BigDecimal("500.00"))
        val EXPENSE_DRAFT_WITH_CATEGORY: ExpenseDraft =
            ExpenseDraft(
                amount = EXPENSE_AMOUNT,
                description = EXPENSE_DESCRIPTION,
                categoryId = CATEGORY_ID,
                categoryName = CATEGORY_NAME,
            )
        val AWAITING_EXPENSE_DATE_SELECTION: UserState.AwaitingExpenseDateSelection =
            UserState.AwaitingExpenseDateSelection(
                expenseDraft = EXPENSE_DRAFT_WITH_CATEGORY,
                cardMessageId = CARD_MESSAGE_ID,
            )
        val CLOCK: Clock = Clock.fixed(Instant.parse("2026-05-24T12:00:00Z"), ZoneOffset.UTC)
        val TODAY: LocalDate = LocalDate.parse("2026-05-24")
        val YESTERDAY: LocalDate = LocalDate.parse("2026-05-23")
    }
}
