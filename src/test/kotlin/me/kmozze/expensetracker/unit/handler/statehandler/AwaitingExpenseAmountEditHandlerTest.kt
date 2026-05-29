package me.kmozze.expensetracker.unit.handler.statehandler

import io.mockk.confirmVerified
import io.mockk.every
import io.mockk.junit5.MockKExtension
import io.mockk.mockk
import io.mockk.verify
import me.kmozze.expensetracker.exception.BusinessErrorCode
import me.kmozze.expensetracker.exception.exception
import me.kmozze.expensetracker.handler.statehandler.AwaitingExpenseAmountEditHandler
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
class AwaitingExpenseAmountEditHandlerTest {
    private val expenseService: ExpenseService = mockk()
    private val categoryService: CategoryService = mockk()
    private lateinit var handler: AwaitingExpenseAmountEditHandler

    @BeforeEach
    fun setUp() {
        handler =
            AwaitingExpenseAmountEditHandler(
                expenseService = expenseService,
                categoryService = categoryService,
            )
    }

    @Test
    fun `valid amount updates expense and returns idle`() {
        val updatedAmount = Money.of(BigDecimal("650.00"))
        every { expenseService.parseExpenseAmount("650") } returns updatedAmount
        every { expenseService.updateExpenseAmountForUser(USER_ID, EXPENSE_ID, updatedAmount) } returns expense(amount = updatedAmount)
        every { expenseService.findExpenseForUser(USER_ID, EXPENSE_ID) } returns expense(amount = updatedAmount)
        every { categoryService.findCategoryForUser(CATEGORY_ID, USER_ID) } returns CATEGORY

        val result = handle(UserCommand.PlainText("650"))

        assertThat(result.response.outgoingMessages).hasSize(2)
        assertThat(result.response.outgoingMessages[0].text).isEqualTo(BotText.Done)
        assertThat(result.response.outgoingMessages[0].actions).containsExactly(BotAction.RemoveReplyKeyboard)
        assertThat(result.response.outgoingMessages[1].text)
            .isEqualTo(
                BotText.ExpenseSaved(
                    amount = updatedAmount,
                    categoryName = CATEGORY.name,
                    expenseDate = EXPENSE_DATE,
                    description = EXPENSE_DESCRIPTION,
                ),
            )
        assertThat(result.response.outgoingMessages[1].actions).containsExactly(BotAction.ShowExpenseCardActions(EXPENSE_ID))
        assertThat(result.nextState).isEqualTo(UserState.Idle)
        verify(exactly = 1) { expenseService.parseExpenseAmount("650") }
        verify(exactly = 1) { expenseService.updateExpenseAmountForUser(USER_ID, EXPENSE_ID, updatedAmount) }
        verify(exactly = 1) { expenseService.findExpenseForUser(USER_ID, EXPENSE_ID) }
        verify(exactly = 1) { categoryService.findCategoryForUser(CATEGORY_ID, USER_ID) }
        confirmVerified(expenseService, categoryService)
    }

    @Test
    fun `invalid amount keeps amount prompt with error text`() {
        every { expenseService.parseExpenseAmount("abc") } throws BusinessErrorCode.INVALID_EXPENSE_AMOUNT.exception()

        val result = handle(UserCommand.PlainText("abc"))

        assertThat(
            result.response.outgoingMessages
                .single()
                .text,
        ).isEqualTo(BotText.Error(BusinessErrorCode.INVALID_EXPENSE_AMOUNT))
        assertThat(
            result.response.outgoingMessages
                .single()
                .actions,
        ).containsExactly(BotAction.ShowCancel)
        assertThat(result.nextState).isEqualTo(AWAITING_EXPENSE_AMOUNT_EDIT)
        verify(exactly = 1) { expenseService.parseExpenseAmount("abc") }
        confirmVerified(expenseService, categoryService)
    }

    @Test
    fun `cancel returns updated expense card and finishes dialog`() {
        every { expenseService.findExpenseForUser(USER_ID, EXPENSE_ID) } returns expense()
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
                    description = EXPENSE_DESCRIPTION,
                ),
            )
        assertThat(result.response.outgoingMessages[1].actions).containsExactly(BotAction.ShowExpenseCardActions(EXPENSE_ID))
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
            currentState = AWAITING_EXPENSE_AMOUNT_EDIT,
        )

    private fun textFor(command: UserCommand): String? =
        when (command) {
            is UserCommand.PlainText -> command.value
            else -> null
        }

    private fun expense(amount: Money = EXPENSE_AMOUNT): Expense =
        Expense(
            id = EXPENSE_ID,
            categoryId = CATEGORY_ID,
            amount = amount,
            userId = USER_ID,
            expenseDate = EXPENSE_DATE,
            description = EXPENSE_DESCRIPTION,
        )

    private companion object {
        const val USER_ID = 123L
        const val CHAT_ID = 456L
        val CATEGORY_ID: UUID = UUID.fromString("00000000-0000-0000-0000-000000000002")
        val EXPENSE_ID: UUID = UUID.fromString("00000000-0000-0000-0000-000000000001")
        val EXPENSE_AMOUNT: Money = Money.of(BigDecimal("500.00"))
        val EXPENSE_DATE: LocalDate = LocalDate.parse("2026-05-24")
        const val EXPENSE_DESCRIPTION = "такси"
        val CATEGORY: Category =
            Category(
                id = CATEGORY_ID,
                name = "Транспорт",
                userId = USER_ID,
            )
        val AWAITING_EXPENSE_AMOUNT_EDIT: UserState.AwaitingExpenseAmountEdit =
            UserState.AwaitingExpenseAmountEdit(
                expenseId = EXPENSE_ID,
            )
    }
}
