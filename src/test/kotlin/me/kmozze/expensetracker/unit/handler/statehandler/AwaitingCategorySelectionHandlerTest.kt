package me.kmozze.expensetracker.unit.handler.statehandler

import io.mockk.confirmVerified
import io.mockk.every
import io.mockk.junit5.MockKExtension
import io.mockk.mockk
import io.mockk.verify
import me.kmozze.expensetracker.exception.BusinessErrorCode
import me.kmozze.expensetracker.handler.statehandler.AwaitingCategorySelectionHandler
import me.kmozze.expensetracker.model.domain.BotAction
import me.kmozze.expensetracker.model.domain.BotMessage
import me.kmozze.expensetracker.model.domain.Money
import me.kmozze.expensetracker.model.domain.ParsedExpense
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
class AwaitingCategorySelectionHandlerTest {
    private val categoryService: CategoryService = mockk()
    private val expenseService: ExpenseService = mockk()
    private lateinit var handler: AwaitingCategorySelectionHandler

    @BeforeEach
    fun setUp() {
        handler =
            AwaitingCategorySelectionHandler(
                categoryService = categoryService,
                expenseService = expenseService,
            )
    }

    @Test
    fun `selected category saves expense and returns idle`() {
        val category = category(id = CATEGORY_ID)
        val savedExpense = expense(categoryId = CATEGORY_ID)
        every { categoryService.findCategoryForUser(CATEGORY_ID, USER_ID) } returns category
        every {
            expenseService.saveExpense(
                userId = USER_ID,
                categoryId = CATEGORY_ID,
                parsedExpense = PARSED_EXPENSE,
            )
        } returns savedExpense

        val result = handle(UserCommand.SelectCategory(CATEGORY_ID))

        assertThat(result.response.message)
            .isEqualTo(
                BotMessage.ExpenseSaved(
                    amount = EXPENSE_AMOUNT,
                    categoryName = category.name,
                    expenseDate = EXPENSE_DATE,
                    description = EXPENSE_DESCRIPTION,
                ),
            )
        assertThat(result.response.actions).containsExactly(BotAction.ShowMainMenu)
        assertThat(result.nextState).isEqualTo(UserState.Idle)
        verify(exactly = 1) { categoryService.findCategoryForUser(CATEGORY_ID, USER_ID) }
        verify(exactly = 1) {
            expenseService.saveExpense(
                userId = USER_ID,
                categoryId = CATEGORY_ID,
                parsedExpense = PARSED_EXPENSE,
            )
        }
        confirmVerified(categoryService, expenseService)
    }

    @Test
    fun `missing category keeps category selection open with error`() {
        val categories = listOf(category(id = CATEGORY_ID), category(id = SECOND_CATEGORY_ID))
        every { categoryService.findCategoryForUser(CATEGORY_ID, USER_ID) } returns null
        every { categoryService.getCategories(USER_ID) } returns categories

        val result = handle(UserCommand.SelectCategory(CATEGORY_ID))

        assertThat(result.response.message).isEqualTo(BotMessage.Error(BusinessErrorCode.CATEGORY_NOT_FOUND))
        assertThat(result.response.actions).containsExactly(BotAction.ShowCategorySelection(categories))
        assertThat(result.nextState).isEqualTo(AWAITING_CATEGORY_SELECTION)
        verify(exactly = 1) { categoryService.findCategoryForUser(CATEGORY_ID, USER_ID) }
        verify(exactly = 1) { categoryService.getCategories(USER_ID) }
        confirmVerified(categoryService, expenseService)
    }

    @Test
    fun `invalid category selection keeps category selection open with error`() {
        val categories = listOf(category(id = CATEGORY_ID), category(id = SECOND_CATEGORY_ID))
        every { categoryService.getCategories(USER_ID) } returns categories

        val result = handle(UserCommand.InvalidCategorySelection)

        assertThat(result.response.message)
            .isEqualTo(BotMessage.Error(BusinessErrorCode.INVALID_CATEGORY_SELECTION))
        assertThat(result.response.actions).containsExactly(BotAction.ShowCategorySelection(categories))
        assertThat(result.nextState).isEqualTo(AWAITING_CATEGORY_SELECTION)
        verify(exactly = 1) { categoryService.getCategories(USER_ID) }
        confirmVerified(categoryService, expenseService)
    }

    @Test
    fun `cancel returns idle without service calls`() {
        val result = handle(UserCommand.Cancel)

        assertThat(result.response.message).isEqualTo(BotMessage.ExpenseCanceled)
        assertThat(result.response.actions).containsExactly(BotAction.ShowMainMenu)
        assertThat(result.nextState).isEqualTo(UserState.Idle)
        confirmVerified(categoryService, expenseService)
    }

    @Test
    fun `non-selection command repeats category selection`() {
        val categories = listOf(category(id = CATEGORY_ID), category(id = SECOND_CATEGORY_ID))
        every { categoryService.getCategories(USER_ID) } returns categories

        val result = handle(UserCommand.PlainText("700 кофе"))

        assertThat(result.response.message)
            .isEqualTo(BotMessage.SelectCategory(EXPENSE_AMOUNT, EXPENSE_DESCRIPTION))
        assertThat(result.response.actions).containsExactly(BotAction.ShowCategorySelection(categories))
        assertThat(result.nextState).isEqualTo(AWAITING_CATEGORY_SELECTION)
        verify(exactly = 1) { categoryService.getCategories(USER_ID) }
        confirmVerified(categoryService, expenseService)
    }

    private fun handle(command: UserCommand) =
        handler.handle(
            input =
                makeUserInput(
                    userId = USER_ID,
                    chatId = CHAT_ID,
                    callbackData = "callback",
                    command = command,
                ),
            currentState = AWAITING_CATEGORY_SELECTION,
        )

    private fun category(id: UUID): Category =
        Category(
            id = id,
            name = "Еда",
            userId = USER_ID,
        )

    private fun expense(categoryId: UUID): Expense =
        Expense(
            categoryId = categoryId,
            amount = EXPENSE_AMOUNT,
            userId = USER_ID,
            expenseDate = EXPENSE_DATE,
            description = EXPENSE_DESCRIPTION,
        )

    private companion object {
        const val USER_ID = 123L
        const val CHAT_ID = 456L
        const val EXPENSE_DESCRIPTION = "такси"
        val EXPENSE_DATE: LocalDate = LocalDate.parse("2026-05-24")
        val EXPENSE_AMOUNT: Money = Money.of(BigDecimal("500.00"))
        val PARSED_EXPENSE: ParsedExpense = ParsedExpense(EXPENSE_AMOUNT, EXPENSE_DESCRIPTION)
        val AWAITING_CATEGORY_SELECTION: UserState.AwaitingCategorySelection =
            UserState.AwaitingCategorySelection(PARSED_EXPENSE)
        val CATEGORY_ID: UUID = UUID.fromString("00000000-0000-0000-0000-000000000001")
        val SECOND_CATEGORY_ID: UUID = UUID.fromString("00000000-0000-0000-0000-000000000002")
    }
}
