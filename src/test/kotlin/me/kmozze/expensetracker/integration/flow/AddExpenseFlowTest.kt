package me.kmozze.expensetracker.integration.flow

import me.kmozze.expensetracker.adapter.callback.CallbackData
import me.kmozze.expensetracker.adapter.ui.Buttons
import me.kmozze.expensetracker.exception.BusinessErrorCode
import me.kmozze.expensetracker.handler.DialogueRouter
import me.kmozze.expensetracker.model.domain.bot.BotAction
import me.kmozze.expensetracker.model.domain.bot.BotText
import me.kmozze.expensetracker.model.domain.bot.HandlerResponse
import me.kmozze.expensetracker.model.domain.bot.OutgoingMessage
import me.kmozze.expensetracker.model.domain.bot.ResponseDelivery
import me.kmozze.expensetracker.model.domain.bot.UserState
import me.kmozze.expensetracker.model.domain.expense.ExpenseDraft
import me.kmozze.expensetracker.model.domain.expense.ExpenseDraftCategory
import me.kmozze.expensetracker.model.domain.expense.Money
import me.kmozze.expensetracker.model.entity.Category
import me.kmozze.expensetracker.model.entity.Expense
import me.kmozze.expensetracker.repository.ICategoryRepository
import me.kmozze.expensetracker.repository.IExpenseRepository
import me.kmozze.expensetracker.support.processUserInput
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.math.BigDecimal
import java.time.Clock
import java.time.LocalDate
import java.time.OffsetDateTime

class AddExpenseFlowTest : AbstractFlowIntegrationTest() {
    @Autowired
    private lateinit var dialogueRouter: DialogueRouter

    @Autowired
    private lateinit var expenseRepository: IExpenseRepository

    @Autowired
    private lateinit var categoryRepository: ICategoryRepository

    @Autowired
    private lateinit var clock: Clock

    @Test
    fun `add expense flow saves expense after today date selection`() {
        val userId = 2001L
        val chatId = 3001L

        val addExpenseResult = startExpenseInput(userId, chatId)

        addExpenseResult.assertSingleMessage(BotText.AddExpenseInstructions)
        addExpenseResult.assertNextState(UserState.AwaitingExpenseInput)

        val categorySelection = submitExpenseForCategorySelection(userId, chatId, EXPENSE_TEXT_WITH_DESCRIPTION)
        val category = categorySelection.category

        categorySelection.result.assertMessageTexts(
            BotText.ExpenseView(
                amount = EXPENSE_AMOUNT,
                categoryName = null,
                expenseDate = null,
                description = EXPENSE_DESCRIPTION,
            ),
            BotText.SelectCategory,
        )
        categorySelection.result.assertNextState(UserState.AwaitingCategorySelection(EXPENSE_WITH_DESCRIPTION))

        val dateSelectionResult = selectCategoryForDateSelection(userId, chatId, category, EXPENSE_WITH_DESCRIPTION)

        dateSelectionResult.assertMessageTexts(
            BotText.ExpenseView(
                amount = EXPENSE_AMOUNT,
                categoryName = category.name,
                expenseDate = null,
                description = EXPENSE_DESCRIPTION,
            ),
            BotText.SelectExpenseDate,
        )
        dateSelectionResult.assertNextState(
            UserState.AwaitingExpenseDateSelection(
                expenseDraft = EXPENSE_WITH_DESCRIPTION.withCategory(category),
            ),
        )
        assertThat(findExpenses(userId)).isEmpty()

        val expenseDateBeforeSave = LocalDate.now(clock)
        val savedExpenseResult = selectTodayDate(userId, chatId)
        val expenseDateAfterSave = LocalDate.now(clock)
        val savedMessage = savedExpenseResult.savedExpenseMessage()

        assertThat(savedMessage.amount).isEqualTo(EXPENSE_AMOUNT)
        assertThat(savedMessage.categoryName).isEqualTo(category.name)
        assertThat(savedMessage.expenseDate).isBetween(expenseDateBeforeSave, expenseDateAfterSave)
        assertThat(savedMessage.description).isEqualTo(EXPENSE_DESCRIPTION)
        val doneMessage = savedExpenseResult.outgoingMessages.first()
        assertThat(doneMessage.text).isEqualTo(BotText.ExpenseSaved)
        assertThat(doneMessage.actions).containsExactly(BotAction.RemoveReplyKeyboard)
        savedExpenseResult.assertNextState(UserState.Idle)

        val expenses = findExpenses(userId)

        assertThat(expenses).hasSize(1)
        val expense = expenses.single()

        assertThat(expense.amount).isEqualTo(EXPENSE_AMOUNT)
        assertThat(expense.categoryId).isEqualTo(category.id)
        assertThat(expense.expenseDate).isBetween(expenseDateBeforeSave, expenseDateAfterSave)
        assertThat(expense.description).isEqualTo(EXPENSE_DESCRIPTION)
        assertThat(savedExpenseResult.expenseCardAction().expenseId).isEqualTo(expense.id)
    }

    @Test
    fun `add expense flow saves expense without description after manual date input`() {
        val userId = 2008L
        val chatId = 3008L

        startExpenseInput(userId, chatId)
        val categorySelection = submitExpenseForCategorySelection(userId, chatId, EXPENSE_TEXT_WITHOUT_DESCRIPTION)
        val category = categorySelection.category

        categorySelection.result.assertMessageTexts(
            BotText.ExpenseView(
                amount = EXPENSE_AMOUNT,
                categoryName = null,
                expenseDate = null,
                description = null,
            ),
            BotText.SelectCategory,
        )
        categorySelection.result.assertNextState(UserState.AwaitingCategorySelection(EXPENSE_WITHOUT_DESCRIPTION))

        selectCategoryForDateSelection(userId, chatId, category, EXPENSE_WITHOUT_DESCRIPTION)

        val manualDateInputResult =
            dialogueRouter.processUserInput(
                userId = userId,
                chatId = chatId,
                text = Buttons.ENTER_DATE_MANUALLY,
            )
        manualDateInputResult.assertMessageTexts(
            BotText.ExpenseView(
                amount = EXPENSE_AMOUNT,
                categoryName = category.name,
                expenseDate = null,
                description = null,
            ),
            BotText.EnterExpenseDateManually,
        )
        manualDateInputResult.assertLastMessageActions(BotAction.ShowCancel)
        manualDateInputResult.assertNextState(
            UserState.AwaitingExpenseManualDateInput(
                expenseDraft = EXPENSE_WITHOUT_DESCRIPTION.withCategory(category),
            ),
        )

        val savedExpenseResult =
            dialogueRouter.processUserInput(
                userId = userId,
                chatId = chatId,
                text = MANUAL_DATE_TEXT,
            )

        val savedMessage = savedExpenseResult.savedExpenseMessage()
        assertThat(savedMessage.amount).isEqualTo(EXPENSE_AMOUNT)
        assertThat(savedMessage.categoryName).isEqualTo(category.name)
        assertThat(savedMessage.expenseDate).isEqualTo(MANUAL_DATE)
        assertThat(savedMessage.description).isNull()
        val doneActions =
            savedExpenseResult.outgoingMessages
                .first()
                .actions
        assertThat(doneActions).containsExactly(BotAction.RemoveReplyKeyboard)
        savedExpenseResult.assertNextState(UserState.Idle)

        val expenses = findExpenses(userId)
        assertThat(expenses).hasSize(1)
        val expense = expenses.single()

        assertThat(expense.amount).isEqualTo(EXPENSE_AMOUNT)
        assertThat(expense.categoryId).isEqualTo(category.id)
        assertThat(expense.expenseDate).isEqualTo(MANUAL_DATE)
        assertThat(expense.description).isNull()
        assertThat(savedExpenseResult.expenseCardAction().expenseId).isEqualTo(expense.id)
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
            text = Buttons.ENTER_DATE_MANUALLY,
        )

        val result =
            dialogueRouter.processUserInput(
                userId = userId,
                chatId = chatId,
                text = "31.02.2026",
            )

        result.assertSingleMessage(
            text = BotText.Error(BusinessErrorCode.EXPENSE_DATE_INVALID_FORMAT),
            actions = listOf(BotAction.ShowCancel),
        )
        result.assertNextState(
            UserState.AwaitingExpenseManualDateInput(
                expenseDraft = EXPENSE_WITH_DESCRIPTION.withCategory(category),
            ),
        )
        assertThat(findExpenses(userId)).isEmpty()
    }

    @Test
    fun `expense card deletion asks for confirmation and deletes expense after confirmation`() {
        val userId = 2010L
        val chatId = 3010L

        startExpenseInput(userId, chatId)
        val categorySelection = submitExpenseForCategorySelection(userId, chatId, EXPENSE_TEXT_WITH_DESCRIPTION)
        val category = categorySelection.category
        selectCategoryForDateSelection(userId, chatId, category, EXPENSE_WITH_DESCRIPTION)
        selectTodayDate(userId, chatId)
        val expense = findExpenses(userId).single()

        val deleteRequest =
            dialogueRouter.processUserInput(
                userId = userId,
                chatId = chatId,
                callbackData = CallbackData.deleteExpense(expense.id),
                callbackMessageId = CARD_MESSAGE_ID,
            )

        deleteRequest.assertSingleMessage(
            text =
                BotText.ExpenseView(
                    amount = EXPENSE_AMOUNT,
                    categoryName = category.name,
                    expenseDate = expense.expenseDate,
                    description = EXPENSE_DESCRIPTION,
                ),
            actions = listOf(BotAction.ShowExpenseDeletionConfirmation(expense.id)),
            delivery = ResponseDelivery.EditMessage(CARD_MESSAGE_ID),
        )
        assertThat(findExpenses(userId)).hasSize(1)

        val cancellation =
            dialogueRouter.processUserInput(
                userId = userId,
                chatId = chatId,
                callbackData = CallbackData.cancelExpenseDeletion(expense.id),
                callbackMessageId = CARD_MESSAGE_ID,
            )

        cancellation.assertSingleMessage(
            text =
                BotText.ExpenseView(
                    amount = EXPENSE_AMOUNT,
                    categoryName = category.name,
                    expenseDate = expense.expenseDate,
                    description = EXPENSE_DESCRIPTION,
                ),
            actions = listOf(BotAction.ShowExpenseCardActions(expense.id)),
            delivery = ResponseDelivery.EditMessage(CARD_MESSAGE_ID),
        )

        dialogueRouter.processUserInput(
            userId = userId,
            chatId = chatId,
            callbackData = CallbackData.deleteExpense(expense.id),
            callbackMessageId = CARD_MESSAGE_ID,
        )
        val confirmation =
            dialogueRouter.processUserInput(
                userId = userId,
                chatId = chatId,
                callbackData = CallbackData.confirmExpenseDeletion(expense.id),
                callbackMessageId = CARD_MESSAGE_ID,
            )

        confirmation.assertSingleMessage(
            text = BotText.ExpenseDeleted,
            actions = listOf(BotAction.ClearInlineKeyboard),
            delivery = ResponseDelivery.EditMessage(CARD_MESSAGE_ID),
        )
        confirmation.assertNextState(UserState.Idle)
        assertThat(findExpenses(userId)).isEmpty()
    }

    @Test
    fun `unknown category text keeps category selection open without saving expense`() {
        val userId = 2004L
        val chatId = 3004L

        startCategorySelection(userId, chatId)

        val result =
            dialogueRouter.processUserInput(
                userId = userId,
                chatId = chatId,
                text = "Чужая",
            )

        val message = result.assertSingleMessage(BotText.Error(BusinessErrorCode.CATEGORY_NOT_FOUND))
        assertThat(message.actions.single()).isInstanceOf(BotAction.ShowCategorySelection::class.java)
        result.assertNextState(UserState.AwaitingCategorySelection(EXPENSE_WITH_DESCRIPTION))
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

        result.assertSingleMessage(BotText.Error(BusinessErrorCode.EXPENSE_INVALID_FORMAT))
        result.assertNextState(UserState.AwaitingExpenseInput)
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

        assertThat(result.outgoingMessages)
            .containsExactly(
                OutgoingMessage(
                    text = BotText.WelcomeBack,
                    actions = listOf(BotAction.RemoveReplyKeyboard),
                ),
                OutgoingMessage(
                    text = BotText.MainMenu,
                    actions = listOf(BotAction.ShowMainMenu),
                ),
            )
        result.assertNextState(UserState.Idle)
        assertThat(findExpenses(userId)).isEmpty()
    }

    private fun startExpenseInput(
        userId: Long,
        chatId: Long,
    ): HandlerResponse {
        dialogueRouter.processUserInput(userId = userId, chatId = chatId, text = "/start")

        return dialogueRouter.processUserInput(
            userId = userId,
            chatId = chatId,
            callbackData = CallbackData.menuAddExpense(),
        )
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

        assertThat(action.categoryNames).isNotEmpty()
        val categories = categoryRepository.findAllByUserId(userId)
        assertThat(action.categoryNames).containsExactlyElementsOf(categories.map { it.name })
        val categoriesByName = categories.associateBy { it.name }
        val category =
            action.categoryNames
                .map { categoriesByName.getValue(it) }
                .first()

        return CategorySelection(
            result = result,
            category = category,
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
    ): HandlerResponse {
        val result =
            dialogueRouter.processUserInput(
                userId = userId,
                chatId = chatId,
                text = category.name,
            )

        result.assertLastMessageActions(BotAction.ShowExpenseDateSelection)
        result.assertNextState(
            UserState.AwaitingExpenseDateSelection(
                expenseDraft = expenseDraft.withCategory(category),
            ),
        )

        return result
    }

    private fun selectTodayDate(
        userId: Long,
        chatId: Long,
    ): HandlerResponse =
        dialogueRouter.processUserInput(
            userId = userId,
            chatId = chatId,
            text = Buttons.TODAY,
        )

    private fun findExpenses(userId: Long): List<Expense> =
        expenseRepository.findAllByUserIdAndPeriod(
            userId = userId,
            from = TEST_PERIOD_FROM,
            to = TEST_PERIOD_TO,
        )

    private fun ExpenseDraft.withCategory(category: Category): ExpenseDraft =
        copy(
            category =
                ExpenseDraftCategory(
                    categoryId = category.id,
                    name = category.name,
                ),
        )

    private fun HandlerResponse.categorySelectionAction(): BotAction.ShowCategorySelection =
        outgoingMessages
            .last()
            .actions
            .single() as BotAction.ShowCategorySelection

    private fun HandlerResponse.savedExpenseMessage(): BotText.ExpenseView {
        assertThat(outgoingMessages).hasSize(2)
        return (outgoingMessages[1].text as BotText.ExpenseView)
    }

    private fun HandlerResponse.expenseCardAction(): BotAction.ShowExpenseCardActions =
        outgoingMessages[1].actions.single() as BotAction.ShowExpenseCardActions

    private data class CategorySelection(
        val result: HandlerResponse,
        val category: Category,
    )

    private companion object {
        const val EXPENSE_TEXT_WITH_DESCRIPTION = "500 такси"
        const val EXPENSE_TEXT_WITHOUT_DESCRIPTION = "500"
        const val EXPENSE_DESCRIPTION = "такси"
        const val MANUAL_DATE_TEXT = "20.05.2026"
        val MANUAL_DATE: LocalDate = LocalDate.parse("2026-05-20")
        val EXPENSE_AMOUNT: Money = Money.of(BigDecimal("500.00"))
        val EXPENSE_WITH_DESCRIPTION: ExpenseDraft = ExpenseDraft(EXPENSE_AMOUNT, EXPENSE_DESCRIPTION)
        val EXPENSE_WITHOUT_DESCRIPTION: ExpenseDraft = ExpenseDraft(EXPENSE_AMOUNT, null)
        val TEST_PERIOD_FROM: OffsetDateTime = OffsetDateTime.parse("2000-01-01T00:00:00Z")
        val TEST_PERIOD_TO: OffsetDateTime = OffsetDateTime.parse("2100-01-01T00:00:00Z")
        const val CARD_MESSAGE_ID = 777
    }
}
