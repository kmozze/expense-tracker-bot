package me.kmozze.expensetracker.handler.statehandler

import me.kmozze.expensetracker.model.domain.BotAction
import me.kmozze.expensetracker.model.domain.BotText
import me.kmozze.expensetracker.model.domain.HandlerResponse
import me.kmozze.expensetracker.model.domain.HandlerResult
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
): HandlerResult {
    val expense = expenseService.findExpenseForUser(userId = userId, expenseId = expenseId) ?: return expenseEditUnavailableResult()

    val category =
        categoryService.findCategoryForUser(
            categoryId = expense.categoryId,
            userId = userId,
        ) ?: return expenseEditUnavailableResult()

    val outgoingMessages =
        mutableListOf<OutgoingMessage>().apply {
            if (prefixText != null) {
                add(OutgoingMessage(text = prefixText, actions = listOf(BotAction.RemoveReplyKeyboard)))
            }

            add(
                OutgoingMessage(
                    text =
                        BotText.ExpenseSaved(
                            amount = expense.amount,
                            categoryName = category.name,
                            expenseDate = expense.expenseDate,
                            description = expense.description,
                        ),
                    actions = listOf(BotAction.ShowExpenseCardActions(expense.id)),
                ),
            )
        }

    return HandlerResult(
        response =
            HandlerResponse(
                outgoingMessages = outgoingMessages,
            ),
        nextState = nextState,
    )
}

internal fun cancelExpenseEdit(
    expenseService: ExpenseService,
    categoryService: CategoryService,
    userId: Long,
    expenseId: UUID,
): HandlerResult =
    buildUpdatedExpenseResult(
        expenseService = expenseService,
        categoryService = categoryService,
        userId = userId,
        expenseId = expenseId,
        nextState = UserState.Idle,
        prefixText = BotText.Done,
    )

internal fun expenseEditUnavailableResult(): HandlerResult =
    HandlerResult(
        response =
            HandlerResponse(
                outgoingMessages =
                    listOf(
                        OutgoingMessage(
                            text = BotText.ExpenseUnavailable,
                            actions =
                                listOf(
                                    BotAction.ClearInlineKeyboard,
                                    BotAction.RemoveReplyKeyboard,
                                ),
                        ),
                    ),
            ),
        nextState = UserState.Idle,
    )
