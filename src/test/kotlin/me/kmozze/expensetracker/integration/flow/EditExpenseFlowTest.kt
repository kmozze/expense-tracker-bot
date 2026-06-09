package me.kmozze.expensetracker.integration.flow

import me.kmozze.expensetracker.adapter.callback.CallbackData
import me.kmozze.expensetracker.adapter.ui.Buttons
import me.kmozze.expensetracker.handler.DialogueRouter
import me.kmozze.expensetracker.model.domain.bot.BotAction
import me.kmozze.expensetracker.model.domain.bot.BotText
import me.kmozze.expensetracker.model.domain.bot.HandlerResponse
import me.kmozze.expensetracker.model.domain.bot.ResponseDelivery
import me.kmozze.expensetracker.model.domain.bot.UserState
import me.kmozze.expensetracker.model.domain.expense.ExpenseDraft
import me.kmozze.expensetracker.model.domain.expense.ExpenseDraftCategory
import me.kmozze.expensetracker.model.domain.expense.ExpenseEditSession
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
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.UUID

class EditExpenseFlowTest : AbstractFlowIntegrationTest() {
    @Autowired
    private lateinit var dialogueRouter: DialogueRouter

    @Autowired
    private lateinit var expenseRepository: IExpenseRepository

    @Autowired
    private lateinit var categoryRepository: ICategoryRepository

    @Test
    fun `edit expense from card updates amount category date and description`() {
        val userId = 2011L
        val chatId = 3011L

        val createdExpense = createExpense(userId, chatId)
        val initialCategory = createdExpense.second

        val editStart =
            requestEdit(userId = userId, chatId = chatId, expenseId = createdExpense.first.id, callbackMessageId = CARD_MESSAGE_ID)

        editStart.assertMessage(
            index = 0,
            text =
                BotText.ExpenseView(
                    amount = createdExpense.first.amount,
                    categoryName = createdExpense.second.name,
                    expenseDate = createdExpense.first.expenseDate,
                    description = createdExpense.first.description,
                ),
            actions = listOf(BotAction.ClearInlineKeyboard),
            delivery = ResponseDelivery.EditMessage(CARD_MESSAGE_ID),
        )
        editStart.assertMessage(
            index = 1,
            text = BotText.EditExpenseFieldSelection,
            actions = listOf(BotAction.ShowExpenseEditFieldSelection),
        )
        val initialDraft = createdExpense.first.toDraft(initialCategory)
        editStart.assertNextState(editFieldSelectionState(createdExpense.first, initialCategory))

        val amountPrompt =
            dialogueRouter.processUserInput(
                userId = userId,
                chatId = chatId,
                text = Buttons.EDIT_EXPENSE_AMOUNT,
            )
        amountPrompt.assertSingleMessage(
            text = BotText.EnterExpenseAmount,
            actions = listOf(BotAction.ShowCancel),
        )
        amountPrompt.assertNextState(
            UserState.AwaitingExpenseAmountEdit(
                editSession = editSession(createdExpense.first.id, initialDraft),
            ),
        )

        val amountUpdated =
            dialogueRouter.processUserInput(
                userId = userId,
                chatId = chatId,
                text = "650",
            )
        amountUpdated.assertMessageTexts(
            BotText.ExpenseView(
                amount = EXPENSE_AMOUNT_UPDATED,
                categoryName = initialCategory.name,
                expenseDate = createdExpense.first.expenseDate,
                description = EXPENSE_DESCRIPTION_UPDATED,
            ),
            BotText.EditExpenseFieldSelection,
        )
        amountUpdated.assertLastMessageActions(BotAction.ShowExpenseEditFieldSelection)
        val amountDraft = initialDraft.copy(amount = EXPENSE_AMOUNT_UPDATED)
        amountUpdated.assertNextState(
            UserState.AwaitingExpenseEditFieldSelection(
                editSession = editSession(createdExpense.first.id, amountDraft),
            ),
        )
        assertThat(expenseById(userId, createdExpense.first.id).amount).isEqualTo(EXPENSE_AMOUNT)

        val categorySelectionPrompt =
            dialogueRouter.processUserInput(
                userId = userId,
                chatId = chatId,
                text = Buttons.EDIT_EXPENSE_CATEGORY,
            )
        val categorySelection = categorySelectionPrompt.categorySelectionAction()
        categorySelectionPrompt.assertNextState(
            UserState.AwaitingExpenseCategoryEdit(
                editSession = editSession(createdExpense.first.id, amountDraft),
            ),
        )
        val newCategory =
            categoryRepository
                .findAllByUserId(userId)
                .single { it.name == categorySelection.categoryNames.first { categoryName -> categoryName != initialCategory.name } }
        val categoryUpdated =
            dialogueRouter.processUserInput(
                userId = userId,
                chatId = chatId,
                text = newCategory.name,
            )
        categoryUpdated.assertMessageTexts(
            BotText.ExpenseView(
                amount = EXPENSE_AMOUNT_UPDATED,
                categoryName = newCategory.name,
                expenseDate = createdExpense.first.expenseDate,
                description = EXPENSE_DESCRIPTION_UPDATED,
            ),
            BotText.EditExpenseFieldSelection,
        )
        val categoryDraft =
            amountDraft.copy(
                category =
                    ExpenseDraftCategory(
                        categoryId = newCategory.id,
                        name = newCategory.name,
                    ),
            )
        categoryUpdated.assertNextState(
            UserState.AwaitingExpenseEditFieldSelection(
                editSession = editSession(createdExpense.first.id, categoryDraft),
            ),
        )

        val dateSelectionPrompt =
            dialogueRouter.processUserInput(
                userId = userId,
                chatId = chatId,
                text = Buttons.EDIT_EXPENSE_DATE,
            )
        dateSelectionPrompt.assertMessageTexts(
            BotText.ExpenseView(
                amount = EXPENSE_AMOUNT_UPDATED,
                categoryName = newCategory.name,
                expenseDate = createdExpense.first.expenseDate,
                description = EXPENSE_DESCRIPTION_UPDATED,
            ),
            BotText.SelectExpenseDate,
        )
        dateSelectionPrompt.assertLastMessageActions(BotAction.ShowExpenseDateSelection)
        dateSelectionPrompt.assertNextState(
            UserState.AwaitingExpenseDateEditSelection(
                editSession = editSession(createdExpense.first.id, categoryDraft),
            ),
        )
        val manualDatePrompt =
            dialogueRouter.processUserInput(
                userId = userId,
                chatId = chatId,
                text = Buttons.ENTER_DATE_MANUALLY,
            )
        manualDatePrompt.assertMessageTexts(
            BotText.ExpenseView(
                amount = EXPENSE_AMOUNT_UPDATED,
                categoryName = newCategory.name,
                expenseDate = createdExpense.first.expenseDate,
                description = EXPENSE_DESCRIPTION_UPDATED,
            ),
            BotText.EnterExpenseDateManually,
        )
        manualDatePrompt.assertNextState(
            UserState.AwaitingExpenseDateEditManualInput(
                editSession = editSession(createdExpense.first.id, categoryDraft),
            ),
        )

        val dateUpdated =
            dialogueRouter.processUserInput(
                userId = userId,
                chatId = chatId,
                text = MANUAL_DATE_TEXT,
            )
        val dateDraft = categoryDraft.copy(expenseDate = MANUAL_DATE)
        dateUpdated.assertMessageTexts(
            BotText.ExpenseView(
                amount = EXPENSE_AMOUNT_UPDATED,
                categoryName = newCategory.name,
                expenseDate = MANUAL_DATE,
                description = EXPENSE_DESCRIPTION_UPDATED,
            ),
            BotText.EditExpenseFieldSelection,
        )
        dateUpdated.assertNextState(
            UserState.AwaitingExpenseEditFieldSelection(
                editSession = editSession(createdExpense.first.id, dateDraft),
            ),
        )

        val descriptionPrompt =
            dialogueRouter.processUserInput(
                userId = userId,
                chatId = chatId,
                text = Buttons.EDIT_EXPENSE_DESCRIPTION,
            )
        descriptionPrompt.assertSingleMessage(
            text = BotText.EnterExpenseDescription,
            actions = listOf(BotAction.ShowCancel),
        )
        descriptionPrompt.assertNextState(
            UserState.AwaitingExpenseDescriptionEdit(
                editSession = editSession(createdExpense.first.id, dateDraft),
            ),
        )

        val descriptionUpdated =
            dialogueRouter.processUserInput(
                userId = userId,
                chatId = chatId,
                text = DESCRIPTION_UPDATED,
            )
        val finalDraft = dateDraft.copy(description = DESCRIPTION_UPDATED)
        descriptionUpdated.assertMessageTexts(
            BotText.ExpenseView(
                amount = EXPENSE_AMOUNT_UPDATED,
                categoryName = newCategory.name,
                expenseDate = MANUAL_DATE,
                description = DESCRIPTION_UPDATED,
            ),
            BotText.EditExpenseFieldSelection,
        )
        descriptionUpdated.assertNextState(
            UserState.AwaitingExpenseEditFieldSelection(
                editSession = editSession(createdExpense.first.id, finalDraft),
            ),
        )
        val unchangedBeforeFinish = expenseById(userId, createdExpense.first.id)
        assertThat(unchangedBeforeFinish.amount).isEqualTo(EXPENSE_AMOUNT)
        assertThat(unchangedBeforeFinish.categoryId).isEqualTo(initialCategory.id)
        assertThat(unchangedBeforeFinish.expenseDate).isEqualTo(createdExpense.first.expenseDate)
        assertThat(unchangedBeforeFinish.description).isEqualTo(EXPENSE_DESCRIPTION_UPDATED)

        val editFinished =
            dialogueRouter.processUserInput(
                userId = userId,
                chatId = chatId,
                text = Buttons.FINISH_EXPENSE_EDIT,
            )
        val descriptionSaved = editFinished.savedExpenseMessage()
        assertThat(descriptionSaved.amount).isEqualTo(EXPENSE_AMOUNT_UPDATED)
        assertThat(descriptionSaved.description).isEqualTo(DESCRIPTION_UPDATED)
        assertThat(descriptionSaved.expenseDate).isEqualTo(MANUAL_DATE)
        assertThat(descriptionSaved.categoryName).isEqualTo(newCategory.name)
        editFinished.assertNextState(UserState.Idle)
        assertThat(editFinished.expenseCardAction().expenseId).isEqualTo(createdExpense.first.id)

        val saved = expenseById(userId, createdExpense.first.id)
        assertThat(saved.amount).isEqualTo(EXPENSE_AMOUNT_UPDATED)
        assertThat(saved.categoryId).isEqualTo(newCategory.id)
        assertThat(saved.expenseDate).isEqualTo(MANUAL_DATE)
        assertThat(saved.description).isEqualTo(DESCRIPTION_UPDATED)
        assertThat(saved.id).isEqualTo(createdExpense.first.id)
    }

    @Test
    fun `cancel edit after multiple draft changes keeps saved expense unchanged`() {
        val userId = 2014L
        val chatId = 3014L

        val createdExpense = createExpense(userId, chatId)
        val initialExpense = createdExpense.first
        val initialCategory = createdExpense.second

        requestEdit(userId = userId, chatId = chatId, expenseId = initialExpense.id, callbackMessageId = CARD_MESSAGE_ID)

        dialogueRouter.processUserInput(userId = userId, chatId = chatId, text = Buttons.EDIT_EXPENSE_AMOUNT)
        dialogueRouter.processUserInput(userId = userId, chatId = chatId, text = "650")

        val categorySelectionPrompt =
            dialogueRouter.processUserInput(
                userId = userId,
                chatId = chatId,
                text = Buttons.EDIT_EXPENSE_CATEGORY,
            )
        val categorySelection = categorySelectionPrompt.categorySelectionAction()
        val newCategory =
            categoryRepository
                .findAllByUserId(userId)
                .single { it.name == categorySelection.categoryNames.first { categoryName -> categoryName != initialCategory.name } }
        assertThat(newCategory.id).isNotEqualTo(initialCategory.id)
        dialogueRouter.processUserInput(userId = userId, chatId = chatId, text = newCategory.name)

        dialogueRouter.processUserInput(userId = userId, chatId = chatId, text = Buttons.EDIT_EXPENSE_DATE)
        dialogueRouter.processUserInput(userId = userId, chatId = chatId, text = Buttons.ENTER_DATE_MANUALLY)
        dialogueRouter.processUserInput(userId = userId, chatId = chatId, text = MANUAL_DATE_TEXT)

        dialogueRouter.processUserInput(userId = userId, chatId = chatId, text = Buttons.EDIT_EXPENSE_DESCRIPTION)
        dialogueRouter.processUserInput(userId = userId, chatId = chatId, text = DESCRIPTION_UPDATED)

        val unchangedBeforeCancel = expenseById(userId, initialExpense.id)
        assertThat(unchangedBeforeCancel.amount).isEqualTo(EXPENSE_AMOUNT)
        assertThat(unchangedBeforeCancel.categoryId).isEqualTo(initialCategory.id)
        assertThat(unchangedBeforeCancel.expenseDate).isEqualTo(initialExpense.expenseDate)
        assertThat(unchangedBeforeCancel.description).isEqualTo(EXPENSE_DESCRIPTION_UPDATED)

        val editCanceled =
            dialogueRouter.processUserInput(
                userId = userId,
                chatId = chatId,
                text = Buttons.CANCEL,
            )

        editCanceled.assertMessage(
            index = 0,
            text = BotText.Done,
            actions = listOf(BotAction.RemoveReplyKeyboard),
        )
        editCanceled.assertMessage(
            index = 1,
            text =
                BotText.ExpenseView(
                    amount = EXPENSE_AMOUNT,
                    categoryName = initialCategory.name,
                    expenseDate = initialExpense.expenseDate,
                    description = EXPENSE_DESCRIPTION_UPDATED,
                ),
            actions = listOf(BotAction.ShowExpenseCardActions(initialExpense.id)),
        )
        editCanceled.assertNextState(UserState.Idle)

        val savedAfterCancel = expenseById(userId, initialExpense.id)
        assertThat(savedAfterCancel.amount).isEqualTo(EXPENSE_AMOUNT)
        assertThat(savedAfterCancel.categoryId).isEqualTo(initialCategory.id)
        assertThat(savedAfterCancel.expenseDate).isEqualTo(initialExpense.expenseDate)
        assertThat(savedAfterCancel.description).isEqualTo(EXPENSE_DESCRIPTION_UPDATED)
        assertThat(savedAfterCancel.id).isEqualTo(initialExpense.id)
    }

    @Test
    fun `edit unavailable expense from card shows unavailable message and resets state`() {
        val userId = 2012L
        val chatId = 3012L

        val unavailableMessage =
            dialogueRouter.processUserInput(
                userId = userId,
                chatId = chatId,
                callbackData = CallbackData.editExpense(UUID.fromString("00000000-0000-0000-0000-000000000001")),
                callbackMessageId = CARD_MESSAGE_ID,
            )

        unavailableMessage.assertSingleMessage(
            text = BotText.ExpenseUnavailable,
            actions = listOf(BotAction.ClearInlineKeyboard),
            delivery = ResponseDelivery.EditMessage(CARD_MESSAGE_ID),
        )
        unavailableMessage.assertNextState(UserState.Idle)
    }

    @Test
    fun `edit with invalid field repeats field selection`() {
        val userId = 2013L
        val chatId = 3013L

        val createdExpense = createExpense(userId, chatId)
        val editStart =
            requestEdit(userId = userId, chatId = chatId, expenseId = createdExpense.first.id, callbackMessageId = CARD_MESSAGE_ID)
        val invalidFieldResult =
            dialogueRouter.processUserInput(
                userId = userId,
                chatId = chatId,
                text = "Неверная кнопка",
            )
        invalidFieldResult.assertSingleMessage(
            text = BotText.EditExpenseFieldSelection,
            actions = listOf(BotAction.ShowExpenseEditFieldSelection),
            delivery = ResponseDelivery.SendNewMessage,
        )
        invalidFieldResult.assertNextState(editFieldSelectionState(createdExpense.first, createdExpense.second))
        assertThat(editStart.outgoingMessages.first().delivery).isEqualTo(ResponseDelivery.EditMessage(CARD_MESSAGE_ID))
    }

    private fun createExpense(
        userId: Long,
        chatId: Long,
    ): Pair<Expense, Category> {
        startExpenseInput(userId, chatId)
        val categorySelection = submitExpenseForCategorySelection(userId, chatId, EXPENSE_TEXT_WITH_DESCRIPTION)
        val category =
            categorySelection.categories.firstOrNull { it.name == "Транспорт" }
                ?: categorySelection.categories.first()
        selectCategoryForDateSelection(userId, chatId, category, EXPENSE_WITH_DESCRIPTION)
        val savedResult = selectTodayDate(userId, chatId)
        assertThat(savedResult.outgoingMessages).hasSize(2)
        val savedMessage = savedResult.savedExpenseMessage()
        assertThat(savedMessage.amount).isEqualTo(EXPENSE_AMOUNT)
        assertThat(savedMessage.categoryName).isEqualTo(category.name)
        val expense = findExpenses(userId).single()
        assertThat(expense).isNotNull
        return expense to category
    }

    private fun startExpenseInput(
        userId: Long,
        chatId: Long,
    ) {
        dialogueRouter.processUserInput(userId = userId, chatId = chatId, text = "/start")
        dialogueRouter.processUserInput(userId = userId, chatId = chatId, callbackData = CallbackData.menuAddExpense())
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
        val storedCategories = categoryRepository.findAllByUserId(userId)
        assertThat(action.categoryNames).containsExactlyElementsOf(storedCategories.map { it.name })
        val categoriesByName = storedCategories.associateBy { it.name }
        val categories =
            action.categoryNames
                .map { categoriesByName.getValue(it) }

        return CategorySelection(result, categories)
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
                expenseDraft =
                    expenseDraft.copy(
                        category =
                            ExpenseDraftCategory(
                                categoryId = category.id,
                                name = category.name,
                            ),
                    ),
            ),
        )

        return result
    }

    private fun requestEdit(
        userId: Long,
        chatId: Long,
        expenseId: UUID,
        callbackMessageId: Int,
    ) = dialogueRouter.processUserInput(
        userId = userId,
        chatId = chatId,
        callbackData = CallbackData.editExpense(expenseId),
        callbackMessageId = callbackMessageId,
    )

    private fun expenseById(
        userId: Long,
        expenseId: UUID,
    ): Expense =
        expenseRepository
            .findAllByUserIdAndPeriod(
                userId = userId,
                from = TEST_PERIOD_FROM,
                to = TEST_PERIOD_TO,
            ).single { it.id == expenseId }

    private fun findExpenses(userId: Long): List<Expense> =
        expenseRepository.findAllByUserIdAndPeriod(
            userId = userId,
            from = TEST_PERIOD_FROM,
            to = TEST_PERIOD_TO,
        )

    private fun editFieldSelectionState(
        expense: Expense,
        category: Category,
    ): UserState.AwaitingExpenseEditFieldSelection =
        UserState.AwaitingExpenseEditFieldSelection(
            editSession = editSession(expense.id, expense.toDraft(category)),
        )

    private fun editSession(
        expenseId: UUID,
        expenseDraft: ExpenseDraft,
    ): ExpenseEditSession =
        ExpenseEditSession(
            expenseId = expenseId,
            expenseDraft = expenseDraft,
        )

    private fun Expense.toDraft(category: Category): ExpenseDraft =
        ExpenseDraft(
            amount = amount,
            description = description,
            category = ExpenseDraftCategory(categoryId = categoryId, name = category.name),
            expenseDate = expenseDate,
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
        val categories: List<Category>,
    )

    private companion object {
        const val EXPENSE_TEXT_WITH_DESCRIPTION = "500 такси"
        const val DESCRIPTION_UPDATED = "Рабочая встреча"
        const val EXPENSE_DESCRIPTION_UPDATED = "такси"
        const val MANUAL_DATE_TEXT = "20.05.2026"
        val MANUAL_DATE: LocalDate = LocalDate.parse("2026-05-20")
        val EXPENSE_AMOUNT: Money = Money.of(BigDecimal("500.00"))
        val EXPENSE_AMOUNT_UPDATED: Money = Money.of(BigDecimal("650.00"))
        val EXPENSE_WITH_DESCRIPTION: ExpenseDraft = ExpenseDraft(EXPENSE_AMOUNT, EXPENSE_DESCRIPTION_UPDATED)
        val TEST_PERIOD_FROM: OffsetDateTime = OffsetDateTime.parse("2000-01-01T00:00:00Z")
        val TEST_PERIOD_TO: OffsetDateTime = OffsetDateTime.parse("2100-01-01T00:00:00Z")
        const val CARD_MESSAGE_ID = 777
    }
}
