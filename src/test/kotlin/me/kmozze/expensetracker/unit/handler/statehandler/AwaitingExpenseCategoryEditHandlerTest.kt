package me.kmozze.expensetracker.unit.handler.statehandler

import io.mockk.confirmVerified
import io.mockk.every
import io.mockk.junit5.MockKExtension
import io.mockk.mockk
import io.mockk.verify
import me.kmozze.expensetracker.exception.BusinessErrorCode
import me.kmozze.expensetracker.handler.statehandler.AwaitingExpenseCategoryEditHandler
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
class AwaitingExpenseCategoryEditHandlerTest {
    private val expenseService: ExpenseService = mockk()
    private val categoryService: CategoryService = mockk()
    private lateinit var handler: AwaitingExpenseCategoryEditHandler

    @BeforeEach
    fun setUp() {
        handler =
            AwaitingExpenseCategoryEditHandler(
                expenseService = expenseService,
                categoryService = categoryService,
            )
    }

    @Test
    fun `existing category updates expense and updates card`() {
        val updatedExpense = EXPENSE.copy(categoryId = UPDATED_CATEGORY_ID)
        every { categoryService.getCategories(USER_ID) } returns listOf(EXISTING_CATEGORY, UPDATED_CATEGORY)
        every { expenseService.updateExpenseCategoryForUser(USER_ID, EXPENSE_ID, UPDATED_CATEGORY_ID) } returns updatedExpense
        every { expenseService.findExpenseForUser(USER_ID, EXPENSE_ID) } returns updatedExpense
        every { categoryService.findCategoryForUser(UPDATED_CATEGORY_ID, USER_ID) } returns UPDATED_CATEGORY

        val result = handle(UserCommand.PlainText(UPDATED_CATEGORY.name))

        assertThat(result.response.outgoingMessages).hasSize(2)
        assertThat(result.response.outgoingMessages[0].text).isEqualTo(BotText.Done)
        assertThat(result.response.outgoingMessages[0].actions)
            .containsExactly(BotAction.RemoveReplyKeyboard)
        assertThat(result.response.outgoingMessages[1].text)
            .isEqualTo(
                BotText.ExpenseSaved(
                    amount = EXPENSE_AMOUNT,
                    categoryName = UPDATED_CATEGORY.name,
                    expenseDate = EXPENSE_DATE,
                    description = EXPENSE_DESCRIPTION,
                ),
            )
        assertThat(result.response.outgoingMessages[1].actions)
            .containsExactly(BotAction.ShowExpenseCardActions(EXPENSE_ID))
        assertThat(result.nextState).isEqualTo(UserState.Idle)
        verify(exactly = 1) { categoryService.getCategories(USER_ID) }
        verify(exactly = 1) { expenseService.updateExpenseCategoryForUser(USER_ID, EXPENSE_ID, UPDATED_CATEGORY_ID) }
        verify(exactly = 1) { expenseService.findExpenseForUser(USER_ID, EXPENSE_ID) }
        verify(exactly = 1) { categoryService.findCategoryForUser(UPDATED_CATEGORY_ID, USER_ID) }
        confirmVerified(expenseService, categoryService)
    }

    @Test
    fun `missing category repeats selection when categories exist`() {
        val categories = listOf(EXISTING_CATEGORY, UPDATED_CATEGORY)
        every { categoryService.getCategories(USER_ID) } returns categories

        val result = handle(UserCommand.PlainText("Нет такой"))

        assertThat(
            result.response.outgoingMessages
                .single()
                .text,
        ).isEqualTo(BotText.Error(BusinessErrorCode.CATEGORY_NOT_FOUND))
        assertThat(
            result.response.outgoingMessages
                .single()
                .actions,
        ).containsExactly(BotAction.ShowCategorySelection(categories.map { it.name }))
        assertThat(result.nextState).isEqualTo(AWAITING_EXPENSE_CATEGORY_EDIT)
        verify(exactly = 1) { categoryService.getCategories(USER_ID) }
        confirmVerified(expenseService, categoryService)
    }

    @Test
    fun `missing categories closes dialog and returns main menu`() {
        every { categoryService.getCategories(USER_ID) } returns emptyList()

        val result = handle(UserCommand.PlainText("Нет такой"))

        assertThat(
            result.response.outgoingMessages
                .single()
                .text,
        ).isEqualTo(BotText.NoCategories)
        assertThat(
            result.response.outgoingMessages
                .single()
                .actions,
        ).containsExactly(BotAction.ShowMainMenu)
        assertThat(result.nextState).isEqualTo(UserState.Idle)
        verify(exactly = 1) { categoryService.getCategories(USER_ID) }
        confirmVerified(expenseService, categoryService)
    }

    @Test
    fun `unavailable expense keeps card unavailable message`() {
        every { expenseService.updateExpenseCategoryForUser(USER_ID, EXPENSE_ID, UPDATED_CATEGORY_ID) } returns null
        every { categoryService.getCategories(USER_ID) } returns listOf(EXISTING_CATEGORY, UPDATED_CATEGORY)

        val result = handle(UserCommand.PlainText(UPDATED_CATEGORY.name))

        assertThat(
            result.response.outgoingMessages
                .single()
                .text,
        ).isEqualTo(BotText.ExpenseUnavailable)
        assertThat(
            result.response.outgoingMessages
                .single()
                .actions,
        ).containsExactly(
            BotAction.ClearInlineKeyboard,
            BotAction.RemoveReplyKeyboard,
        )
        assertThat(result.nextState).isEqualTo(UserState.Idle)
        verify(exactly = 1) { categoryService.getCategories(USER_ID) }
        verify(exactly = 1) { expenseService.updateExpenseCategoryForUser(USER_ID, EXPENSE_ID, UPDATED_CATEGORY_ID) }
        confirmVerified(expenseService, categoryService)
    }

    @Test
    fun `cancel returns card with actions`() {
        every { expenseService.findExpenseForUser(USER_ID, EXPENSE_ID) } returns EXPENSE
        every { categoryService.findCategoryForUser(EXISTING_CATEGORY_ID, USER_ID) } returns EXISTING_CATEGORY

        val result = handle(UserCommand.Cancel)

        assertThat(result.response.outgoingMessages).hasSize(2)
        assertThat(result.response.outgoingMessages[0].text).isEqualTo(BotText.Done)
        assertThat(result.response.outgoingMessages[0].actions)
            .containsExactly(BotAction.RemoveReplyKeyboard)
        assertThat(result.response.outgoingMessages[1].text)
            .isEqualTo(
                BotText.ExpenseSaved(
                    amount = EXPENSE_AMOUNT,
                    categoryName = EXISTING_CATEGORY.name,
                    expenseDate = EXPENSE_DATE,
                    description = EXPENSE_DESCRIPTION,
                ),
            )
        assertThat(result.response.outgoingMessages[1].actions)
            .containsExactly(BotAction.ShowExpenseCardActions(EXPENSE_ID))
        assertThat(result.nextState).isEqualTo(UserState.Idle)
        verify(exactly = 1) { expenseService.findExpenseForUser(USER_ID, EXPENSE_ID) }
        verify(exactly = 1) { categoryService.findCategoryForUser(EXISTING_CATEGORY_ID, USER_ID) }
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
            currentState = AWAITING_EXPENSE_CATEGORY_EDIT,
        )

    private fun textFor(command: UserCommand): String? =
        when (command) {
            is UserCommand.PlainText -> command.value
            else -> null
        }

    private companion object {
        const val USER_ID = 123L
        const val CHAT_ID = 456L
        val EXPENSE_ID: UUID = UUID.fromString("00000000-0000-0000-0000-000000000001")
        val EXISTING_CATEGORY_ID: UUID = UUID.fromString("00000000-0000-0000-0000-000000000002")
        val UPDATED_CATEGORY_ID: UUID = UUID.fromString("00000000-0000-0000-0000-000000000003")
        val EXPENSE_AMOUNT: Money = Money.of(BigDecimal("500.00"))
        val EXPENSE_DATE: LocalDate = LocalDate.parse("2026-05-24")
        const val EXPENSE_DESCRIPTION = "такси"
        val EXISTING_CATEGORY =
            Category(
                id = EXISTING_CATEGORY_ID,
                name = "Транспорт",
                userId = USER_ID,
            )
        val UPDATED_CATEGORY =
            Category(
                id = UPDATED_CATEGORY_ID,
                name = "Еда",
                userId = USER_ID,
            )
        val EXPENSE =
            Expense(
                id = EXPENSE_ID,
                categoryId = EXISTING_CATEGORY_ID,
                amount = EXPENSE_AMOUNT,
                userId = USER_ID,
                expenseDate = EXPENSE_DATE,
                description = EXPENSE_DESCRIPTION,
            )
        val AWAITING_EXPENSE_CATEGORY_EDIT =
            UserState.AwaitingExpenseCategoryEdit(
                expenseId = EXPENSE_ID,
            )
    }
}
