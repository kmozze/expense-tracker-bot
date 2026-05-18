package me.kmozze.expensetracker.integration.handler

import me.kmozze.expensetracker.adapter.ui.Buttons
import me.kmozze.expensetracker.handler.DialogueRouter
import me.kmozze.expensetracker.integration.AbstractIntegrationTest
import me.kmozze.expensetracker.model.domain.Action
import me.kmozze.expensetracker.model.domain.Message
import me.kmozze.expensetracker.model.domain.ParsedExpense
import me.kmozze.expensetracker.model.domain.UserInput
import me.kmozze.expensetracker.model.domain.UserState
import me.kmozze.expensetracker.repository.IExpenseRepository
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.time.OffsetDateTime

@Transactional
class AddExpenseFlowTest : AbstractIntegrationTest() {
    @Autowired
    private lateinit var dialogueRouter: DialogueRouter

    @Autowired
    private lateinit var expenseRepository: IExpenseRepository

    @Test
    fun `add expense flow saves expense after category selection`() {
        val userId = 2001L
        val chatId = 3001L
        val from = OffsetDateTime.now().minusMinutes(5)

        dialogueRouter.process(UserInput(userId = userId, chatId = chatId, text = "/start"))

        val addExpenseResult =
            dialogueRouter.process(
                UserInput(
                    userId = userId,
                    chatId = chatId,
                    text = Buttons.ADD_EXPENSE,
                ),
            )

        assertThat(addExpenseResult.response.message).isEqualTo(Message.AddExpenseInstructions)
        assertThat(addExpenseResult.nextState).isEqualTo(UserState.AwaitingExpenseInput)

        val parsedExpenseResult =
            dialogueRouter.process(
                UserInput(
                    userId = userId,
                    chatId = chatId,
                    text = "500 такси",
                ),
            )
        val categorySelectionAction = parsedExpenseResult.response.actions.single() as Action.ShowCategorySelection
        val category = categorySelectionAction.categories.first()

        assertThat(parsedExpenseResult.response.message)
            .isEqualTo(Message.SelectCategory(BigDecimal("500"), "такси"))
        assertThat(parsedExpenseResult.nextState)
            .isEqualTo(UserState.AwaitingCategorySelection(ParsedExpense(BigDecimal("500"), "такси")))

        val savedExpenseResult =
            dialogueRouter.process(
                UserInput(
                    userId = userId,
                    chatId = chatId,
                    callbackData = "select_category:${category.id}",
                ),
            )

        assertThat(savedExpenseResult.response.message)
            .isEqualTo(Message.ExpenseSaved(BigDecimal("500.00"), category.name, "такси"))
        assertThat(savedExpenseResult.nextState).isEqualTo(UserState.Idle)

        val expenses =
            expenseRepository.findAllByUserIdAndPeriod(
                userId = userId,
                from = from,
                to = OffsetDateTime.now().plusMinutes(5),
            )

        assertThat(expenses).hasSize(1)
        assertThat(expenses.single().amount).isEqualByComparingTo(BigDecimal("500"))
        assertThat(expenses.single().categoryId).isEqualTo(category.id)
        assertThat(expenses.single().description).isEqualTo("такси")
    }
}
