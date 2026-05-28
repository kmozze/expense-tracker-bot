package me.kmozze.expensetracker.adapter.callback

import java.util.UUID

object CallbackData {
    internal const val MENU_ADD_EXPENSE_VALUE = "menu:add_expense"
    internal const val MENU_VIEW_EXPENSES_VALUE = "menu:view_expenses"
    internal const val MENU_CATEGORIES_VALUE = "menu:categories"
    internal const val MENU_STATISTICS_VALUE = "menu:statistics"
    internal const val EXPENSE_EDIT_PREFIX = "expense:edit:"
    internal const val EXPENSE_DELETE_PREFIX = "expense:delete:"

    fun editExpense(expenseId: UUID): String = "$EXPENSE_EDIT_PREFIX$expenseId"

    fun menuAddExpense(): String = MENU_ADD_EXPENSE_VALUE

    fun menuViewExpenses(): String = MENU_VIEW_EXPENSES_VALUE

    fun menuCategories(): String = MENU_CATEGORIES_VALUE

    fun menuStatistics(): String = MENU_STATISTICS_VALUE

    fun deleteExpense(expenseId: UUID): String = "$EXPENSE_DELETE_PREFIX$expenseId"
}
