package me.kmozze.expensetracker.integration.flow

import me.kmozze.expensetracker.adapter.callback.CallbackData
import me.kmozze.expensetracker.adapter.ui.Buttons
import me.kmozze.expensetracker.handler.DialogueRouter
import me.kmozze.expensetracker.model.domain.BotAction
import me.kmozze.expensetracker.model.domain.BotText
import me.kmozze.expensetracker.model.domain.ExpenseDraft
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

        assertThat(editStart.response.outgoingMessages[0].text).isEqualTo(
            BotText.ExpenseView(
                amount = createdExpense.first.amount,
                categoryName = createdExpense.second.name,
                expenseDate = createdExpense.first.expenseDate,
                description = createdExpense.first.description,
            ),
        )
        assertThat(editStart.response.outgoingMessages[0].actions).containsExactly(BotAction.ClearInlineKeyboard)
        assertThat(editStart.response.outgoingMessages[0].delivery).isEqualTo(ResponseDelivery.EditMessage(CARD_MESSAGE_ID))
        assertThat(editStart.response.outgoingMessages[1].text).isEqualTo(BotText.EditExpenseFieldSelection)
        assertThat(editStart.response.outgoingMessages[1].actions).containsExactly(BotAction.ShowExpenseEditFieldSelection)
        assertThat(editStart.nextState).isEqualTo(UserState.AwaitingExpenseEditFieldSelection(createdExpense.first.id))

        val amountPrompt =
            dialogueRouter.processUserInput(
                userId = userId,
                chatId = chatId,
                text = Buttons.EDIT_EXPENSE_AMOUNT,
            )
        assertThat(
            amountPrompt.response.outgoingMessages
                .single()
                .text,
        ).isEqualTo(BotText.EnterExpenseAmount)
        assertThat(
            amountPrompt.response.outgoingMessages
                .single()
                .actions,
        ).containsExactly(BotAction.ShowCancel)
        assertThat(amountPrompt.nextState).isEqualTo(UserState.AwaitingExpenseAmountEdit(expenseId = createdExpense.first.id))

        val amountUpdated =
            dialogueRouter.processUserInput(
                userId = userId,
                chatId = chatId,
                text = "650",
            )
        assertThat(amountUpdated.response.outgoingMessages).hasSize(2)
        assertThat(
            amountUpdated.response.outgoingMessages
                .first()
                .text,
        ).isEqualTo(BotText.ExpenseSaved)
        assertThat(
            amountUpdated.response.outgoingMessages
                .first()
                .actions,
        ).containsExactly(BotAction.RemoveReplyKeyboard)
        val amountSaved = amountUpdated.savedExpenseMessage()
        assertThat(amountSaved.amount).isEqualTo(EXPENSE_AMOUNT_UPDATED)
        assertThat(amountSaved.categoryName).isEqualTo(initialCategory.name)
        assertThat(amountUpdated.nextState).isEqualTo(UserState.Idle)
        assertThat(amountUpdated.expenseCardAction().expenseId).isEqualTo(createdExpense.first.id)

        val editAfterAmount =
            requestEdit(userId = userId, chatId = chatId, expenseId = createdExpense.first.id, callbackMessageId = CARD_MESSAGE_ID)
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
        val categoryUpdated =
            dialogueRouter.processUserInput(
                userId = userId,
                chatId = chatId,
                text = newCategory.name,
            )
        val categorySaved = categoryUpdated.savedExpenseMessage()
        assertThat(categorySaved.categoryName).isEqualTo(newCategory.name)
        assertThat(categorySaved.amount).isEqualTo(EXPENSE_AMOUNT_UPDATED)
        assertThat(categoryUpdated.nextState).isEqualTo(UserState.Idle)

        val editAfterCategory =
            requestEdit(userId = userId, chatId = chatId, expenseId = createdExpense.first.id, callbackMessageId = CARD_MESSAGE_ID)
        val dateSelectionPrompt =
            dialogueRouter.processUserInput(
                userId = userId,
                chatId = chatId,
                text = Buttons.EDIT_EXPENSE_DATE,
            )
        assertThat(
            dateSelectionPrompt.response.outgoingMessages
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
            dateSelectionPrompt.response.outgoingMessages
                .last()
                .actions,
        ).containsExactly(BotAction.ShowExpenseDateSelection)
        val manualDatePrompt =
            dialogueRouter.processUserInput(
                userId = userId,
                chatId = chatId,
                text = Buttons.ENTER_DATE_MANUALLY,
            )
        assertThat(
            manualDatePrompt.response.outgoingMessages
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
            ),
        )

        val dateUpdated =
            dialogueRouter.processUserInput(
                userId = userId,
                chatId = chatId,
                text = MANUAL_DATE_TEXT,
            )
        val dateSaved = dateUpdated.savedExpenseMessage()
        assertThat(dateSaved.expenseDate).isEqualTo(MANUAL_DATE)
        assertThat(dateUpdated.nextState).isEqualTo(UserState.Idle)

        val editAfterDate =
            requestEdit(userId = userId, chatId = chatId, expenseId = createdExpense.first.id, callbackMessageId = CARD_MESSAGE_ID)
        val descriptionPrompt =
            dialogueRouter.processUserInput(
                userId = userId,
                chatId = chatId,
                text = Buttons.EDIT_EXPENSE_DESCRIPTION,
            )
        assertThat(
            descriptionPrompt.response.outgoingMessages
                .single()
                .text,
        ).isEqualTo(BotText.EnterExpenseDescription)
        assertThat(
            descriptionPrompt.response.outgoingMessages
                .single()
                .actions,
        ).containsExactly(BotAction.ShowCancel)

        val descriptionUpdated =
            dialogueRouter.processUserInput(
                userId = userId,
                chatId = chatId,
                text = DESCRIPTION_UPDATED,
            )
        val descriptionSaved = descriptionUpdated.savedExpenseMessage()
        assertThat(descriptionSaved.description).isEqualTo(DESCRIPTION_UPDATED)
        assertThat(descriptionSaved.expenseDate).isEqualTo(MANUAL_DATE)
        assertThat(descriptionSaved.categoryName).isEqualTo(newCategory.name)
        assertThat(descriptionUpdated.nextState).isEqualTo(UserState.Idle)

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
            unavailableMessage.response.outgoingMessages
                .single()
                .text,
        ).isEqualTo(BotText.ExpenseUnavailable)
        assertThat(
            unavailableMessage.response.outgoingMessages
                .single()
                .actions,
        ).containsExactly(BotAction.ClearInlineKeyboard)
        assertThat(unavailableMessage.nextState).isEqualTo(UserState.Idle)
        assertThat(
            unavailableMessage.response.outgoingMessages
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
        assertThat(invalidFieldResult.response.outgoingMessages).isNotEmpty
        val firstMessage = invalidFieldResult.response.outgoingMessages.first()
        assertThat(firstMessage.text).isEqualTo(BotText.EditExpenseFieldSelection)
        assertThat(firstMessage.actions).containsExactly(BotAction.ShowExpenseEditFieldSelection)
        assertThat(invalidFieldResult.nextState).isEqualTo(UserState.AwaitingExpenseEditFieldSelection(createdExpense.first.id))
        assertThat(firstMessage.delivery)
            .isEqualTo(ResponseDelivery.SendNewMessage)
        assertThat(editStart.response.outgoingMessages).isNotEmpty
        assertThat(
            editStart.response.outgoingMessages
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
        assertThat(savedResult.response.outgoingMessages).hasSize(2)
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
    ): me.kmozze.expensetracker.model.domain.HandlerResult =
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
    ): me.kmozze.expensetracker.model.domain.HandlerResult {
        val result =
            dialogueRouter.processUserInput(
                userId = userId,
                chatId = chatId,
                text = category.name,
            )

        assertThat(
            result.response.outgoingMessages
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

    private fun me.kmozze.expensetracker.model.domain.HandlerResult.categorySelectionAction(): BotAction.ShowCategorySelection =
        response.outgoingMessages
            .last()
            .actions
            .single() as BotAction.ShowCategorySelection

    private fun me.kmozze.expensetracker.model.domain.HandlerResult.savedExpenseMessage(): BotText.ExpenseView {
        assertThat(response.outgoingMessages).hasSize(2)
        return (response.outgoingMessages[1].text as BotText.ExpenseView)
    }

    private fun me.kmozze.expensetracker.model.domain.HandlerResult.expenseCardAction(): BotAction.ShowExpenseCardActions =
        response.outgoingMessages[1].actions.single() as BotAction.ShowExpenseCardActions

    private data class CategorySelection(
        val result: me.kmozze.expensetracker.model.domain.HandlerResult,
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
