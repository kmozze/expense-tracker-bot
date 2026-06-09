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
import me.kmozze.expensetracker.model.domain.BotText
import me.kmozze.expensetracker.model.domain.ExpenseDraft
import me.kmozze.expensetracker.model.domain.Money
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
import java.time.LocalDate
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
            )
    }

    @Test
    fun `valid manual date saves expense and returns idle`() {
        val completeDraft = EXPENSE_DRAFT_WITH_CATEGORY.copy(expenseDate = MANUAL_DATE)
        every { expenseService.parseExpenseDate(MANUAL_DATE_TEXT) } returns MANUAL_DATE
        every { expenseService.saveExpense(USER_ID, completeDraft) } returns expense(expenseDate = MANUAL_DATE)

        val result = handle(UserCommand.PlainText(MANUAL_DATE_TEXT))

        assertThat(result.outgoingMessages).hasSize(2)
        assertThat(result.outgoingMessages[0].text).isEqualTo(BotText.ExpenseSaved)
        assertThat(result.outgoingMessages[0].actions).containsExactly(BotAction.RemoveReplyKeyboard)
        assertThat(result.outgoingMessages[1].text)
            .isEqualTo(
                BotText.ExpenseView(
                    amount = EXPENSE_AMOUNT,
                    categoryName = CATEGORY_NAME,
                    expenseDate = MANUAL_DATE,
                    description = EXPENSE_DESCRIPTION,
                ),
            )
        assertThat(result.outgoingMessages[1].actions).containsExactly(BotAction.ShowExpenseCardActions(EXPENSE_ID))
        assertThat(result.nextState).isEqualTo(UserState.Idle)
        verify(exactly = 1) { expenseService.parseExpenseDate(MANUAL_DATE_TEXT) }
        verify(exactly = 1) { expenseService.saveExpense(USER_ID, completeDraft) }
        confirmVerified(expenseService)
    }

    @Test
    fun `invalid manual date keeps manual input open with error`() {
        every { expenseService.parseExpenseDate("дата") } throws BusinessErrorCode.EXPENSE_DATE_INVALID_FORMAT.exception()

        val result = handle(UserCommand.PlainText("дата"))

        assertThat(
            result.outgoingMessages
                .single()
                .text,
        ).isEqualTo(BotText.Error(BusinessErrorCode.EXPENSE_DATE_INVALID_FORMAT))
        assertThat(
            result.outgoingMessages
                .single()
                .actions,
        ).containsExactly(BotAction.ShowCancel)
        assertThat(result.nextState).isEqualTo(AWAITING_EXPENSE_MANUAL_DATE_INPUT)
        verify(exactly = 1) { expenseService.parseExpenseDate("дата") }
        confirmVerified(expenseService)
    }

    @Test
    fun `non-text command repeats manual input`() {
        val result = handle(UserCommand.RequestExpenseEdit(EXPENSE_ID))

        assertThat(result.outgoingMessages).hasSize(2)
        assertThat(result.outgoingMessages[0].text)
            .isEqualTo(
                BotText.ExpenseView(
                    amount = EXPENSE_AMOUNT,
                    categoryName = CATEGORY_NAME,
                    expenseDate = null,
                    description = EXPENSE_DESCRIPTION,
                ),
            )
        assertThat(result.outgoingMessages[0].actions).isEmpty()
        assertThat(result.outgoingMessages[1].text).isEqualTo(BotText.EnterExpenseDateManually)
        assertThat(result.outgoingMessages[1].actions).containsExactly(BotAction.ShowCancel)
        assertThat(result.nextState).isEqualTo(AWAITING_EXPENSE_MANUAL_DATE_INPUT)
        confirmVerified(expenseService)
    }

    @Test
    fun `cancel returns idle without saving expense`() {
        val result = handle(UserCommand.Cancel)

        assertThat(
            result.outgoingMessages
                .single()
                .text,
        ).isEqualTo(BotText.ExpenseCanceled)
        assertThat(
            result.outgoingMessages
                .single()
                .actions,
        ).containsExactly(BotAction.RemoveReplyKeyboard)
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
                    command = command,
                ),
            currentState = AWAITING_EXPENSE_MANUAL_DATE_INPUT,
        )

    private fun textFor(command: UserCommand): String? =
        when (command) {
            is UserCommand.PlainText -> command.value
            else -> null
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
        const val CATEGORY_NAME = "Транспорт"
        const val EXPENSE_DESCRIPTION = "такси"
        const val MANUAL_DATE_TEXT = "20.05.2026"
        val CATEGORY_ID: UUID = UUID.fromString("00000000-0000-0000-0000-000000000001")
        val EXPENSE_ID: UUID = UUID.fromString("00000000-0000-0000-0000-000000000010")
        val EXPENSE_AMOUNT: Money = Money.of(BigDecimal("500.00"))
        val EXPENSE_DRAFT_WITH_CATEGORY: ExpenseDraft =
            ExpenseDraft(
                amount = EXPENSE_AMOUNT,
                description = EXPENSE_DESCRIPTION,
                categoryId = CATEGORY_ID,
            )
        val AWAITING_EXPENSE_MANUAL_DATE_INPUT: UserState.AwaitingExpenseManualDateInput =
            UserState.AwaitingExpenseManualDateInput(
                expenseDraft = EXPENSE_DRAFT_WITH_CATEGORY,
                categoryName = CATEGORY_NAME,
            )
        val MANUAL_DATE: LocalDate = LocalDate.parse("2026-05-20")
    }
}
