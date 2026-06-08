package me.kmozze.expensetracker.handler.statehandler

import me.kmozze.expensetracker.handler.handlerResponse
import me.kmozze.expensetracker.handler.outgoingMessage
import me.kmozze.expensetracker.model.domain.BotAction
import me.kmozze.expensetracker.model.domain.BotText
import me.kmozze.expensetracker.model.domain.ExpenseDraft
import me.kmozze.expensetracker.model.domain.ExpenseEditSession
import me.kmozze.expensetracker.model.domain.HandlerResponse
import me.kmozze.expensetracker.model.domain.OutgoingMessage
import me.kmozze.expensetracker.model.domain.UserState
import me.kmozze.expensetracker.service.CategoryService
import me.kmozze.expensetracker.service.ExpenseService
import java.util.UUID

internal fun buildUpdatedExpenseResult(
    expenseService: ExpenseService,
    categoryService: CategoryService,
    userId: Long,
    expenseId: UUID,
    nextState: UserState,
    prefixText: BotText?,
): HandlerResponse {
    val expense = expenseService.findExpenseForUser(userId = userId, expenseId = expenseId) ?: return expenseEditUnavailableResult()

    val category =
        categoryService.findCategoryForUser(
            categoryId = expense.categoryId,
            userId = userId,
        ) ?: return expenseEditUnavailableResult()

    val outgoingMessages =
        mutableListOf<OutgoingMessage>().apply {
            if (prefixText != null) {
                add(outgoingMessage(text = prefixText, actions = listOf(BotAction.RemoveReplyKeyboard)))
            }

            add(
                outgoingMessage(
                    text = expense.toExpenseView(category.name),
                    actions = listOf(BotAction.ShowExpenseCardActions(expense.id)),
                ),
            )
        }

    return handlerResponse(
        messages = outgoingMessages,
        nextState = nextState,
    )
}

internal fun cancelExpenseEdit(
    expenseService: ExpenseService,
    categoryService: CategoryService,
    userId: Long,
    expenseId: UUID,
): HandlerResponse =
    buildUpdatedExpenseResult(
        expenseService = expenseService,
        categoryService = categoryService,
        userId = userId,
        expenseId = expenseId,
        nextState = UserState.Idle,
        prefixText = BotText.Done,
    )

internal fun buildDraftExpenseEditFieldSelectionResult(editSession: ExpenseEditSession): HandlerResponse =
    handlerResponse(
        messages =
            listOf(
                outgoingMessage(
                    text = editSession.expenseDraft.toExpenseView(),
                    actions = emptyList(),
                ),
                outgoingMessage(
                    text = BotText.EditExpenseFieldSelection,
                    actions = listOf(BotAction.ShowExpenseEditFieldSelection),
                ),
            ),
        nextState =
            UserState.AwaitingExpenseEditFieldSelection(
                editSession = editSession,
            ),
    )

internal fun ExpenseEditSession.withExpenseDraft(expenseDraft: ExpenseDraft): ExpenseEditSession = copy(expenseDraft = expenseDraft)

internal fun finishExpenseEdit(
    expenseService: ExpenseService,
    categoryService: CategoryService,
    userId: Long,
    editSession: ExpenseEditSession,
): HandlerResponse {
    val updatedExpense =
        expenseService.updateExpenseFromDraftForUser(
            userId = userId,
            expenseId = editSession.expenseId,
            expenseDraft = editSession.expenseDraft,
        ) ?: return expenseEditUnavailableResult()

    val category =
        categoryService.findCategoryForUser(
            categoryId = updatedExpense.categoryId,
            userId = userId,
        ) ?: return expenseEditUnavailableResult()

    return handlerResponse(
        messages =
            listOf(
                outgoingMessage(
                    text = BotText.ExpenseSaved,
                    actions = listOf(BotAction.RemoveReplyKeyboard),
                ),
                outgoingMessage(
                    text = updatedExpense.toExpenseView(category.name),
                    actions = listOf(BotAction.ShowExpenseCardActions(updatedExpense.id)),
                ),
            ),
        nextState = UserState.Idle,
    )
}

internal fun expenseEditUnavailableResult(): HandlerResponse =
    handlerResponse(
        message =
            outgoingMessage(
                text = BotText.ExpenseUnavailable,
                actions =
                    listOf(
                        BotAction.ClearInlineKeyboard,
                        BotAction.RemoveReplyKeyboard,
                    ),
            ),
        nextState = UserState.Idle,
    )
