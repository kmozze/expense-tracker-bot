package me.kmozze.expensetracker.unit.handler.statehandler

import io.mockk.confirmVerified
import io.mockk.every
import io.mockk.junit5.MockKExtension
import io.mockk.mockk
import io.mockk.verify
import me.kmozze.expensetracker.exception.BusinessErrorCode
import me.kmozze.expensetracker.handler.statehandler.AwaitingExpenseDateEditSelectionHandler
import me.kmozze.expensetracker.model.domain.BotAction
import me.kmozze.expensetracker.model.domain.BotText
import me.kmozze.expensetracker.model.domain.ExpenseDateChoice
import me.kmozze.expensetracker.model.domain.ExpenseDraft
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
    fun `today selection updates draft date`() {
        val result = handle(UserCommand.SelectExpenseDate(ExpenseDateChoice.Today))

        assertDraftDateSelectionResult(result, TODAY)
        confirmVerified(expenseService, categoryService)
    }

    @Test
    fun `yesterday selection updates draft date`() {
        val result = handle(UserCommand.SelectExpenseDate(ExpenseDateChoice.Yesterday))

        assertDraftDateSelectionResult(result, YESTERDAY)
        confirmVerified(expenseService, categoryService)
    }

    @Test
    fun `manual selection asks for manual date input with draft card`() {
        val result = handle(UserCommand.SelectExpenseDate(ExpenseDateChoice.ManualInput))

        val outgoingMessages = result.outgoingMessages
        assertThat(outgoingMessages).hasSize(2)
        assertThat(outgoingMessages[0].text).isEqualTo(EXPENSE_VIEW)
        assertThat(outgoingMessages[0].actions).isEmpty()
        assertThat(outgoingMessages[1].text).isEqualTo(BotText.EnterExpenseDateManually)
        assertThat(outgoingMessages[1].actions).containsExactly(BotAction.ShowCancel)
        assertThat(result.nextState)
            .isEqualTo(
                UserState.AwaitingExpenseDateEditManualInput(
                    expenseId = EXPENSE_ID,
                    expenseDraft = EXPENSE_DRAFT,
                    categoryName = CATEGORY.name,
                ),
            )
        confirmVerified(expenseService, categoryService)
    }

    @Test
    fun `invalid date text repeats selection`() {
        val result = handle(UserCommand.PlainText("завтра"))

        assertThat(result.outgoingMessages.single().text)
            .isEqualTo(BotText.Error(BusinessErrorCode.INVALID_EXPENSE_DATE_SELECTION))
        assertThat(result.outgoingMessages.single().actions).containsExactly(BotAction.ShowExpenseDateSelection)
        assertThat(result.nextState).isEqualTo(AWAITING_EXPENSE_DATE_EDIT_SELECTION)
        confirmVerified(expenseService, categoryService)
    }

    @Test
    fun `cancel returns saved expense card`() {
        every { expenseService.findExpenseForUser(USER_ID, EXPENSE_ID) } returns EXPENSE
        every { categoryService.findCategoryForUser(CATEGORY_ID, USER_ID) } returns CATEGORY

        val result = handle(UserCommand.Cancel)

        assertThat(result.outgoingMessages[0].text).isEqualTo(BotText.Done)
        assertThat(result.nextState).isEqualTo(UserState.Idle)
        verify(exactly = 1) { expenseService.findExpenseForUser(USER_ID, EXPENSE_ID) }
        verify(exactly = 1) { categoryService.findCategoryForUser(CATEGORY_ID, USER_ID) }
        confirmVerified(expenseService, categoryService)
    }

    private fun assertDraftDateSelectionResult(
        result: me.kmozze.expensetracker.model.domain.HandlerResponse,
        expectedDate: LocalDate,
    ) {
        val updatedDraft = EXPENSE_DRAFT.copy(expenseDate = expectedDate)

        assertThat(result.outgoingMessages).hasSize(2)
        assertThat(result.outgoingMessages[0].text)
            .isEqualTo(
                BotText.ExpenseView(
                    amount = EXPENSE_AMOUNT,
                    categoryName = CATEGORY.name,
                    expenseDate = expectedDate,
                    description = EXPENSE.description,
                ),
            )
        assertThat(result.outgoingMessages[0].actions).isEmpty()
        assertThat(result.outgoingMessages[1].text).isEqualTo(BotText.EditExpenseFieldSelection)
        assertThat(result.outgoingMessages[1].actions).containsExactly(BotAction.ShowExpenseEditFieldSelection)
        assertThat(result.nextState)
            .isEqualTo(
                UserState.AwaitingExpenseEditFieldSelection(
                    expenseId = EXPENSE_ID,
                    expenseDraft = updatedDraft,
                    categoryName = CATEGORY.name,
                ),
            )
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
        val EXPENSE_DRAFT =
            ExpenseDraft(
                amount = EXPENSE_AMOUNT,
                description = EXPENSE.description,
                categoryId = CATEGORY_ID,
                expenseDate = TODAY,
            )
        val EXPENSE_VIEW: BotText.ExpenseView =
            BotText.ExpenseView(
                amount = EXPENSE_AMOUNT,
                categoryName = CATEGORY.name,
                expenseDate = TODAY,
                description = EXPENSE.description,
            )
        val AWAITING_EXPENSE_DATE_EDIT_SELECTION: UserState.AwaitingExpenseDateEditSelection =
            UserState.AwaitingExpenseDateEditSelection(
                expenseId = EXPENSE_ID,
                expenseDraft = EXPENSE_DRAFT,
                categoryName = CATEGORY.name,
            )
    }
}
