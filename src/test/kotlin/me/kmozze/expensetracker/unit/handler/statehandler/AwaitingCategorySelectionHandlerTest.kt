package me.kmozze.expensetracker.unit.handler.statehandler

import io.mockk.confirmVerified
import io.mockk.every
import io.mockk.junit5.MockKExtension
import io.mockk.mockk
import io.mockk.verify
import me.kmozze.expensetracker.exception.BusinessErrorCode
import me.kmozze.expensetracker.handler.statehandler.AwaitingCategorySelectionHandler
import me.kmozze.expensetracker.model.domain.BotAction
import me.kmozze.expensetracker.model.domain.BotText
import me.kmozze.expensetracker.model.domain.ExpenseDraft
import me.kmozze.expensetracker.model.domain.Money
import me.kmozze.expensetracker.model.domain.UserCommand
import me.kmozze.expensetracker.model.domain.UserState
import me.kmozze.expensetracker.model.entity.Category
import me.kmozze.expensetracker.service.CategoryService
import me.kmozze.expensetracker.support.makeUserInput
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import java.math.BigDecimal
import java.util.UUID

@ExtendWith(MockKExtension::class)
class AwaitingCategorySelectionHandlerTest {
    private val categoryService: CategoryService = mockk()
    private lateinit var handler: AwaitingCategorySelectionHandler

    @BeforeEach
    fun setUp() {
        handler =
            AwaitingCategorySelectionHandler(
                categoryService = categoryService,
            )
    }

    @Test
    fun `selected category opens expense date selection`() {
        val category = category(id = CATEGORY_ID, name = CATEGORY_NAME)
        every { categoryService.getCategories(USER_ID) } returns listOf(category)

        val result = handle(UserCommand.PlainText(CATEGORY_NAME))

        assertThat(
            result.response.outgoingMessages
                .single()
                .text,
        ).isEqualTo(
            BotText.SelectExpenseDate(
                amount = EXPENSE_AMOUNT,
                categoryName = category.name,
                description = EXPENSE_DESCRIPTION,
            ),
        )
        assertThat(
            result.response.outgoingMessages
                .single()
                .actions,
        ).containsExactly(BotAction.ShowExpenseDateSelection)
        assertThat(result.nextState)
            .isEqualTo(
                UserState.AwaitingExpenseDateSelection(
                    expenseDraft = EXPENSE_DRAFT.copy(categoryId = CATEGORY_ID),
                    categoryName = category.name,
                ),
            )
        verify(exactly = 1) { categoryService.getCategories(USER_ID) }
        confirmVerified(categoryService)
    }

    @Test
    fun `missing category keeps category selection open with error`() {
        val categories = listOf(category(id = CATEGORY_ID, name = CATEGORY_NAME))
        every { categoryService.getCategories(USER_ID) } returns categories

        val result = handle(UserCommand.PlainText("Такси"))

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
        assertThat(result.nextState).isEqualTo(AWAITING_CATEGORY_SELECTION)
        verify(exactly = 1) { categoryService.getCategories(USER_ID) }
        confirmVerified(categoryService)
    }

    @Test
    fun `cancel returns idle without service calls`() {
        val result = handle(UserCommand.Cancel)

        assertThat(
            result.response.outgoingMessages
                .single()
                .text,
        ).isEqualTo(BotText.ExpenseCanceled)
        assertThat(
            result.response.outgoingMessages
                .single()
                .actions,
        ).containsExactly(BotAction.RemoveReplyKeyboard)
        assertThat(result.nextState).isEqualTo(UserState.Idle)
        confirmVerified(categoryService)
    }

    @Test
    fun `non-selection command repeats category selection`() {
        val categories = listOf(category(id = CATEGORY_ID), category(id = SECOND_CATEGORY_ID))
        every { categoryService.getCategories(USER_ID) } returns categories

        val result = handle(UserCommand.AddExpense)

        assertThat(
            result.response.outgoingMessages
                .single()
                .text,
        ).isEqualTo(BotText.SelectCategory(EXPENSE_AMOUNT, EXPENSE_DESCRIPTION))
        assertThat(
            result.response.outgoingMessages
                .single()
                .actions,
        ).containsExactly(BotAction.ShowCategorySelection(categories.map { it.name }))
        assertThat(result.nextState).isEqualTo(AWAITING_CATEGORY_SELECTION)
        verify(exactly = 1) { categoryService.getCategories(USER_ID) }
        confirmVerified(categoryService)
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
            currentState = AWAITING_CATEGORY_SELECTION,
        )

    private fun textFor(command: UserCommand): String? =
        when (command) {
            is UserCommand.PlainText -> command.value
            else -> null
        }

    private fun category(
        id: UUID,
        name: String = "Еда",
    ): Category =
        Category(
            id = id,
            name = name,
            userId = USER_ID,
        )

    private companion object {
        const val USER_ID = 123L
        const val CHAT_ID = 456L
        const val CATEGORY_NAME = "Транспорт"
        const val EXPENSE_DESCRIPTION = "такси"
        val EXPENSE_AMOUNT: Money = Money.of(BigDecimal("500.00"))
        val EXPENSE_DRAFT: ExpenseDraft = ExpenseDraft(EXPENSE_AMOUNT, EXPENSE_DESCRIPTION)
        val AWAITING_CATEGORY_SELECTION: UserState.AwaitingCategorySelection =
            UserState.AwaitingCategorySelection(EXPENSE_DRAFT)
        val CATEGORY_ID: UUID = UUID.fromString("00000000-0000-0000-0000-000000000001")
        val SECOND_CATEGORY_ID: UUID = UUID.fromString("00000000-0000-0000-0000-000000000002")
    }
}
