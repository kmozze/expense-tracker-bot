package me.kmozze.expensetracker.unit.handler.statehandler

import io.mockk.confirmVerified
import io.mockk.every
import io.mockk.junit5.MockKExtension
import io.mockk.mockk
import io.mockk.verify
import me.kmozze.expensetracker.handler.statehandler.AwaitingExpenseEditFieldSelectionHandler
import me.kmozze.expensetracker.model.domain.BotAction
import me.kmozze.expensetracker.model.domain.BotText
import me.kmozze.expensetracker.model.domain.ExpenseDraft
import me.kmozze.expensetracker.model.domain.ExpenseEditField
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
    fun `amount selection asks for amount text and carries draft`() {
        val result = handle(UserCommand.SelectExpenseEditField(ExpenseEditField.Amount))

        assertThat(result.outgoingMessages.single().text).isEqualTo(BotText.EnterExpenseAmount)
        assertThat(result.outgoingMessages.single().actions).containsExactly(BotAction.ShowCancel)
        assertThat(result.nextState)
            .isEqualTo(
                UserState.AwaitingExpenseAmountEdit(
                    expenseId = EXPENSE_ID,
                    expenseDraft = EXPENSE_DRAFT,
                    categoryName = CATEGORY.name,
                ),
            )
        confirmVerified(expenseService, categoryService)
    }

    @Test
    fun `description selection asks for description text and carries draft`() {
        val result = handle(UserCommand.SelectExpenseEditField(ExpenseEditField.Description))

        assertThat(result.outgoingMessages.single().text).isEqualTo(BotText.EnterExpenseDescription)
        assertThat(result.outgoingMessages.single().actions).containsExactly(BotAction.ShowCancel)
        assertThat(result.nextState)
            .isEqualTo(
                UserState.AwaitingExpenseDescriptionEdit(
                    expenseId = EXPENSE_ID,
                    expenseDraft = EXPENSE_DRAFT,
                    categoryName = CATEGORY.name,
                ),
            )
        confirmVerified(expenseService, categoryService)
    }

    @Test
    fun `category selection shows categories with draft card`() {
        every { categoryService.getCategories(USER_ID) } returns listOf(CATEGORY)

        val result = handle(UserCommand.SelectExpenseEditField(ExpenseEditField.Category))

        assertThat(result.outgoingMessages).hasSize(2)
        assertThat(result.outgoingMessages[0].text).isEqualTo(EXPENSE_VIEW)
        assertThat(result.outgoingMessages[0].actions).isEmpty()
        assertThat(result.outgoingMessages[1].text).isEqualTo(BotText.SelectCategory)
        assertThat(result.outgoingMessages[1].actions).containsExactly(BotAction.ShowCategorySelection(listOf(CATEGORY.name)))
        assertThat(result.nextState)
            .isEqualTo(
                UserState.AwaitingExpenseCategoryEdit(
                    expenseId = EXPENSE_ID,
                    expenseDraft = EXPENSE_DRAFT,
                    categoryName = CATEGORY.name,
                ),
            )
        verify(exactly = 1) { categoryService.getCategories(USER_ID) }
        confirmVerified(expenseService, categoryService)
    }

    @Test
    fun `date selection opens date choice with draft card`() {
        val result = handle(UserCommand.SelectExpenseEditField(ExpenseEditField.Date))

        assertThat(result.outgoingMessages).hasSize(2)
        assertThat(result.outgoingMessages[0].text).isEqualTo(EXPENSE_VIEW)
        assertThat(result.outgoingMessages[0].actions).isEmpty()
        assertThat(result.outgoingMessages[1].text).isEqualTo(BotText.SelectExpenseDate)
        assertThat(result.outgoingMessages[1].actions).containsExactly(BotAction.ShowExpenseDateSelection)
        assertThat(result.nextState)
            .isEqualTo(
                UserState.AwaitingExpenseDateEditSelection(
                    expenseId = EXPENSE_ID,
                    expenseDraft = EXPENSE_DRAFT,
                    categoryName = CATEGORY.name,
                ),
            )
        confirmVerified(expenseService, categoryService)
    }

    @Test
    fun `invalid field selection repeats with same state`() {
        val result = handle(UserCommand.PlainText("Неверно"))

        assertThat(result.outgoingMessages.single().text).isEqualTo(BotText.EditExpenseFieldSelection)
        assertThat(result.outgoingMessages.single().actions).containsExactly(BotAction.ShowExpenseEditFieldSelection)
        assertThat(result.nextState).isEqualTo(AWAITING_EXPENSE_EDIT_FIELD_SELECTION)
        confirmVerified(expenseService, categoryService)
    }

    @Test
    fun `finish saves draft and returns saved card with actions`() {
        every {
            expenseService.updateExpenseFromDraftForUser(USER_ID, EXPENSE_ID, EXPENSE_DRAFT)
        } returns EXPENSE
        every { categoryService.findCategoryForUser(CATEGORY_ID, USER_ID) } returns CATEGORY

        val result = handle(UserCommand.FinishExpenseEdit)

        assertThat(result.outgoingMessages).hasSize(2)
        assertThat(result.outgoingMessages[0].text).isEqualTo(BotText.ExpenseSaved)
        assertThat(result.outgoingMessages[0].actions).containsExactly(BotAction.RemoveReplyKeyboard)
        assertThat(result.outgoingMessages[1].text).isEqualTo(EXPENSE_VIEW)
        assertThat(result.outgoingMessages[1].actions).containsExactly(BotAction.ShowExpenseCardActions(EXPENSE_ID))
        assertThat(result.nextState).isEqualTo(UserState.Idle)
        verify(exactly = 1) { expenseService.updateExpenseFromDraftForUser(USER_ID, EXPENSE_ID, EXPENSE_DRAFT) }
        verify(exactly = 1) { categoryService.findCategoryForUser(CATEGORY_ID, USER_ID) }
        confirmVerified(expenseService, categoryService)
    }

    @Test
    fun `cancel returns saved card with actions`() {
        every { expenseService.findExpenseForUser(USER_ID, EXPENSE_ID) } returns EXPENSE
        every { categoryService.findCategoryForUser(CATEGORY_ID, USER_ID) } returns CATEGORY

        val result = handle(UserCommand.Cancel)

        assertThat(result.outgoingMessages).hasSize(2)
        assertThat(result.outgoingMessages[0].text).isEqualTo(BotText.Done)
        assertThat(result.outgoingMessages[0].actions).containsExactly(BotAction.RemoveReplyKeyboard)
        assertThat(result.outgoingMessages[1].text).isEqualTo(EXPENSE_VIEW)
        assertThat(result.outgoingMessages[1].actions).containsExactly(BotAction.ShowExpenseCardActions(EXPENSE_ID))
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
        val EXPENSE_DRAFT: ExpenseDraft =
            ExpenseDraft(
                amount = EXPENSE_AMOUNT,
                description = "такси",
                categoryId = CATEGORY_ID,
                expenseDate = EXPENSE_DATE,
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
        val EXPENSE_VIEW: BotText.ExpenseView =
            BotText.ExpenseView(
                amount = EXPENSE_AMOUNT,
                categoryName = CATEGORY.name,
                expenseDate = EXPENSE_DATE,
                description = EXPENSE.description,
            )
        val AWAITING_EXPENSE_EDIT_FIELD_SELECTION: UserState.AwaitingExpenseEditFieldSelection =
            UserState.AwaitingExpenseEditFieldSelection(
                expenseId = EXPENSE_ID,
                expenseDraft = EXPENSE_DRAFT,
                categoryName = CATEGORY.name,
            )
    }
}
