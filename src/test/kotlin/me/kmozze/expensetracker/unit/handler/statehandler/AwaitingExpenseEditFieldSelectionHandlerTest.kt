package me.kmozze.expensetracker.unit.handler.statehandler

import io.mockk.confirmVerified
import io.mockk.every
import io.mockk.junit5.MockKExtension
import io.mockk.mockk
import io.mockk.verify
import me.kmozze.expensetracker.adapter.ui.Buttons
import me.kmozze.expensetracker.handler.statehandler.AwaitingExpenseEditFieldSelectionHandler
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
import java.time.LocalDate
import java.util.UUID

@ExtendWith(MockKExtension::class)
class AwaitingExpenseEditFieldSelectionHandlerTest {
    private val expenseService: ExpenseService = mockk()
    private val categoryService: CategoryService = mockk()
    private lateinit var handler: AwaitingExpenseEditFieldSelectionHandler

    @BeforeEach
    fun setUp() {
        handler =
            AwaitingExpenseEditFieldSelectionHandler(
                expenseService = expenseService,
                categoryService = categoryService,
            )
    }

    @Test
    fun `amount selection asks for amount text`() {
        val result = handle(UserCommand.PlainText(Buttons.EDIT_EXPENSE_AMOUNT))

        assertThat(
            result.response.outgoingMessages
                .single()
                .text,
        ).isEqualTo(BotText.EnterExpenseAmount)
        assertThat(
            result.response.outgoingMessages
                .single()
                .actions,
        ).containsExactly(BotAction.ShowCancel)
        assertThat(result.nextState)
            .isEqualTo(
                UserState.AwaitingExpenseAmountEdit(
                    expenseId = EXPENSE_ID,
                ),
            )
        confirmVerified(expenseService, categoryService)
    }

    @Test
    fun `description selection asks for description text`() {
        val result = handle(UserCommand.PlainText(Buttons.EDIT_EXPENSE_DESCRIPTION))

        assertThat(
            result.response.outgoingMessages
                .single()
                .text,
        ).isEqualTo(BotText.EnterExpenseDescription)
        assertThat(
            result.response.outgoingMessages
                .single()
                .actions,
        ).containsExactly(BotAction.ShowCancel)
        assertThat(result.nextState)
            .isEqualTo(
                UserState.AwaitingExpenseDescriptionEdit(
                    expenseId = EXPENSE_ID,
                ),
            )
        confirmVerified(expenseService, categoryService)
    }

    @Test
    fun `category selection shows categories`() {
        every { expenseService.findExpenseForUser(USER_ID, EXPENSE_ID) } returns EXPENSE
        every { categoryService.getCategories(USER_ID) } returns listOf(CATEGORY)

        val result = handle(UserCommand.PlainText(Buttons.EDIT_EXPENSE_CATEGORY))

        assertThat(
            result.response.outgoingMessages
                .single()
                .text,
        ).isEqualTo(BotText.SelectCategory(amount = EXPENSE_AMOUNT, description = EXPENSE.description))
        assertThat(
            result.response.outgoingMessages
                .single()
                .actions,
        ).containsExactly(BotAction.ShowCategorySelection(listOf(CATEGORY)))
        assertThat(result.nextState)
            .isEqualTo(UserState.AwaitingExpenseCategoryEdit(expenseId = EXPENSE_ID))
        verify(exactly = 1) { expenseService.findExpenseForUser(USER_ID, EXPENSE_ID) }
        verify(exactly = 1) { categoryService.getCategories(USER_ID) }
        confirmVerified(expenseService, categoryService)
    }

    @Test
    fun `date selection opens date choice`() {
        every { expenseService.findExpenseForUser(USER_ID, EXPENSE_ID) } returns EXPENSE
        every { categoryService.findCategoryForUser(CATEGORY_ID, USER_ID) } returns CATEGORY

        val result = handle(UserCommand.PlainText(Buttons.EDIT_EXPENSE_DATE))

        assertThat(
            result.response.outgoingMessages
                .single()
                .text,
        ).isEqualTo(
            BotText.SelectExpenseDate(
                amount = EXPENSE_AMOUNT,
                categoryName = CATEGORY.name,
                description = EXPENSE.description,
            ),
        )
        assertThat(
            result.response.outgoingMessages
                .single()
                .actions,
        ).containsExactly(BotAction.ShowExpenseDateSelection)
        assertThat(result.nextState)
            .isEqualTo(
                UserState.AwaitingExpenseDateEditSelection(
                    expenseId = EXPENSE_ID,
                ),
            )
        verify(exactly = 1) { expenseService.findExpenseForUser(USER_ID, EXPENSE_ID) }
        verify(exactly = 1) { categoryService.findCategoryForUser(CATEGORY_ID, USER_ID) }
        confirmVerified(expenseService, categoryService)
    }

    @Test
    fun `invalid field selection repeats with same state`() {
        every { expenseService.findExpenseForUser(USER_ID, EXPENSE_ID) } returns EXPENSE

        val result = handle(UserCommand.PlainText("Неверно"))

        assertThat(
            result.response.outgoingMessages
                .single()
                .text,
        ).isEqualTo(BotText.EditExpenseFieldSelection)
        assertThat(
            result.response.outgoingMessages
                .single()
                .actions,
        ).containsExactly(BotAction.ShowExpenseEditFieldSelection)
        assertThat(result.nextState).isEqualTo(UserState.AwaitingExpenseEditFieldSelection(EXPENSE_ID))
        verify(exactly = 1) { expenseService.findExpenseForUser(USER_ID, EXPENSE_ID) }
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
                BotText.ExpenseSaved(
                    amount = EXPENSE_AMOUNT,
                    categoryName = CATEGORY.name,
                    expenseDate = EXPENSE_DATE,
                    description = EXPENSE.description,
                ),
            )
        assertThat(result.response.outgoingMessages[1].actions)
            .containsExactly(BotAction.ShowExpenseCardActions(EXPENSE_ID))
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
            currentState = AWAITING_EXPENSE_EDIT_FIELD_SELECTION,
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
                description = "такси",
            )
        val AWAITING_EXPENSE_EDIT_FIELD_SELECTION: UserState.AwaitingExpenseEditFieldSelection =
            UserState.AwaitingExpenseEditFieldSelection(
                expenseId = EXPENSE_ID,
            )
    }
}
