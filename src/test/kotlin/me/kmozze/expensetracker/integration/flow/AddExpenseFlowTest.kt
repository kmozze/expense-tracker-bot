package me.kmozze.expensetracker.integration.flow

import me.kmozze.expensetracker.adapter.callback.CallbackData
import me.kmozze.expensetracker.adapter.ui.Buttons
import me.kmozze.expensetracker.exception.BusinessErrorCode
import me.kmozze.expensetracker.handler.DialogueRouter
import me.kmozze.expensetracker.model.domain.BotAction
import me.kmozze.expensetracker.model.domain.BotMessage
import me.kmozze.expensetracker.model.domain.ExpenseDraft
import me.kmozze.expensetracker.model.domain.HandlerResult
import me.kmozze.expensetracker.model.domain.Money
import me.kmozze.expensetracker.model.domain.ResponseDelivery
import me.kmozze.expensetracker.model.domain.UserState
import me.kmozze.expensetracker.model.entity.Category
import me.kmozze.expensetracker.model.entity.Expense
import me.kmozze.expensetracker.repository.ICategoryRepository
import me.kmozze.expensetracker.repository.IExpenseRepository
import me.kmozze.expensetracker.support.processUserInput
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.math.BigDecimal
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.UUID

class AddExpenseFlowTest : AbstractFlowIntegrationTest() {
    @Autowired
    private lateinit var dialogueRouter: DialogueRouter

    @Autowired
    private lateinit var categoryRepository: ICategoryRepository

    @Autowired
    private lateinit var expenseRepository: IExpenseRepository

    @Test
    fun `add expense flow saves expense after today date selection`() {
        val userId = 2001L
        val chatId = 3001L

        val addExpenseResult = startExpenseInput(userId, chatId)

        assertThat(addExpenseResult.response.message).isEqualTo(BotMessage.AddExpenseInstructions)
        assertThat(addExpenseResult.nextState).isEqualTo(UserState.AwaitingExpenseInput)

        val categorySelection = submitExpenseForCategorySelection(userId, chatId, EXPENSE_TEXT_WITH_DESCRIPTION)
        val category = categorySelection.category

        assertThat(categorySelection.result.response.message)
            .isEqualTo(BotMessage.SelectCategory(EXPENSE_AMOUNT, EXPENSE_DESCRIPTION))
        assertThat(categorySelection.result.nextState)
            .isEqualTo(UserState.AwaitingCategorySelection(EXPENSE_WITH_DESCRIPTION))

        val dateSelectionResult = selectCategoryForDateSelection(userId, chatId, category, EXPENSE_WITH_DESCRIPTION)

        assertThat(dateSelectionResult.response.message)
            .isEqualTo(
                BotMessage.SelectExpenseDate(
                    amount = EXPENSE_AMOUNT,
                    categoryName = category.name,
                    description = EXPENSE_DESCRIPTION,
                ),
            )
        assertThat(dateSelectionResult.nextState)
            .isEqualTo(
                UserState.AwaitingExpenseDateSelection(
                    expenseDraft =
                        EXPENSE_WITH_DESCRIPTION.copy(
                            categoryId = category.id,
                            categoryName = category.name,
                        ),
                    cardMessageId = CARD_MESSAGE_ID,
                ),
            )
        assertThat(findExpenses(userId)).isEmpty()

        val expenseDateBeforeSave = LocalDate.now()
        val savedExpenseResult = selectTodayDate(userId, chatId)
        val expenseDateAfterSave = LocalDate.now()
        val savedMessage = savedExpenseResult.response.message as BotMessage.ExpenseSaved
        val savedExpenseId = savedExpenseResult.savedExpenseAction().expenseId

        assertThat(savedMessage.amount).isEqualTo(EXPENSE_AMOUNT)
        assertThat(savedMessage.categoryName).isEqualTo(category.name)
        assertThat(savedMessage.expenseDate).isBetween(expenseDateBeforeSave, expenseDateAfterSave)
        assertThat(savedMessage.description).isEqualTo(EXPENSE_DESCRIPTION)
        assertThat(savedMessage.showDeletionConfirmation).isFalse()
        assertThat(savedExpenseResult.response.actions).containsExactly(BotAction.ShowExpenseCardActions(savedExpenseId))
        assertThat(savedExpenseResult.response.delivery).isEqualTo(ResponseDelivery.EditMessage(CARD_MESSAGE_ID))
        assertThat(savedExpenseResult.nextState).isEqualTo(UserState.Idle)

        val expenses = findExpenses(userId)

        assertThat(expenses).hasSize(1)
        val expense = expenses.single()

        assertThat(expense.id).isEqualTo(savedExpenseId)
        assertThat(expense.amount).isEqualTo(EXPENSE_AMOUNT)
        assertThat(expense.categoryId).isEqualTo(category.id)
        assertThat(expense.expenseDate).isBetween(expenseDateBeforeSave, expenseDateAfterSave)
        assertThat(expense.description).isEqualTo(EXPENSE_DESCRIPTION)
    }

    @Test
    fun `add expense flow saves expense without description after manual date input`() {
        val userId = 2008L
        val chatId = 3008L

        startExpenseInput(userId, chatId)
        val categorySelection = submitExpenseForCategorySelection(userId, chatId, EXPENSE_TEXT_WITHOUT_DESCRIPTION)
        val category = categorySelection.category

        assertThat(categorySelection.result.response.message)
            .isEqualTo(BotMessage.SelectCategory(EXPENSE_AMOUNT, null))
        assertThat(categorySelection.result.nextState)
            .isEqualTo(UserState.AwaitingCategorySelection(EXPENSE_WITHOUT_DESCRIPTION))

        selectCategoryForDateSelection(userId, chatId, category, EXPENSE_WITHOUT_DESCRIPTION)

        val manualDateInputResult =
            dialogueRouter.processUserInput(
                userId = userId,
                chatId = chatId,
                callbackData = CallbackData.enterExpenseDateManually(),
                callbackMessageId = CARD_MESSAGE_ID,
            )
        assertThat(manualDateInputResult.response.message)
            .isEqualTo(
                BotMessage.EnterExpenseDateManually(
                    amount = EXPENSE_AMOUNT,
                    categoryName = category.name,
                    description = null,
                ),
            )
        assertThat(manualDateInputResult.response.actions).containsExactly(BotAction.ShowCancel)
        assertThat(manualDateInputResult.response.delivery).isEqualTo(ResponseDelivery.EditMessage(CARD_MESSAGE_ID))
        assertThat(manualDateInputResult.nextState)
            .isEqualTo(
                UserState.AwaitingExpenseManualDateInput(
                    expenseDraft =
                        EXPENSE_WITHOUT_DESCRIPTION.copy(
                            categoryId = category.id,
                            categoryName = category.name,
                        ),
                    cardMessageId = CARD_MESSAGE_ID,
                ),
            )

        val savedExpenseResult =
            dialogueRouter.processUserInput(
                userId = userId,
                chatId = chatId,
                text = MANUAL_DATE_TEXT,
            )

        val savedMessage = savedExpenseResult.response.message as BotMessage.ExpenseSaved
        val savedExpenseId = savedExpenseResult.savedExpenseAction().expenseId
        assertThat(savedMessage.amount).isEqualTo(EXPENSE_AMOUNT)
        assertThat(savedMessage.categoryName).isEqualTo(category.name)
        assertThat(savedMessage.expenseDate).isEqualTo(MANUAL_DATE)
        assertThat(savedMessage.description).isNull()
        assertThat(savedMessage.showDeletionConfirmation).isFalse()
        assertThat(savedExpenseResult.response.actions).containsExactly(BotAction.ShowExpenseCardActions(savedExpenseId))
        assertThat(savedExpenseResult.response.delivery).isEqualTo(ResponseDelivery.EditMessage(CARD_MESSAGE_ID))
        assertThat(savedExpenseResult.nextState).isEqualTo(UserState.Idle)

        val expenses = findExpenses(userId)
        assertThat(expenses).hasSize(1)
        val expense = expenses.single()

        assertThat(expense.id).isEqualTo(savedExpenseId)
        assertThat(expense.amount).isEqualTo(EXPENSE_AMOUNT)
        assertThat(expense.categoryId).isEqualTo(category.id)
        assertThat(expense.expenseDate).isEqualTo(MANUAL_DATE)
        assertThat(expense.description).isNull()
    }

    @Test
    fun `saved expense card deletion flow removes expense after confirmation`() {
        val userId = 2010L
        val chatId = 3010L

        startExpenseInput(userId, chatId)
        val categorySelection = submitExpenseForCategorySelection(userId, chatId, EXPENSE_TEXT_WITH_DESCRIPTION)
        val category = categorySelection.category
        selectCategoryForDateSelection(userId, chatId, category, EXPENSE_WITH_DESCRIPTION)

        val savedExpenseResult = selectTodayDate(userId, chatId)
        val savedMessage = savedExpenseResult.response.message as BotMessage.ExpenseSaved
        val savedExpenseId = savedExpenseResult.savedExpenseAction().expenseId

        val confirmationResult =
            dialogueRouter.processUserInput(
                userId = userId,
                chatId = chatId,
                callbackData = CallbackData.requestExpenseDeletion(savedExpenseId),
                callbackMessageId = CARD_MESSAGE_ID,
            )

        assertThat(confirmationResult.response.message)
            .isEqualTo(
                savedMessage.copy(showDeletionConfirmation = true),
            )
        assertThat(confirmationResult.response.actions)
            .containsExactly(BotAction.ShowExpenseDeletionConfirmation(savedExpenseId))
        assertThat(confirmationResult.response.delivery).isEqualTo(ResponseDelivery.EditMessage(CARD_MESSAGE_ID))
        assertThat(confirmationResult.nextState)
            .isEqualTo(
                UserState.AwaitingExpenseDeletionConfirmation(
                    expenseId = savedExpenseId,
                    cardMessageId = CARD_MESSAGE_ID,
                ),
            )
        assertThat(findExpenses(userId)).hasSize(1)

        val deletedResult =
            dialogueRouter.processUserInput(
                userId = userId,
                chatId = chatId,
                callbackData = CallbackData.confirmExpenseDeletion(savedExpenseId),
                callbackMessageId = CARD_MESSAGE_ID,
            )

        assertThat(deletedResult.response.message).isEqualTo(BotMessage.ExpenseDeleted)
        assertThat(deletedResult.response.actions).containsExactly(BotAction.ClearInlineKeyboard)
        assertThat(deletedResult.response.delivery).isEqualTo(ResponseDelivery.EditMessage(CARD_MESSAGE_ID))
        assertThat(deletedResult.nextState).isEqualTo(UserState.Idle)
        assertThat(findExpenses(userId)).isEmpty()
    }

    @Test
    fun `invalid manual date keeps manual date input open without saving expense`() {
        val userId = 2009L
        val chatId = 3009L

        startExpenseInput(userId, chatId)
        val categorySelection = submitExpenseForCategorySelection(userId, chatId, EXPENSE_TEXT_WITH_DESCRIPTION)
        val category = categorySelection.category
        selectCategoryForDateSelection(userId, chatId, category, EXPENSE_WITH_DESCRIPTION)
        dialogueRouter.processUserInput(
            userId = userId,
            chatId = chatId,
            callbackData = CallbackData.enterExpenseDateManually(),
            callbackMessageId = CARD_MESSAGE_ID,
        )

        val result =
            dialogueRouter.processUserInput(
                userId = userId,
                chatId = chatId,
                text = "31.02.2026",
            )

        assertThat(result.response.message).isEqualTo(BotMessage.Error(BusinessErrorCode.EXPENSE_DATE_INVALID_FORMAT))
        assertThat(result.response.actions).containsExactly(BotAction.ShowCancel)
        assertThat(result.response.delivery).isEqualTo(ResponseDelivery.EditMessage(CARD_MESSAGE_ID))
        assertThat(result.nextState)
            .isEqualTo(
                UserState.AwaitingExpenseManualDateInput(
                    expenseDraft =
                        EXPENSE_WITH_DESCRIPTION.copy(
                            categoryId = category.id,
                            categoryName = category.name,
                        ),
                    cardMessageId = CARD_MESSAGE_ID,
                ),
            )
        assertThat(findExpenses(userId)).isEmpty()
    }

    @Test
    fun `foreign category callback keeps category selection open without saving expense`() {
        val userId = 2004L
        val chatId = 3004L

        startCategorySelection(userId, chatId)

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
                callbackMessageId = CARD_MESSAGE_ID,
            )

        assertThat(result.response.message).isEqualTo(BotMessage.Error(BusinessErrorCode.CATEGORY_NOT_FOUND))
        assertThat(result.response.actions.single()).isInstanceOf(BotAction.ShowCategorySelection::class.java)
        assertThat(result.response.delivery).isEqualTo(ResponseDelivery.EditMessage(CARD_MESSAGE_ID))
        assertThat(result.nextState).isEqualTo(UserState.AwaitingCategorySelection(EXPENSE_WITH_DESCRIPTION))
        assertThat(findExpenses(userId)).isEmpty()
    }

    @Test
    fun `parse error keeps awaiting expense input without saving expense`() {
        val userId = 2005L
        val chatId = 3005L

        startExpenseInput(userId, chatId)

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

    private fun startExpenseInput(
        userId: Long,
        chatId: Long,
    ): HandlerResult {
        dialogueRouter.processUserInput(userId = userId, chatId = chatId, text = "/start")

        return dialogueRouter.processUserInput(userId = userId, chatId = chatId, text = Buttons.ADD_EXPENSE)
    }

    private fun submitExpenseForCategorySelection(
        userId: Long,
        chatId: Long,
        expenseText: String,
    ): CategorySelection {
        val result =
            dialogueRouter.processUserInput(
                userId = userId,
                chatId = chatId,
                text = expenseText,
            )
        val action = result.categorySelectionAction()

        assertThat(action.categories).isNotEmpty()

        return CategorySelection(
            result = result,
            category = action.categories.first(),
        )
    }

    private fun startCategorySelection(
        userId: Long,
        chatId: Long,
    ): CategorySelection {
        startExpenseInput(userId, chatId)

        return submitExpenseForCategorySelection(userId, chatId, EXPENSE_TEXT_WITH_DESCRIPTION)
    }

    private fun selectCategoryForDateSelection(
        userId: Long,
        chatId: Long,
        category: Category,
        expenseDraft: ExpenseDraft,
    ): HandlerResult {
        val result =
            dialogueRouter.processUserInput(
                userId = userId,
                chatId = chatId,
                callbackData = CallbackData.selectCategory(category.id),
                callbackMessageId = CARD_MESSAGE_ID,
            )

        assertThat(result.response.actions).containsExactly(BotAction.ShowExpenseDateSelection)
        assertThat(result.response.delivery).isEqualTo(ResponseDelivery.EditMessage(CARD_MESSAGE_ID))
        assertThat(result.nextState)
            .isEqualTo(
                UserState.AwaitingExpenseDateSelection(
                    expenseDraft =
                        expenseDraft.copy(
                            categoryId = category.id,
                            categoryName = category.name,
                        ),
                    cardMessageId = CARD_MESSAGE_ID,
                ),
            )

        return result
    }

    private fun selectTodayDate(
        userId: Long,
        chatId: Long,
    ): HandlerResult =
        dialogueRouter.processUserInput(
            userId = userId,
            chatId = chatId,
            callbackData = CallbackData.selectExpenseDateToday(),
            callbackMessageId = CARD_MESSAGE_ID,
        )

    private fun findExpenses(userId: Long): List<Expense> =
        expenseRepository.findAllByUserIdAndPeriod(
            userId = userId,
            from = TEST_PERIOD_FROM,
            to = TEST_PERIOD_TO,
        )

    private fun HandlerResult.categorySelectionAction(): BotAction.ShowCategorySelection =
        response.actions.single() as BotAction.ShowCategorySelection

    private fun HandlerResult.savedExpenseAction(): BotAction.ShowExpenseCardActions =
        response.actions.single() as BotAction.ShowExpenseCardActions

    private data class CategorySelection(
        val result: HandlerResult,
        val category: Category,
    )

    private companion object {
        const val EXPENSE_TEXT_WITH_DESCRIPTION = "500 такси"
        const val EXPENSE_TEXT_WITHOUT_DESCRIPTION = "500"
        const val EXPENSE_DESCRIPTION = "такси"
        const val MANUAL_DATE_TEXT = "20.05.2026"
        const val CARD_MESSAGE_ID = 789
        val MANUAL_DATE: LocalDate = LocalDate.parse("2026-05-20")
        val EXPENSE_AMOUNT: Money = Money.of(BigDecimal("500.00"))
        val EXPENSE_WITH_DESCRIPTION: ExpenseDraft = ExpenseDraft(EXPENSE_AMOUNT, EXPENSE_DESCRIPTION)
        val EXPENSE_WITHOUT_DESCRIPTION: ExpenseDraft = ExpenseDraft(EXPENSE_AMOUNT, null)
        val TEST_PERIOD_FROM: OffsetDateTime = OffsetDateTime.parse("2000-01-01T00:00:00Z")
        val TEST_PERIOD_TO: OffsetDateTime = OffsetDateTime.parse("2100-01-01T00:00:00Z")
    }
}
