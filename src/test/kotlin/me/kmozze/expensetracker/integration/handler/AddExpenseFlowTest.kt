package me.kmozze.expensetracker.integration.handler

import me.kmozze.expensetracker.adapter.callback.CallbackData
import me.kmozze.expensetracker.adapter.ui.Buttons
import me.kmozze.expensetracker.exception.BusinessErrorCode
import me.kmozze.expensetracker.handler.DialogueRouter
import me.kmozze.expensetracker.integration.AbstractIntegrationTest
import me.kmozze.expensetracker.model.domain.BotAction
import me.kmozze.expensetracker.model.domain.BotMessage
import me.kmozze.expensetracker.model.domain.Money
import me.kmozze.expensetracker.model.domain.ParsedExpense
import me.kmozze.expensetracker.model.domain.UserCommand
import me.kmozze.expensetracker.model.domain.UserState
import me.kmozze.expensetracker.model.entity.Category
import me.kmozze.expensetracker.model.entity.Expense
import me.kmozze.expensetracker.repository.ICategoryRepository
import me.kmozze.expensetracker.repository.IExpenseRepository
import me.kmozze.expensetracker.support.processUserInput
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.time.OffsetDateTime
import java.util.UUID

@Transactional
class AddExpenseFlowTest : AbstractIntegrationTest() {
    @Autowired
    private lateinit var dialogueRouter: DialogueRouter

    @Autowired
    private lateinit var categoryRepository: ICategoryRepository

    @Autowired
    private lateinit var expenseRepository: IExpenseRepository

    @Test
    fun `add expense flow saves expense after category selection`() {
        val userId = 2001L
        val chatId = 3001L

        dialogueRouter.processUserInput(userId = userId, chatId = chatId, text = "/start")

        val addExpenseResult =
            dialogueRouter.processUserInput(
                userId = userId,
                chatId = chatId,
                text = Buttons.ADD_EXPENSE,
            )

        assertThat(addExpenseResult.response.message).isEqualTo(BotMessage.AddExpenseInstructions)
        assertThat(addExpenseResult.nextState).isEqualTo(UserState.AwaitingExpenseInput)

        val parsedExpenseResult =
            dialogueRouter.processUserInput(
                userId = userId,
                chatId = chatId,
                text = "500 такси",
            )
        val categorySelectionAction = parsedExpenseResult.response.actions.single() as BotAction.ShowCategorySelection

        assertThat(categorySelectionAction.categories).isNotEmpty()
        val category = categorySelectionAction.categories.first()

        assertThat(parsedExpenseResult.response.message)
            .isEqualTo(BotMessage.SelectCategory(Money.of(BigDecimal("500.00")), "такси"))
        assertThat(parsedExpenseResult.nextState)
            .isEqualTo(UserState.AwaitingCategorySelection(ParsedExpense(Money.of(BigDecimal("500.00")), "такси")))

        val savedExpenseResult =
            dialogueRouter.processUserInput(
                userId = userId,
                chatId = chatId,
                callbackData = CallbackData.selectCategory(category.id),
            )

        assertThat(savedExpenseResult.response.message)
            .isEqualTo(BotMessage.ExpenseSaved(Money.of(BigDecimal("500.00")), category.name, "такси"))
        assertThat(savedExpenseResult.nextState).isEqualTo(UserState.Idle)

        val expenses = findExpenses(userId)

        assertThat(expenses).hasSize(1)
        assertThat(expenses.single().amount).isEqualTo(Money.of(BigDecimal("500.00")))
        assertThat(expenses.single().categoryId).isEqualTo(category.id)
        assertThat(expenses.single().description).isEqualTo("такси")
    }

    @Test
    fun `add expense flow saves expense without description`() {
        val userId = 2008L
        val chatId = 3008L

        dialogueRouter.processUserInput(userId = userId, chatId = chatId, text = "/start")
        dialogueRouter.processUserInput(userId = userId, chatId = chatId, text = Buttons.ADD_EXPENSE)

        val parsedExpenseResult =
            dialogueRouter.processUserInput(
                userId = userId,
                chatId = chatId,
                text = "500",
            )
        val categorySelectionAction = parsedExpenseResult.response.actions.single() as BotAction.ShowCategorySelection

        assertThat(categorySelectionAction.categories).isNotEmpty()
        val category = categorySelectionAction.categories.first()

        assertThat(parsedExpenseResult.response.message)
            .isEqualTo(BotMessage.SelectCategory(Money.of(BigDecimal("500.00")), null))
        assertThat(parsedExpenseResult.nextState)
            .isEqualTo(UserState.AwaitingCategorySelection(ParsedExpense(Money.of(BigDecimal("500.00")), null)))

        val savedExpenseResult =
            dialogueRouter.processUserInput(
                userId = userId,
                chatId = chatId,
                callbackData = CallbackData.selectCategory(category.id),
            )

        assertThat(savedExpenseResult.response.message)
            .isEqualTo(BotMessage.ExpenseSaved(Money.of(BigDecimal("500.00")), category.name, null))
        assertThat(savedExpenseResult.nextState).isEqualTo(UserState.Idle)

        val expenses = findExpenses(userId)
        assertThat(expenses).hasSize(1)
        assertThat(expenses.single().description).isNull()
    }

    @Test
    fun `cancel category selection returns to idle without saving expense`() {
        val userId = 2002L
        val chatId = 3002L

        startCategorySelection(userId, chatId)

        val result =
            dialogueRouter.processUserInput(
                userId = userId,
                chatId = chatId,
                callbackData = CallbackData.cancel(),
            )

        assertThat(result.response.message).isEqualTo(BotMessage.ExpenseCanceled)
        assertThat(result.nextState).isEqualTo(UserState.Idle)
        assertThat(findExpenses(userId)).isEmpty()
    }

    @Test
    fun `invalid category selection command keeps category selection open without saving expense`() {
        val userId = 2003L
        val chatId = 3003L

        val parsedExpense = startCategorySelection(userId, chatId)

        val result =
            dialogueRouter.processUserInput(
                userId = userId,
                chatId = chatId,
                command = UserCommand.InvalidCategorySelection,
            )

        assertThat(result.response.message).isEqualTo(BotMessage.Error(BusinessErrorCode.INVALID_CATEGORY_SELECTION))
        assertThat(result.response.actions.single()).isInstanceOf(BotAction.ShowCategorySelection::class.java)
        assertThat(result.nextState).isEqualTo(UserState.AwaitingCategorySelection(parsedExpense))
        assertThat(findExpenses(userId)).isEmpty()
    }

    @Test
    fun `foreign category callback keeps category selection open without saving expense`() {
        val userId = 2004L
        val chatId = 3004L
        val parsedExpense = startCategorySelection(userId, chatId)
        val foreignCategory =
            categoryRepository.create(
                Category(
                    id = UUID.randomUUID(),
                    name = "Чужая",
                    userId = 9999L,
                ),
            )

        val result =
            dialogueRouter.processUserInput(
                userId = userId,
                chatId = chatId,
                callbackData = CallbackData.selectCategory(foreignCategory.id),
            )

        assertThat(result.response.message).isEqualTo(BotMessage.Error(BusinessErrorCode.CATEGORY_NOT_FOUND))
        assertThat(result.response.actions.single()).isInstanceOf(BotAction.ShowCategorySelection::class.java)
        assertThat(result.nextState).isEqualTo(UserState.AwaitingCategorySelection(parsedExpense))
        assertThat(findExpenses(userId)).isEmpty()
    }

    @Test
    fun `parse error keeps awaiting expense input without saving expense`() {
        val userId = 2005L
        val chatId = 3005L

        dialogueRouter.processUserInput(userId = userId, chatId = chatId, text = "/start")
        dialogueRouter.processUserInput(userId = userId, chatId = chatId, text = Buttons.ADD_EXPENSE)

        val result =
            dialogueRouter.processUserInput(
                userId = userId,
                chatId = chatId,
                text = "такси",
            )

        assertThat(result.response.message).isEqualTo(BotMessage.Error(BusinessErrorCode.EXPENSE_INVALID_FORMAT))
        assertThat(result.nextState).isEqualTo(UserState.AwaitingExpenseInput)
        assertThat(findExpenses(userId)).isEmpty()
    }

    @Test
    fun `expense input without categories returns no categories message without saving expense`() {
        val userId = 2007L
        val chatId = 3007L

        dialogueRouter.processUserInput(userId = userId, chatId = chatId, text = Buttons.ADD_EXPENSE)

        val result =
            dialogueRouter.processUserInput(
                userId = userId,
                chatId = chatId,
                text = "500 такси",
            )

        assertThat(result.response.message).isEqualTo(BotMessage.NoCategories)
        assertThat(result.response.actions).containsExactly(BotAction.ShowMainMenu)
        assertThat(result.nextState).isEqualTo(UserState.Idle)
        assertThat(findExpenses(userId)).isEmpty()
    }

    @Test
    fun `start command in the middle of add expense flow resets state without saving expense`() {
        val userId = 2006L
        val chatId = 3006L

        startCategorySelection(userId, chatId)

        val result =
            dialogueRouter.processUserInput(
                userId = userId,
                chatId = chatId,
                text = "/start",
            )

        assertThat(result.response.message).isEqualTo(BotMessage.WelcomeBack)
        assertThat(result.nextState).isEqualTo(UserState.Idle)
        assertThat(findExpenses(userId)).isEmpty()
    }

    private fun startCategorySelection(
        userId: Long,
        chatId: Long,
    ): ParsedExpense {
        dialogueRouter.processUserInput(userId = userId, chatId = chatId, text = "/start")
        dialogueRouter.processUserInput(userId = userId, chatId = chatId, text = Buttons.ADD_EXPENSE)

        val result =
            dialogueRouter.processUserInput(
                userId = userId,
                chatId = chatId,
                text = "500 такси",
            )
        val categorySelectionAction = result.response.actions.single() as BotAction.ShowCategorySelection

        assertThat(result.response.message).isEqualTo(BotMessage.SelectCategory(Money.of(BigDecimal("500.00")), "такси"))
        assertThat(categorySelectionAction.categories).isNotEmpty()

        return ParsedExpense(Money.of(BigDecimal("500.00")), "такси")
    }

    private fun findExpenses(userId: Long): List<Expense> =
        expenseRepository.findAllByUserIdAndPeriod(
            userId = userId,
            from = TEST_PERIOD_FROM,
            to = TEST_PERIOD_TO,
        )

    private companion object {
        val TEST_PERIOD_FROM: OffsetDateTime = OffsetDateTime.parse("2000-01-01T00:00:00Z")
        val TEST_PERIOD_TO: OffsetDateTime = OffsetDateTime.parse("2100-01-01T00:00:00Z")
    }
}
