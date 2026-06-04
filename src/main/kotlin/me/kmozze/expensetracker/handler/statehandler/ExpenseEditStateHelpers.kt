package me.kmozze.expensetracker.handler.statehandler

import me.kmozze.expensetracker.handler.handlerResponse
import me.kmozze.expensetracker.handler.outgoingMessage
import me.kmozze.expensetracker.model.domain.BotAction
import me.kmozze.expensetracker.model.domain.BotText
import me.kmozze.expensetracker.model.domain.ExpenseDraft
import me.kmozze.expensetracker.model.domain.HandlerResponse
import me.kmozze.expensetracker.model.domain.OutgoingMessage
import me.kmozze.expensetracker.model.domain.UserState
import me.kmozze.expensetracker.model.entity.Category
import me.kmozze.expensetracker.model.entity.Expense
import me.kmozze.expensetracker.service.CategoryService
import me.kmozze.expensetracker.service.ExpenseService
import java.time.LocalDate
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
                    text = expense.toExpenseView(category),
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

internal fun Expense.toExpenseView(category: Category): BotText.ExpenseView =
    BotText.ExpenseView(
        amount = amount,
        categoryName = category.name,
        expenseDate = expenseDate,
        description = description,
    )

internal fun ExpenseDraft.toExpenseView(
    categoryName: String? = null,
    expenseDate: LocalDate? = this.expenseDate,
): BotText.ExpenseView =
    BotText.ExpenseView(
        amount = amount,
        categoryName = categoryName,
        expenseDate = expenseDate,
        description = description,
    )

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
