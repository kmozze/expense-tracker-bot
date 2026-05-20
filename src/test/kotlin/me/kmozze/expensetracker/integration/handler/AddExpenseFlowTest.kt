package me.kmozze.expensetracker.integration.handler

import me.kmozze.expensetracker.adapter.ui.Buttons
import me.kmozze.expensetracker.exception.BusinessErrorCode
import me.kmozze.expensetracker.handler.DialogueRouter
import me.kmozze.expensetracker.integration.AbstractIntegrationTest
import me.kmozze.expensetracker.model.domain.Action
import me.kmozze.expensetracker.model.domain.Message
import me.kmozze.expensetracker.model.domain.ParsedExpense
import me.kmozze.expensetracker.model.domain.UserInput
import me.kmozze.expensetracker.model.domain.UserState
import me.kmozze.expensetracker.model.entity.Category
import me.kmozze.expensetracker.model.entity.Expense
import me.kmozze.expensetracker.repository.ICategoryRepository
import me.kmozze.expensetracker.repository.IExpenseRepository
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

        assertThat(categorySelectionAction.categories).isNotEmpty()
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
            findExpenses(userId)

        assertThat(expenses).hasSize(1)
        assertThat(expenses.single().amount).isEqualByComparingTo(BigDecimal("500"))
        assertThat(expenses.single().categoryId).isEqualTo(category.id)
        assertThat(expenses.single().description).isEqualTo("такси")
    }

    @Test
    fun `cancel category selection returns to idle without saving expense`() {
        val userId = 2002L
        val chatId = 3002L

        startCategorySelection(userId, chatId)

        val result =
            dialogueRouter.process(
                UserInput(
                    userId = userId,
                    chatId = chatId,
                    callbackData = "cancel",
                ),
            )

        assertThat(result.response.message).isEqualTo(Message.ExpenseCanceled)
        assertThat(result.nextState).isEqualTo(UserState.Idle)
        assertThat(findExpenses(userId)).isEmpty()
    }

    @Test
    fun `invalid category callback keeps category selection open without saving expense`() {
        val userId = 2003L
        val chatId = 3003L

        val parsedExpense = startCategorySelection(userId, chatId)

        val result =
            dialogueRouter.process(
                UserInput(
                    userId = userId,
                    chatId = chatId,
                    callbackData = "select_category:not-a-uuid",
                ),
            )

        assertThat(result.response.message).isEqualTo(Message.Error(BusinessErrorCode.INVALID_CATEGORY_SELECTION))
        assertThat(result.response.actions.single()).isInstanceOf(Action.ShowCategorySelection::class.java)
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
            dialogueRouter.process(
                UserInput(
                    userId = userId,
                    chatId = chatId,
                    callbackData = "select_category:${foreignCategory.id}",
                ),
            )

        assertThat(result.response.message).isEqualTo(Message.Error(BusinessErrorCode.CATEGORY_NOT_FOUND))
        assertThat(result.response.actions.single()).isInstanceOf(Action.ShowCategorySelection::class.java)
        assertThat(result.nextState).isEqualTo(UserState.AwaitingCategorySelection(parsedExpense))
        assertThat(findExpenses(userId)).isEmpty()
    }

    @Test
    fun `parse error keeps awaiting expense input without saving expense`() {
        val userId = 2005L
        val chatId = 3005L

        dialogueRouter.process(UserInput(userId = userId, chatId = chatId, text = "/start"))
        dialogueRouter.process(UserInput(userId = userId, chatId = chatId, text = Buttons.ADD_EXPENSE))

        val result =
            dialogueRouter.process(
                UserInput(
                    userId = userId,
                    chatId = chatId,
                    text = "такси",
                ),
            )

        assertThat(result.response.message).isEqualTo(Message.Error(BusinessErrorCode.EXPENSE_INVALID_FORMAT))
        assertThat(result.nextState).isEqualTo(UserState.AwaitingExpenseInput)
        assertThat(findExpenses(userId)).isEmpty()
    }

    @Test
    fun `start command in the middle of add expense flow resets state without saving expense`() {
        val userId = 2006L
        val chatId = 3006L

        startCategorySelection(userId, chatId)

        val result =
            dialogueRouter.process(
                UserInput(
                    userId = userId,
                    chatId = chatId,
                    text = "/start",
                ),
            )

        assertThat(result.response.message).isEqualTo(Message.WelcomeBack)
        assertThat(result.nextState).isEqualTo(UserState.Idle)
        assertThat(findExpenses(userId)).isEmpty()
    }

    private fun startCategorySelection(
        userId: Long,
        chatId: Long,
    ): ParsedExpense {
        dialogueRouter.process(UserInput(userId = userId, chatId = chatId, text = "/start"))
        dialogueRouter.process(UserInput(userId = userId, chatId = chatId, text = Buttons.ADD_EXPENSE))

        val result =
            dialogueRouter.process(
                UserInput(
                    userId = userId,
                    chatId = chatId,
                    text = "500 такси",
                ),
            )
        val categorySelectionAction = result.response.actions.single() as Action.ShowCategorySelection

        assertThat(result.response.message).isEqualTo(Message.SelectCategory(BigDecimal("500"), "такси"))
        assertThat(categorySelectionAction.categories).isNotEmpty()

        return ParsedExpense(BigDecimal("500"), "такси")
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
