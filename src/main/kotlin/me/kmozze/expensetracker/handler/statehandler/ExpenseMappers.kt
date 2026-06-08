package me.kmozze.expensetracker.handler.statehandler

import me.kmozze.expensetracker.model.domain.BotText
import me.kmozze.expensetracker.model.domain.ExpenseDraft
import me.kmozze.expensetracker.model.domain.ExpenseDraftCategory
import me.kmozze.expensetracker.model.entity.Expense

internal fun Expense.toExpenseDraft(categoryName: String): ExpenseDraft =
    ExpenseDraft(
        amount = amount,
        description = description,
        category = ExpenseDraftCategory(categoryId = categoryId, name = categoryName),
        expenseDate = expenseDate,
    )

internal fun Expense.toExpenseView(categoryName: String): BotText.ExpenseView =
    BotText.ExpenseView(
        amount = amount,
        categoryName = categoryName,
        expenseDate = expenseDate,
        description = description,
    )

internal fun ExpenseDraft.toExpenseView(): BotText.ExpenseView =
    BotText.ExpenseView(
        amount = amount,
        categoryName = category?.name,
        expenseDate = expenseDate,
        description = description,
    )
