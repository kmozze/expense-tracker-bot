package me.kmozze.expensetracker.integration.flow

import me.kmozze.expensetracker.adapter.callback.CallbackData
import me.kmozze.expensetracker.adapter.ui.Buttons
import me.kmozze.expensetracker.handler.DialogueRouter
import me.kmozze.expensetracker.model.domain.BotAction
import me.kmozze.expensetracker.model.domain.BotText
import me.kmozze.expensetracker.model.domain.ExpenseDraft
import me.kmozze.expensetracker.model.domain.HandlerResponse
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

        assertThat(editStart.outgoingMessages[0].text).isEqualTo(
            BotText.ExpenseView(
                amount = createdExpense.first.amount,
                categoryName = createdExpense.second.name,
                expenseDate = createdExpense.first.expenseDate,
                description = createdExpense.first.description,
            ),
        )
        assertThat(editStart.outgoingMessages[0].actions).containsExactly(BotAction.ClearInlineKeyboard)
        assertThat(editStart.outgoingMessages[0].delivery).isEqualTo(ResponseDelivery.EditMessage(CARD_MESSAGE_ID))
        assertThat(editStart.outgoingMessages[1].text).isEqualTo(BotText.EditExpenseFieldSelection)
        assertThat(editStart.outgoingMessages[1].actions).containsExactly(BotAction.ShowExpenseEditFieldSelection)
        val initialDraft = createdExpense.first.toDraft()
        assertThat(editStart.nextState).isEqualTo(editFieldSelectionState(createdExpense.first, initialCategory))

        val amountPrompt =
            dialogueRouter.processUserInput(
                userId = userId,
                chatId = chatId,
                text = Buttons.EDIT_EXPENSE_AMOUNT,
            )
        assertThat(
            amountPrompt.outgoingMessages
                .single()
                .text,
        ).isEqualTo(BotText.EnterExpenseAmount)
        assertThat(
            amountPrompt.outgoingMessages
                .single()
                .actions,
        ).containsExactly(BotAction.ShowCancel)
        assertThat(amountPrompt.nextState).isEqualTo(
            UserState.AwaitingExpenseAmountEdit(
                expenseId = createdExpense.first.id,
                expenseDraft = initialDraft,
                categoryName = initialCategory.name,
            ),
        )

        val amountUpdated =
            dialogueRouter.processUserInput(
                userId = userId,
                chatId = chatId,
                text = "650",
            )
        assertThat(
            amountUpdated.outgoingMessages
                .map { it.text },
        ).containsExactly(
            BotText.ExpenseView(
                amount = EXPENSE_AMOUNT_UPDATED,
                categoryName = initialCategory.name,
                expenseDate = createdExpense.first.expenseDate,
                description = EXPENSE_DESCRIPTION_UPDATED,
            ),
            BotText.EditExpenseFieldSelection,
        )
        assertThat(
            amountUpdated.outgoingMessages
                .last()
                .actions,
        ).containsExactly(BotAction.ShowExpenseEditFieldSelection)
        val amountDraft = initialDraft.copy(amount = EXPENSE_AMOUNT_UPDATED)
        assertThat(amountUpdated.nextState).isEqualTo(
            UserState.AwaitingExpenseEditFieldSelection(
                expenseId = createdExpense.first.id,
                expenseDraft = amountDraft,
                categoryName = initialCategory.name,
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
        assertThat(categorySelectionPrompt.nextState).isEqualTo(
            UserState.AwaitingExpenseCategoryEdit(
                expenseId = createdExpense.first.id,
                expenseDraft = amountDraft,
                categoryName = initialCategory.name,
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
        assertThat(
            categoryUpdated.outgoingMessages
                .map { it.text },
        ).containsExactly(
            BotText.ExpenseView(
                amount = EXPENSE_AMOUNT_UPDATED,
                categoryName = newCategory.name,
                expenseDate = createdExpense.first.expenseDate,
                description = EXPENSE_DESCRIPTION_UPDATED,
            ),
            BotText.EditExpenseFieldSelection,
        )
        val categoryDraft = amountDraft.copy(categoryId = newCategory.id)
        assertThat(categoryUpdated.nextState).isEqualTo(
            UserState.AwaitingExpenseEditFieldSelection(
                expenseId = createdExpense.first.id,
                expenseDraft = categoryDraft,
                categoryName = newCategory.name,
            ),
        )

        val dateSelectionPrompt =
            dialogueRouter.processUserInput(
                userId = userId,
                chatId = chatId,
                text = Buttons.EDIT_EXPENSE_DATE,
            )
        assertThat(
            dateSelectionPrompt.outgoingMessages
                .map { it.text },
        ).containsExactly(
            BotText.ExpenseView(
                amount = EXPENSE_AMOUNT_UPDATED,
                categoryName = newCategory.name,
                expenseDate = createdExpense.first.expenseDate,
                description = EXPENSE_DESCRIPTION_UPDATED,
            ),
            BotText.SelectExpenseDate,
        )
        assertThat(
            dateSelectionPrompt.outgoingMessages
                .last()
                .actions,
        ).containsExactly(BotAction.ShowExpenseDateSelection)
        assertThat(dateSelectionPrompt.nextState).isEqualTo(
            UserState.AwaitingExpenseDateEditSelection(
                expenseId = createdExpense.first.id,
                expenseDraft = categoryDraft,
                categoryName = newCategory.name,
            ),
        )
        val manualDatePrompt =
            dialogueRouter.processUserInput(
                userId = userId,
                chatId = chatId,
                text = Buttons.ENTER_DATE_MANUALLY,
            )
        assertThat(
            manualDatePrompt.outgoingMessages
                .map { it.text },
        ).containsExactly(
            BotText.ExpenseView(
                amount = EXPENSE_AMOUNT_UPDATED,
                categoryName = newCategory.name,
                expenseDate = createdExpense.first.expenseDate,
                description = EXPENSE_DESCRIPTION_UPDATED,
            ),
            BotText.EnterExpenseDateManually,
        )
        assertThat(manualDatePrompt.nextState).isEqualTo(
            UserState.AwaitingExpenseDateEditManualInput(
                expenseId = createdExpense.first.id,
                expenseDraft = categoryDraft,
                categoryName = newCategory.name,
            ),
        )

        val dateUpdated =
            dialogueRouter.processUserInput(
                userId = userId,
                chatId = chatId,
                text = MANUAL_DATE_TEXT,
            )
        val dateDraft = categoryDraft.copy(expenseDate = MANUAL_DATE)
        assertThat(
            dateUpdated.outgoingMessages
                .map { it.text },
        ).containsExactly(
            BotText.ExpenseView(
                amount = EXPENSE_AMOUNT_UPDATED,
                categoryName = newCategory.name,
                expenseDate = MANUAL_DATE,
                description = EXPENSE_DESCRIPTION_UPDATED,
            ),
            BotText.EditExpenseFieldSelection,
        )
        assertThat(dateUpdated.nextState).isEqualTo(
            UserState.AwaitingExpenseEditFieldSelection(
                expenseId = createdExpense.first.id,
                expenseDraft = dateDraft,
                categoryName = newCategory.name,
            ),
        )

        val descriptionPrompt =
            dialogueRouter.processUserInput(
                userId = userId,
                chatId = chatId,
                text = Buttons.EDIT_EXPENSE_DESCRIPTION,
            )
        assertThat(
            descriptionPrompt.outgoingMessages
                .single()
                .text,
        ).isEqualTo(BotText.EnterExpenseDescription)
        assertThat(
            descriptionPrompt.outgoingMessages
                .single()
                .actions,
        ).containsExactly(BotAction.ShowCancel)
        assertThat(descriptionPrompt.nextState).isEqualTo(
            UserState.AwaitingExpenseDescriptionEdit(
                expenseId = createdExpense.first.id,
                expenseDraft = dateDraft,
                categoryName = newCategory.name,
            ),
        )

        val descriptionUpdated =
            dialogueRouter.processUserInput(
                userId = userId,
                chatId = chatId,
                text = DESCRIPTION_UPDATED,
            )
        val finalDraft = dateDraft.copy(description = DESCRIPTION_UPDATED)
        assertThat(
            descriptionUpdated.outgoingMessages
                .map { it.text },
        ).containsExactly(
            BotText.ExpenseView(
                amount = EXPENSE_AMOUNT_UPDATED,
                categoryName = newCategory.name,
                expenseDate = MANUAL_DATE,
                description = DESCRIPTION_UPDATED,
            ),
            BotText.EditExpenseFieldSelection,
        )
        assertThat(descriptionUpdated.nextState).isEqualTo(
            UserState.AwaitingExpenseEditFieldSelection(
                expenseId = createdExpense.first.id,
                expenseDraft = finalDraft,
                categoryName = newCategory.name,
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
        assertThat(editFinished.nextState).isEqualTo(UserState.Idle)
        assertThat(editFinished.expenseCardAction().expenseId).isEqualTo(createdExpense.first.id)

        val saved = expenseById(userId, createdExpense.first.id)
        assertThat(saved.amount).isEqualTo(EXPENSE_AMOUNT_UPDATED)
        assertThat(saved.categoryId).isEqualTo(newCategory.id)
        assertThat(saved.expenseDate).isEqualTo(MANUAL_DATE)
        assertThat(saved.description).isEqualTo(DESCRIPTION_UPDATED)
        assertThat(saved.id).isEqualTo(createdExpense.first.id)
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

        assertThat(
            unavailableMessage.outgoingMessages
                .single()
                .text,
        ).isEqualTo(BotText.ExpenseUnavailable)
        assertThat(
            unavailableMessage.outgoingMessages
                .single()
                .actions,
        ).containsExactly(BotAction.ClearInlineKeyboard)
        assertThat(unavailableMessage.nextState).isEqualTo(UserState.Idle)
        assertThat(
            unavailableMessage.outgoingMessages
                .single()
                .delivery,
        ).isEqualTo(ResponseDelivery.EditMessage(CARD_MESSAGE_ID))
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
        assertThat(invalidFieldResult.outgoingMessages).isNotEmpty
        val firstMessage = invalidFieldResult.outgoingMessages.first()
        assertThat(firstMessage.text).isEqualTo(BotText.EditExpenseFieldSelection)
        assertThat(firstMessage.actions).containsExactly(BotAction.ShowExpenseEditFieldSelection)
        assertThat(invalidFieldResult.nextState).isEqualTo(editFieldSelectionState(createdExpense.first, createdExpense.second))
        assertThat(firstMessage.delivery)
            .isEqualTo(ResponseDelivery.SendNewMessage)
        assertThat(editStart.outgoingMessages).isNotEmpty
        assertThat(
            editStart.outgoingMessages
                .first()
                .delivery,
        ).isEqualTo(ResponseDelivery.EditMessage(CARD_MESSAGE_ID))
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

        assertThat(
            result.outgoingMessages
                .last()
                .actions,
        ).containsExactly(BotAction.ShowExpenseDateSelection)
        assertThat(result.nextState)
            .isEqualTo(
                UserState.AwaitingExpenseDateSelection(
                    expenseDraft = expenseDraft.copy(categoryId = category.id),
                    categoryName = category.name,
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
            expenseId = expense.id,
            expenseDraft = expense.toDraft(),
            categoryName = category.name,
        )

    private fun Expense.toDraft(): ExpenseDraft =
        ExpenseDraft(
            amount = amount,
            description = description,
            categoryId = categoryId,
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
