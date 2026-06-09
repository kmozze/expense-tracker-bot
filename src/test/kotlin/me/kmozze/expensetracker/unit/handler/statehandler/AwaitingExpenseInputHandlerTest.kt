package me.kmozze.expensetracker.unit.handler.statehandler

import io.mockk.confirmVerified
import io.mockk.every
import io.mockk.junit5.MockKExtension
import io.mockk.mockk
import io.mockk.verify
import me.kmozze.expensetracker.exception.BusinessErrorCode
import me.kmozze.expensetracker.exception.exception
import me.kmozze.expensetracker.handler.statehandler.AwaitingExpenseInputHandler
import me.kmozze.expensetracker.model.domain.BotAction
import me.kmozze.expensetracker.model.domain.BotText
import me.kmozze.expensetracker.model.domain.ExpenseDraft
import me.kmozze.expensetracker.model.domain.Money
import me.kmozze.expensetracker.model.domain.UserCommand
import me.kmozze.expensetracker.model.domain.UserState
import me.kmozze.expensetracker.model.entity.Category
import me.kmozze.expensetracker.service.CategoryService
import me.kmozze.expensetracker.service.ExpenseService
import me.kmozze.expensetracker.support.makeUserInput
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import java.math.BigDecimal
import java.util.UUID

@ExtendWith(MockKExtension::class)
class AwaitingExpenseInputHandlerTest {
    private val expenseService: ExpenseService = mockk()
    private val categoryService: CategoryService = mockk()
    private lateinit var handler: AwaitingExpenseInputHandler

    @BeforeEach
    fun setUp() {
        handler =
            AwaitingExpenseInputHandler(
                expenseService = expenseService,
                categoryService = categoryService,
            )
    }

    @Test
    fun `plain text parses expense and opens category selection`() {
        val categories = listOf(category(id = FIRST_CATEGORY_ID), category(id = SECOND_CATEGORY_ID))
        every { expenseService.parseExpense(EXPENSE_TEXT) } returns EXPENSE_DRAFT
        every { categoryService.getCategories(USER_ID) } returns categories

        val result = handle(UserCommand.PlainText(EXPENSE_TEXT))

        assertThat(result.outgoingMessages).hasSize(2)
        assertThat(result.outgoingMessages[0].text)
            .isEqualTo(
                BotText.ExpenseView(
                    amount = EXPENSE_AMOUNT,
                    categoryName = null,
                    expenseDate = null,
                    description = EXPENSE_DESCRIPTION,
                ),
            )
        assertThat(result.outgoingMessages[0].actions).isEmpty()
        assertThat(result.outgoingMessages[1].text).isEqualTo(BotText.SelectCategory)
        assertThat(result.outgoingMessages[1].actions)
            .containsExactly(BotAction.ShowCategorySelection(categories.map { it.name }))
        assertThat(result.nextState).isEqualTo(UserState.AwaitingCategorySelection(EXPENSE_DRAFT))
        verify(exactly = 1) { expenseService.parseExpense(EXPENSE_TEXT) }
        verify(exactly = 1) { categoryService.getCategories(USER_ID) }
        confirmVerified(expenseService, categoryService)
    }

    @Test
    fun `parse error keeps awaiting expense input without loading categories`() {
        every { expenseService.parseExpense(EXPENSE_TEXT) } throws BusinessErrorCode.EXPENSE_INVALID_FORMAT.exception()

        val result = handle(UserCommand.PlainText(EXPENSE_TEXT))

        assertThat(
            result.outgoingMessages
                .single()
                .text,
        ).isEqualTo(BotText.Error(BusinessErrorCode.EXPENSE_INVALID_FORMAT))
        assertThat(
            result.outgoingMessages
                .single()
                .actions,
        ).isEmpty()
        assertThat(result.nextState).isEqualTo(UserState.AwaitingExpenseInput)
        verify(exactly = 1) { expenseService.parseExpense(EXPENSE_TEXT) }
        confirmVerified(expenseService, categoryService)
    }

    @Test
    fun `parsed expense without categories returns no categories message`() {
        every { expenseService.parseExpense(EXPENSE_TEXT) } returns EXPENSE_DRAFT
        every { categoryService.getCategories(USER_ID) } returns emptyList()

        val result = handle(UserCommand.PlainText(EXPENSE_TEXT))

        assertThat(
            result.outgoingMessages
                .single()
                .text,
        ).isEqualTo(BotText.NoCategories)
        assertThat(
            result.outgoingMessages
                .single()
                .actions,
        ).containsExactly(BotAction.ShowMainMenu)
        assertThat(result.nextState).isEqualTo(UserState.Idle)
        verify(exactly = 1) { expenseService.parseExpense(EXPENSE_TEXT) }
        verify(exactly = 1) { categoryService.getCategories(USER_ID) }
        confirmVerified(expenseService, categoryService)
    }

    @Test
    fun `add expense command repeats input instructions without parsing`() {
        val result = handle(UserCommand.AddExpense)

        assertThat(
            result.outgoingMessages
                .single()
                .text,
        ).isEqualTo(BotText.AddExpenseInstructions)
        assertThat(
            result.outgoingMessages
                .single()
                .actions,
        ).isEmpty()
        assertThat(result.nextState).isEqualTo(UserState.AwaitingExpenseInput)
        confirmVerified(expenseService, categoryService)
    }

    @Test
    fun `menu command leaves expense input with feature in progress`() {
        val result = handle(UserCommand.ViewExpenses)

        assertThat(
            result.outgoingMessages
                .single()
                .text,
        ).isEqualTo(BotText.FeatureInProgress)
        assertThat(
            result.outgoingMessages
                .single()
                .actions,
        ).isEmpty()
        assertThat(result.nextState).isEqualTo(UserState.Idle)
        confirmVerified(expenseService, categoryService)
    }

    @Test
    fun `unsupported command repeats input instructions without parsing`() {
        val result = handle(UserCommand.Unsupported)

        assertThat(
            result.outgoingMessages
                .single()
                .text,
        ).isEqualTo(BotText.AddExpenseInstructions)
        assertThat(
            result.outgoingMessages
                .single()
                .actions,
        ).isEmpty()
        assertThat(result.nextState).isEqualTo(UserState.AwaitingExpenseInput)
        confirmVerified(expenseService, categoryService)
    }

    private fun handle(command: UserCommand) =
        handler.handle(
            input =
                makeUserInput(
                    userId = USER_ID,
                    chatId = CHAT_ID,
                    text = EXPENSE_TEXT,
                    command = command,
                ),
            currentState = UserState.AwaitingExpenseInput,
        )

    private fun category(id: UUID): Category =
        Category(
            id = id,
            name = "Еда",
            userId = USER_ID,
        )

    private companion object {
        const val USER_ID = 123L
        const val CHAT_ID = 456L
        const val EXPENSE_TEXT = "500 такси"
        const val EXPENSE_DESCRIPTION = "такси"
        val EXPENSE_AMOUNT: Money = Money.of(BigDecimal("500.00"))
        val EXPENSE_DRAFT: ExpenseDraft = ExpenseDraft(EXPENSE_AMOUNT, EXPENSE_DESCRIPTION)
        val FIRST_CATEGORY_ID: UUID = UUID.fromString("00000000-0000-0000-0000-000000000001")
        val SECOND_CATEGORY_ID: UUID = UUID.fromString("00000000-0000-0000-0000-000000000002")
    }
}
