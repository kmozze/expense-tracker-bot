package me.kmozze.expensetracker.unit.handler.statehandler

import io.mockk.confirmVerified
import io.mockk.every
import io.mockk.junit5.MockKExtension
import io.mockk.mockk
import io.mockk.verify
import me.kmozze.expensetracker.handler.statehandler.buildUpdatedExpenseResult
import me.kmozze.expensetracker.handler.statehandler.expenseEditUnavailableResult
import me.kmozze.expensetracker.model.domain.BotAction
import me.kmozze.expensetracker.model.domain.BotText
import me.kmozze.expensetracker.model.domain.Money
import me.kmozze.expensetracker.model.domain.UserState
import me.kmozze.expensetracker.model.entity.Category
import me.kmozze.expensetracker.model.entity.Expense
import me.kmozze.expensetracker.service.CategoryService
import me.kmozze.expensetracker.service.ExpenseService
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import java.math.BigDecimal
import java.time.LocalDate
import java.util.UUID

@ExtendWith(MockKExtension::class)
class ExpenseEditStateHelpersTest {
    private val expenseService: ExpenseService = mockk()
    private val categoryService: CategoryService = mockk()

    @Test
    fun `build updated expense result contains done and card with actions`() {
        every { expenseService.findExpenseForUser(USER_ID, EXPENSE_ID) } returns EXPENSE
        every { categoryService.findCategoryForUser(CATEGORY_ID, USER_ID) } returns CATEGORY

        val result =
            buildUpdatedExpenseResult(
                expenseService = expenseService,
                categoryService = categoryService,
                userId = USER_ID,
                expenseId = EXPENSE_ID,
                nextState = UserState.Idle,
                prefixText = BotText.Done,
            )

        assertThat(result.outgoingMessages).hasSize(2)
        assertThat(result.outgoingMessages[0].text).isEqualTo(BotText.Done)
        assertThat(result.outgoingMessages[0].actions).containsExactly(BotAction.RemoveReplyKeyboard)
        assertThat(result.outgoingMessages[1].text)
            .isEqualTo(
                BotText.ExpenseView(
                    amount = EXPENSE.amount,
                    categoryName = CATEGORY.name,
                    expenseDate = EXPENSE.expenseDate,
                    description = EXPENSE.description,
                ),
            )
        assertThat(result.outgoingMessages[1].actions)
            .containsExactly(BotAction.ShowExpenseCardActions(EXPENSE_ID))
        assertThat(result.nextState).isEqualTo(UserState.Idle)

        verify(exactly = 1) { expenseService.findExpenseForUser(USER_ID, EXPENSE_ID) }
        verify(exactly = 1) { categoryService.findCategoryForUser(CATEGORY_ID, USER_ID) }
        confirmVerified(expenseService, categoryService)
    }

    @Test
    fun `expense edit unavailable removes inline keyboard and reply keyboard`() {
        val result = expenseEditUnavailableResult()
        val outgoingMessage = result.outgoingMessages.single()

        assertThat(outgoingMessage.text)
            .isEqualTo(BotText.ExpenseUnavailable)
        assertThat(outgoingMessage.actions)
            .containsExactly(BotAction.ClearInlineKeyboard, BotAction.RemoveReplyKeyboard)
        assertThat(result.nextState).isEqualTo(UserState.Idle)
    }

    private companion object {
        const val USER_ID = 123L
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
        val EXPENSE =
            Expense(
                id = EXPENSE_ID,
                categoryId = CATEGORY_ID,
                amount = EXPENSE_AMOUNT,
                userId = USER_ID,
                expenseDate = EXPENSE_DATE,
                description = "такси",
            )
    }
}
