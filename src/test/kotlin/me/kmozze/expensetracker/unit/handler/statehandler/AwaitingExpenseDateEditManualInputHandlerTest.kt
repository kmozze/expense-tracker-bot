package me.kmozze.expensetracker.unit.handler.statehandler

import io.mockk.confirmVerified
import io.mockk.every
import io.mockk.junit5.MockKExtension
import io.mockk.mockk
import io.mockk.verify
import me.kmozze.expensetracker.exception.BusinessErrorCode
import me.kmozze.expensetracker.exception.exception
import me.kmozze.expensetracker.handler.statehandler.AwaitingExpenseDateEditManualInputHandler
import me.kmozze.expensetracker.model.domain.BotAction
import me.kmozze.expensetracker.model.domain.BotText
import me.kmozze.expensetracker.model.domain.HandlerResult
import me.kmozze.expensetracker.model.domain.Money
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
import java.math.BigDecimal
import java.time.LocalDate
import java.util.UUID

@ExtendWith(MockKExtension::class)
class AwaitingExpenseDateEditManualInputHandlerTest {
    private val expenseService: ExpenseService = mockk()
    private val categoryService: CategoryService = mockk()
    private lateinit var handler: AwaitingExpenseDateEditManualInputHandler

    @BeforeEach
    fun setUp() {
        handler =
            AwaitingExpenseDateEditManualInputHandler(
                expenseService = expenseService,
                categoryService = categoryService,
            )
    }

    @Test
    fun `manual date updates expense and returns idle`() {
        every { expenseService.parseExpenseDate(MANUAL_DATE_TEXT) } returns MANUAL_DATE
        every { expenseService.updateExpenseDateForUser(USER_ID, EXPENSE_ID, MANUAL_DATE) } returns
            EXPENSE.copy(expenseDate = MANUAL_DATE)
        every { expenseService.findExpenseForUser(USER_ID, EXPENSE_ID) } returns
            EXPENSE.copy(expenseDate = MANUAL_DATE)
        every { categoryService.findCategoryForUser(CATEGORY_ID, USER_ID) } returns CATEGORY

        val result = handle(UserCommand.PlainText(MANUAL_DATE_TEXT))

        assertThat(result.response.outgoingMessages).hasSize(2)
        assertThat(result.response.outgoingMessages[0].text).isEqualTo(BotText.ExpenseSaved)
        assertThat(result.response.outgoingMessages[0].actions).containsExactly(BotAction.RemoveReplyKeyboard)
        assertThat(result.response.outgoingMessages[1].text)
            .isEqualTo(
                BotText.ExpenseView(
                    amount = EXPENSE_AMOUNT,
                    categoryName = CATEGORY.name,
                    expenseDate = MANUAL_DATE,
                    description = EXPENSE_DESCRIPTION,
                ),
            )
        assertThat(result.response.outgoingMessages[1].actions)
            .containsExactly(BotAction.ShowExpenseCardActions(EXPENSE_ID))
        assertThat(result.nextState).isEqualTo(UserState.Idle)
        verify(exactly = 1) { expenseService.parseExpenseDate(MANUAL_DATE_TEXT) }
        verify(exactly = 1) { expenseService.updateExpenseDateForUser(USER_ID, EXPENSE_ID, MANUAL_DATE) }
        verify(exactly = 1) { expenseService.findExpenseForUser(USER_ID, EXPENSE_ID) }
        verify(exactly = 1) { categoryService.findCategoryForUser(CATEGORY_ID, USER_ID) }
        confirmVerified(expenseService, categoryService)
    }

    @Test
    fun `manual date keeps input open with error when invalid`() {
        every { expenseService.parseExpenseDate("31.02.2026") } throws BusinessErrorCode.EXPENSE_DATE_INVALID_FORMAT.exception()

        val result = handle(UserCommand.PlainText("31.02.2026"))

        assertThat(
            result.response.outgoingMessages
                .single()
                .text,
        ).isEqualTo(BotText.Error(BusinessErrorCode.EXPENSE_DATE_INVALID_FORMAT))
        assertThat(
            result.response.outgoingMessages
                .single()
                .actions,
        ).containsExactly(BotAction.ShowCancel)
        assertThat(result.nextState).isEqualTo(AWAITING_EXPENSE_DATE_EDIT_MANUAL_INPUT)
        verify(exactly = 1) { expenseService.parseExpenseDate("31.02.2026") }
        confirmVerified(expenseService, categoryService)
    }

    @Test
    fun `non-text command repeats manual date input`() {
        every { expenseService.findExpenseForUser(USER_ID, EXPENSE_ID) } returns EXPENSE
        every { categoryService.findCategoryForUser(CATEGORY_ID, USER_ID) } returns CATEGORY

        val result = handle(UserCommand.RequestExpenseDeletion(EXPENSE_ID))

        assertThat(result.response.outgoingMessages).hasSize(2)
        assertThat(result.response.outgoingMessages[0].text)
            .isEqualTo(
                BotText.ExpenseView(
                    amount = EXPENSE_AMOUNT,
                    categoryName = CATEGORY.name,
                    expenseDate = EXPENSE_DATE,
                    description = EXPENSE_DESCRIPTION,
                ),
            )
        assertThat(result.response.outgoingMessages[0].actions).isEmpty()
        assertThat(result.response.outgoingMessages[1].text).isEqualTo(BotText.EnterExpenseDateManually)
        assertThat(result.response.outgoingMessages[1].actions).containsExactly(BotAction.ShowCancel)
        assertThat(result.nextState).isEqualTo(AWAITING_EXPENSE_DATE_EDIT_MANUAL_INPUT)
        verify(exactly = 1) { expenseService.findExpenseForUser(USER_ID, EXPENSE_ID) }
        verify(exactly = 1) { categoryService.findCategoryForUser(CATEGORY_ID, USER_ID) }
        confirmVerified(expenseService, categoryService)
    }

    @Test
    fun `cancel returns card with actions`() {
        every { expenseService.findExpenseForUser(USER_ID, EXPENSE_ID) } returns EXPENSE
        every { categoryService.findCategoryForUser(CATEGORY_ID, USER_ID) } returns CATEGORY

        val result = handle(UserCommand.Cancel)

        assertThat(result.response.outgoingMessages).hasSize(2)
        assertThat(result.response.outgoingMessages[0].text).isEqualTo(BotText.Done)
        assertThat(result.response.outgoingMessages[0].actions).containsExactly(BotAction.RemoveReplyKeyboard)
        assertThat(result.response.outgoingMessages[1].text)
            .isEqualTo(
                BotText.ExpenseView(
                    amount = EXPENSE_AMOUNT,
                    categoryName = CATEGORY.name,
                    expenseDate = EXPENSE_DATE,
                    description = EXPENSE_DESCRIPTION,
                ),
            )
        assertThat(result.response.outgoingMessages[1].actions)
            .containsExactly(BotAction.ShowExpenseCardActions(EXPENSE_ID))
        assertThat(result.nextState).isEqualTo(UserState.Idle)
        verify(exactly = 1) { expenseService.findExpenseForUser(USER_ID, EXPENSE_ID) }
        verify(exactly = 1) { categoryService.findCategoryForUser(CATEGORY_ID, USER_ID) }
        confirmVerified(expenseService, categoryService)
    }

    private fun handle(command: UserCommand): HandlerResult =
        handler.handle(
            input =
                makeUserInput(
                    userId = USER_ID,
                    chatId = CHAT_ID,
                    text = textFor(command),
                    command = command,
                ),
            currentState = AWAITING_EXPENSE_DATE_EDIT_MANUAL_INPUT,
        )

    private fun textFor(command: UserCommand): String? =
        when (command) {
            is UserCommand.PlainText -> command.value
            else -> null
        }

    private companion object {
        const val USER_ID = 123L
        const val CHAT_ID = 456L
        val CATEGORY_ID: UUID = UUID.fromString("00000000-0000-0000-0000-000000000002")
        val EXPENSE_ID: UUID = UUID.fromString("00000000-0000-0000-0000-000000000001")
        val EXPENSE_AMOUNT: Money = Money.of(BigDecimal("500.00"))
        const val EXPENSE_DESCRIPTION = "такси"
        const val MANUAL_DATE_TEXT = "20.05.2026"
        val MANUAL_DATE: LocalDate = LocalDate.parse("2026-05-20")
        val EXPENSE_DATE: LocalDate = LocalDate.parse("2026-05-24")
        val CATEGORY =
            Category(
                id = CATEGORY_ID,
                name = "Транспорт",
                userId = USER_ID,
            )
        val EXPENSE =
            Expense(
                id = EXPENSE_ID,
                categoryId = CATEGORY_ID,
                amount = EXPENSE_AMOUNT,
                userId = USER_ID,
                expenseDate = EXPENSE_DATE,
                description = EXPENSE_DESCRIPTION,
            )
        val AWAITING_EXPENSE_DATE_EDIT_MANUAL_INPUT =
            UserState.AwaitingExpenseDateEditManualInput(
                expenseId = EXPENSE_ID,
            )
    }
}
