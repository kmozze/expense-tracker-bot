package me.kmozze.expensetracker.unit.handler.statehandler

import io.mockk.confirmVerified
import io.mockk.every
import io.mockk.junit5.MockKExtension
import io.mockk.mockk
import io.mockk.verify
import me.kmozze.expensetracker.adapter.ui.Buttons
import me.kmozze.expensetracker.exception.BusinessErrorCode
import me.kmozze.expensetracker.handler.statehandler.AwaitingExpenseDateEditSelectionHandler
import me.kmozze.expensetracker.model.domain.BotAction
import me.kmozze.expensetracker.model.domain.BotText
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
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.util.UUID

@ExtendWith(MockKExtension::class)
class AwaitingExpenseDateEditSelectionHandlerTest {
    private val expenseService: ExpenseService = mockk()
    private val categoryService: CategoryService = mockk()
    private lateinit var handler: AwaitingExpenseDateEditSelectionHandler

    @BeforeEach
    fun setUp() {
        handler =
            AwaitingExpenseDateEditSelectionHandler(
                expenseService = expenseService,
                categoryService = categoryService,
                clock = CLOCK,
            )
    }

    @Test
    fun `today selection updates date`() {
        every { expenseService.updateExpenseDateForUser(USER_ID, EXPENSE_ID, TODAY) } returns EXPENSE.copy(expenseDate = TODAY)
        every { expenseService.findExpenseForUser(USER_ID, EXPENSE_ID) } returns EXPENSE.copy(expenseDate = TODAY)
        every { categoryService.findCategoryForUser(CATEGORY_ID, USER_ID) } returns CATEGORY

        val result = handle(UserCommand.PlainText(Buttons.TODAY))

        assertThat(result.response.outgoingMessages).hasSize(2)
        assertThat(result.response.outgoingMessages[0].text).isEqualTo(BotText.Done)
        assertThat(result.response.outgoingMessages[1].text).isEqualTo(
            BotText.ExpenseSaved(
                amount = EXPENSE_AMOUNT,
                categoryName = CATEGORY.name,
                expenseDate = TODAY,
                description = EXPENSE.description,
            ),
        )
        assertThat(result.nextState).isEqualTo(UserState.Idle)
        verify(exactly = 1) { expenseService.updateExpenseDateForUser(USER_ID, EXPENSE_ID, TODAY) }
        verify(exactly = 1) { expenseService.findExpenseForUser(USER_ID, EXPENSE_ID) }
        verify(exactly = 1) { categoryService.findCategoryForUser(CATEGORY_ID, USER_ID) }
        confirmVerified(expenseService, categoryService)
    }

    @Test
    fun `yesterday selection updates date`() {
        val yesterday = YESTERDAY
        every { expenseService.updateExpenseDateForUser(USER_ID, EXPENSE_ID, yesterday) } returns EXPENSE.copy(expenseDate = yesterday)
        every { expenseService.findExpenseForUser(USER_ID, EXPENSE_ID) } returns EXPENSE.copy(expenseDate = yesterday)
        every { categoryService.findCategoryForUser(CATEGORY_ID, USER_ID) } returns CATEGORY

        val result = handle(UserCommand.PlainText(Buttons.YESTERDAY))

        assertThat(result.response.outgoingMessages[1].text)
            .isEqualTo(
                BotText.ExpenseSaved(
                    amount = EXPENSE_AMOUNT,
                    categoryName = CATEGORY.name,
                    expenseDate = yesterday,
                    description = EXPENSE.description,
                ),
            )
        assertThat(result.nextState).isEqualTo(UserState.Idle)
        verify(exactly = 1) { expenseService.updateExpenseDateForUser(USER_ID, EXPENSE_ID, yesterday) }
        verify(exactly = 1) { expenseService.findExpenseForUser(USER_ID, EXPENSE_ID) }
        verify(exactly = 1) { categoryService.findCategoryForUser(CATEGORY_ID, USER_ID) }
        confirmVerified(expenseService, categoryService)
    }

    @Test
    fun `manual selection asks for manual date input`() {
        every { expenseService.findExpenseForUser(USER_ID, EXPENSE_ID) } returns EXPENSE
        every { categoryService.findCategoryForUser(CATEGORY_ID, USER_ID) } returns CATEGORY

        val result = handle(UserCommand.PlainText(Buttons.ENTER_DATE_MANUALLY))

        val outgoingMessages = result.response.outgoingMessages
        assertThat(outgoingMessages.single().text).isEqualTo(
            BotText.EnterExpenseDateManually(
                amount = EXPENSE_AMOUNT,
                categoryName = CATEGORY.name,
                description = EXPENSE.description,
            ),
        )
        assertThat(outgoingMessages.single().actions).containsExactly(BotAction.ShowCancel)
        assertThat(result.nextState).isEqualTo(UserState.AwaitingExpenseDateEditManualInput(expenseId = EXPENSE_ID))
        verify(exactly = 1) { expenseService.findExpenseForUser(USER_ID, EXPENSE_ID) }
        verify(exactly = 1) { categoryService.findCategoryForUser(CATEGORY_ID, USER_ID) }
        confirmVerified(expenseService, categoryService)
    }

    @Test
    fun `invalid date text repeats selection`() {
        every { expenseService.findExpenseForUser(USER_ID, EXPENSE_ID) } returns EXPENSE
        val result = handle(UserCommand.PlainText("завтра"))

        val outgoingMessages = result.response.outgoingMessages
        assertThat(outgoingMessages.single().text).isEqualTo(
            BotText.Error(BusinessErrorCode.INVALID_EXPENSE_DATE_SELECTION),
        )
        assertThat(outgoingMessages.single().actions).containsExactly(BotAction.ShowExpenseDateSelection)
        assertThat(result.nextState).isEqualTo(UserState.AwaitingExpenseDateEditSelection(expenseId = EXPENSE_ID))
        verify(exactly = 0) { expenseService.findExpenseForUser(USER_ID, EXPENSE_ID) }
        verify(exactly = 0) { categoryService.findCategoryForUser(CATEGORY_ID, USER_ID) }
        confirmVerified(expenseService, categoryService)
    }

    @Test
    fun `cancel returns expense card`() {
        every { expenseService.findExpenseForUser(USER_ID, EXPENSE_ID) } returns EXPENSE
        every { categoryService.findCategoryForUser(CATEGORY_ID, USER_ID) } returns CATEGORY

        val result = handle(UserCommand.Cancel)

        assertThat(result.response.outgoingMessages[0].text).isEqualTo(BotText.Done)
        assertThat(result.nextState).isEqualTo(UserState.Idle)
        verify(exactly = 1) { expenseService.findExpenseForUser(USER_ID, EXPENSE_ID) }
        verify(exactly = 1) { categoryService.findCategoryForUser(CATEGORY_ID, USER_ID) }
        confirmVerified(expenseService, categoryService)
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
            currentState = AWAITING_EXPENSE_DATE_EDIT_SELECTION,
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
        val CLOCK: Clock = Clock.fixed(Instant.parse("2026-05-24T12:00:00Z"), ZoneOffset.UTC)
        val TODAY: LocalDate = LocalDate.parse("2026-05-24")
        val YESTERDAY: LocalDate = LocalDate.parse("2026-05-23")
        val EXPENSE: Expense =
            Expense(
                id = EXPENSE_ID,
                categoryId = CATEGORY_ID,
                amount = EXPENSE_AMOUNT,
                userId = USER_ID,
                expenseDate = TODAY,
                description = "такси",
            )
        val CATEGORY: Category =
            Category(
                id = CATEGORY_ID,
                name = "Транспорт",
                userId = USER_ID,
            )
        val AWAITING_EXPENSE_DATE_EDIT_SELECTION: UserState.AwaitingExpenseDateEditSelection =
            UserState.AwaitingExpenseDateEditSelection(
                expenseId = EXPENSE_ID,
            )
    }
}
