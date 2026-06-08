package me.kmozze.expensetracker.unit.handler.statehandler

import io.mockk.confirmVerified
import io.mockk.every
import io.mockk.junit5.MockKExtension
import io.mockk.mockk
import io.mockk.verify
import me.kmozze.expensetracker.handler.statehandler.AwaitingExpenseDescriptionEditHandler
import me.kmozze.expensetracker.model.domain.BotAction
import me.kmozze.expensetracker.model.domain.BotText
import me.kmozze.expensetracker.model.domain.ExpenseDraft
import me.kmozze.expensetracker.model.domain.ExpenseDraftCategory
import me.kmozze.expensetracker.model.domain.ExpenseEditSession
import me.kmozze.expensetracker.model.domain.HandlerResponse
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
class AwaitingExpenseDescriptionEditHandlerTest {
    private val expenseService: ExpenseService = mockk()
    private val categoryService: CategoryService = mockk()
    private lateinit var handler: AwaitingExpenseDescriptionEditHandler

    @BeforeEach
    fun setUp() {
        handler =
            AwaitingExpenseDescriptionEditHandler(
                expenseService = expenseService,
                categoryService = categoryService,
            )
    }

    @Test
    fun `description text updates draft and returns to field selection`() {
        val updatedDraft = EXPENSE_DRAFT.copy(description = NEW_EXPENSE_DESCRIPTION)

        val result = handle(UserCommand.PlainText(NEW_EXPENSE_DESCRIPTION))

        assertThat(result.outgoingMessages).hasSize(2)
        assertThat(result.outgoingMessages[0].text)
            .isEqualTo(
                BotText.ExpenseView(
                    amount = EXPENSE_AMOUNT,
                    categoryName = CATEGORY.name,
                    expenseDate = EXPENSE_DATE,
                    description = NEW_EXPENSE_DESCRIPTION,
                ),
            )
        assertThat(result.outgoingMessages[0].actions).isEmpty()
        assertThat(result.outgoingMessages[1].text).isEqualTo(BotText.EditExpenseFieldSelection)
        assertThat(result.outgoingMessages[1].actions).containsExactly(BotAction.ShowExpenseEditFieldSelection)
        assertThat(result.nextState)
            .isEqualTo(
                UserState.AwaitingExpenseEditFieldSelection(
                    editSession = ExpenseEditSession(expenseId = EXPENSE_ID, expenseDraft = updatedDraft),
                ),
            )
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

    @Test
    fun `non-description command repeats description input`() {
        val result = handle(UserCommand.Categories)

        assertThat(result.outgoingMessages.single().text).isEqualTo(BotText.EnterExpenseDescription)
        assertThat(result.outgoingMessages.single().actions).containsExactly(BotAction.ShowCancel)
        assertThat(result.nextState).isEqualTo(AWAITING_EXPENSE_DESCRIPTION_EDIT)
        confirmVerified(expenseService, categoryService)
    }

    @Test
    fun `whitespace description repeats description input`() {
        val result = handle(UserCommand.PlainText("   "))

        assertThat(result.outgoingMessages.single().text).isEqualTo(BotText.EnterExpenseDescription)
        assertThat(result.outgoingMessages.single().actions).containsExactly(BotAction.ShowCancel)
        assertThat(result.nextState).isEqualTo(AWAITING_EXPENSE_DESCRIPTION_EDIT)
        confirmVerified(expenseService, categoryService)
    }

    private fun handle(command: UserCommand): HandlerResponse =
        handler.handle(
            input =
                makeUserInput(
                    userId = USER_ID,
                    chatId = CHAT_ID,
                    text = textFor(command),
                    command = command,
                ),
            currentState = AWAITING_EXPENSE_DESCRIPTION_EDIT,
        )

    private fun textFor(command: UserCommand): String? =
        when (command) {
            is UserCommand.PlainText -> command.value
            else -> null
        }

    private companion object {
        const val USER_ID = 123L
        const val CHAT_ID = 456L
        const val EXPENSE_DESCRIPTION = "такси"
        const val NEW_EXPENSE_DESCRIPTION = "автобус"
        val CATEGORY_ID: UUID = UUID.fromString("00000000-0000-0000-0000-000000000002")
        val EXPENSE_ID: UUID = UUID.fromString("00000000-0000-0000-0000-000000000001")
        val EXPENSE_AMOUNT: Money = Money.of(BigDecimal("500.00"))
        val EXPENSE_DATE: LocalDate = LocalDate.parse("2026-05-24")
        val CATEGORY =
            Category(
                id = CATEGORY_ID,
                name = "Транспорт",
                userId = USER_ID,
            )
        val EXPENSE_DRAFT =
            ExpenseDraft(
                amount = EXPENSE_AMOUNT,
                description = EXPENSE_DESCRIPTION,
                category = ExpenseDraftCategory(categoryId = CATEGORY_ID, name = CATEGORY.name),
                expenseDate = EXPENSE_DATE,
            )
        val EDIT_SESSION =
            ExpenseEditSession(
                expenseId = EXPENSE_ID,
                expenseDraft = EXPENSE_DRAFT,
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
        val EXPENSE_VIEW =
            BotText.ExpenseView(
                amount = EXPENSE_AMOUNT,
                categoryName = CATEGORY.name,
                expenseDate = EXPENSE_DATE,
                description = EXPENSE_DESCRIPTION,
            )
        val AWAITING_EXPENSE_DESCRIPTION_EDIT =
            UserState.AwaitingExpenseDescriptionEdit(
                editSession = EDIT_SESSION,
            )
    }
}
