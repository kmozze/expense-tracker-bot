package me.kmozze.expensetracker.unit.handler.statehandler

import io.mockk.confirmVerified
import io.mockk.every
import io.mockk.junit5.MockKExtension
import io.mockk.mockk
import io.mockk.verify
import me.kmozze.expensetracker.exception.BusinessErrorCode
import me.kmozze.expensetracker.exception.exception
import me.kmozze.expensetracker.handler.statehandler.AwaitingExpenseManualDateInputHandler
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
class AwaitingExpenseManualDateInputHandlerTest {
    private val expenseService: ExpenseService = mockk()
    private lateinit var handler: AwaitingExpenseManualDateInputHandler

    @BeforeEach
    fun setUp() {
        handler =
            AwaitingExpenseManualDateInputHandler(
                expenseService = expenseService,
                clock = CLOCK,
            )
    }

    @Test
    fun `valid manual date saves expense and returns idle`() {
        val completeDraft = EXPENSE_DRAFT_WITH_CATEGORY.copy(expenseDate = MANUAL_DATE)
        every { expenseService.parseExpenseDate(MANUAL_DATE_TEXT) } returns MANUAL_DATE
        every { expenseService.saveExpense(USER_ID, completeDraft) } returns expense(expenseDate = MANUAL_DATE)

        val result = handle(UserCommand.PlainText(MANUAL_DATE_TEXT))

        assertThat(result.response.message)
            .isEqualTo(
                BotMessage.ExpenseSaved(
                    amount = EXPENSE_AMOUNT,
                    categoryName = CATEGORY_NAME,
                    expenseDate = MANUAL_DATE,
                    description = EXPENSE_DESCRIPTION,
                ),
            )
        assertThat(result.response.actions).containsExactly(BotAction.ShowExpenseCardActions(EXPENSE_ID))
        assertThat(result.response.delivery).isEqualTo(ResponseDelivery.EditMessage(CARD_MESSAGE_ID))
        assertThat(result.nextState).isEqualTo(UserState.Idle)
        verify(exactly = 1) { expenseService.parseExpenseDate(MANUAL_DATE_TEXT) }
        verify(exactly = 1) { expenseService.saveExpense(USER_ID, completeDraft) }
        confirmVerified(expenseService)
    }

    @Test
    fun `invalid manual date keeps manual input open with error`() {
        every { expenseService.parseExpenseDate("дата") } throws BusinessErrorCode.EXPENSE_DATE_INVALID_FORMAT.exception()

        val result = handle(UserCommand.PlainText("дата"))

        assertThat(result.response.message).isEqualTo(BotMessage.Error(BusinessErrorCode.EXPENSE_DATE_INVALID_FORMAT))
        assertThat(result.response.actions).containsExactly(BotAction.ShowCancel)
        assertThat(result.response.delivery).isEqualTo(ResponseDelivery.EditMessage(CARD_MESSAGE_ID))
        assertThat(result.nextState).isEqualTo(AWAITING_EXPENSE_MANUAL_DATE_INPUT)
        verify(exactly = 1) { expenseService.parseExpenseDate("дата") }
        confirmVerified(expenseService)
    }

    @Test
    fun `today callback from previous date keyboard saves expense`() {
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
    fun `invalid date callback keeps manual input open with error`() {
        val result = handle(UserCommand.InvalidExpenseDateSelection)

        assertThat(result.response.message).isEqualTo(BotMessage.Error(BusinessErrorCode.INVALID_EXPENSE_DATE_SELECTION))
        assertThat(result.response.actions).containsExactly(BotAction.ShowCancel)
        assertThat(result.response.delivery).isEqualTo(ResponseDelivery.EditMessage(CARD_MESSAGE_ID))
        assertThat(result.nextState).isEqualTo(AWAITING_EXPENSE_MANUAL_DATE_INPUT)
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
                    text = textFor(command),
                    callbackData = callbackDataFor(command),
                    callbackMessageId = callbackMessageIdFor(command),
                    command = command,
                ),
            currentState = AWAITING_EXPENSE_MANUAL_DATE_INPUT,
        )

    private fun textFor(command: UserCommand): String? =
        when (command) {
            is UserCommand.PlainText -> command.value
            else -> null
        }

    private fun callbackDataFor(command: UserCommand): String? =
        when (command) {
            is UserCommand.PlainText -> null
            else -> "callback"
        }

    private fun callbackMessageIdFor(command: UserCommand): Int? =
        when (command) {
            is UserCommand.PlainText -> null
            else -> CARD_MESSAGE_ID
        }

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
        const val MANUAL_DATE_TEXT = "20.05.2026"
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
        val AWAITING_EXPENSE_MANUAL_DATE_INPUT: UserState.AwaitingExpenseManualDateInput =
            UserState.AwaitingExpenseManualDateInput(
                expenseDraft = EXPENSE_DRAFT_WITH_CATEGORY,
                cardMessageId = CARD_MESSAGE_ID,
            )
        val CLOCK: Clock = Clock.fixed(Instant.parse("2026-05-24T12:00:00Z"), ZoneOffset.UTC)
        val TODAY: LocalDate = LocalDate.parse("2026-05-24")
        val MANUAL_DATE: LocalDate = LocalDate.parse("2026-05-20")
    }
}
