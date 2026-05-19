package me.kmozze.expensetracker.integration.handler

import me.kmozze.expensetracker.adapter.callback.CallbackData
import me.kmozze.expensetracker.adapter.input.UserCommandParser
import me.kmozze.expensetracker.adapter.ui.Buttons
import me.kmozze.expensetracker.handler.DialogueRouter
import me.kmozze.expensetracker.integration.AbstractIntegrationTest
import me.kmozze.expensetracker.model.domain.BotAction
import me.kmozze.expensetracker.model.domain.BotMessage
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

        dialogueRouter.process(userInput(userId = userId, chatId = chatId, text = "/start"))

        val addExpenseResult =
            dialogueRouter.process(
                userInput(
                    userId = userId,
                    chatId = chatId,
                    text = Buttons.ADD_EXPENSE,
                ),
            )

        assertThat(addExpenseResult.response.message).isEqualTo(BotMessage.AddExpenseInstructions)
        assertThat(addExpenseResult.nextState).isEqualTo(UserState.AwaitingExpenseInput)

        val parsedExpenseResult =
            dialogueRouter.process(
                userInput(
                    userId = userId,
                    chatId = chatId,
                    text = "500 такси",
                ),
            )
        val categorySelectionAction = parsedExpenseResult.response.actions.single() as BotAction.ShowCategorySelection
        val category = categorySelectionAction.categories.first()

        assertThat(parsedExpenseResult.response.message)
            .isEqualTo(BotMessage.SelectCategory(BigDecimal("500"), "такси"))
        assertThat(parsedExpenseResult.nextState)
            .isEqualTo(UserState.AwaitingCategorySelection(ParsedExpense(BigDecimal("500"), "такси")))

        val savedExpenseResult =
            dialogueRouter.process(
                userInput(
                    userId = userId,
                    chatId = chatId,
                    callbackData = CallbackData.selectCategory(category.id),
                ),
            )

        assertThat(savedExpenseResult.response.message)
            .isEqualTo(BotMessage.ExpenseSaved(BigDecimal("500.00"), category.name, "такси"))
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

    private fun userInput(
        userId: Long,
        chatId: Long,
        text: String? = null,
        callbackData: String? = null,
    ): UserInput =
        UserInput(
            userId = userId,
            chatId = chatId,
            text = text,
            callbackData = callbackData,
            command = UserCommandParser.parse(text = text, callbackData = callbackData),
        )
}
