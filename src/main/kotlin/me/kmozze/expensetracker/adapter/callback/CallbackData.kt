package me.kmozze.expensetracker.adapter.callback

import me.kmozze.expensetracker.model.domain.expense.ExpenseListFilter
import me.kmozze.expensetracker.model.domain.expense.ExpenseListPeriod
import java.util.UUID

object CallbackData {
    internal const val MENU_ADD_EXPENSE_VALUE = "menu:add_expense"
    internal const val MENU_VIEW_EXPENSES_VALUE = "menu:view_expenses"
    internal const val MENU_CATEGORIES_VALUE = "menu:categories"
    internal const val MENU_STATISTICS_VALUE = "menu:statistics"
    internal const val EXPENSE_EDIT_PREFIX = "expense:edit:"
    internal const val EXPENSE_DELETE_CONFIRM_PREFIX = "expense:delete:confirm:"
    internal const val EXPENSE_DELETE_CANCEL_PREFIX = "expense:delete:cancel:"
    internal const val EXPENSE_DELETE_PREFIX = "expense:delete:"
    internal const val EXPENSE_LIST_PREFIX = "el:"
    internal const val EXPENSE_LIST_PERIOD_SELECTION_PREFIX = "el:p:"
    internal const val EXPENSE_LIST_CATEGORY_SELECTION_PREFIX = "el:c:"
    internal const val EXPENSE_LIST_SELECT_PERIOD_PREFIX = "el:sp:"
    internal const val EXPENSE_LIST_SELECT_CATEGORY_PREFIX = "el:sc:"
    internal const val EXPENSE_LIST_SHOW_PREFIX = "el:s:"
    internal const val EXPENSE_LIST_PAGE_PREFIX = "el:g:"
    internal const val EXPENSE_LIST_OPEN_PREFIX = "el:o:"

    internal const val EXPENSE_LIST_CATEGORY_ALL_TOKEN = "a"

    fun editExpense(expenseId: UUID): String = "$EXPENSE_EDIT_PREFIX$expenseId"

    fun menuAddExpense(): String = MENU_ADD_EXPENSE_VALUE

    fun menuViewExpenses(): String = MENU_VIEW_EXPENSES_VALUE

    fun menuCategories(): String = MENU_CATEGORIES_VALUE

    fun menuStatistics(): String = MENU_STATISTICS_VALUE

    fun deleteExpense(expenseId: UUID): String = "$EXPENSE_DELETE_PREFIX$expenseId"

    fun confirmExpenseDeletion(expenseId: UUID): String = "$EXPENSE_DELETE_CONFIRM_PREFIX$expenseId"

    fun cancelExpenseDeletion(expenseId: UUID): String = "$EXPENSE_DELETE_CANCEL_PREFIX$expenseId"

    fun requestExpenseListPeriodSelection(filter: ExpenseListFilter): String = "$EXPENSE_LIST_PERIOD_SELECTION_PREFIX${filter.toToken()}"

    fun requestExpenseListCategorySelection(filter: ExpenseListFilter): String =
        "$EXPENSE_LIST_CATEGORY_SELECTION_PREFIX${filter.toToken()}"

    fun selectExpenseListPeriod(filter: ExpenseListFilter): String = "$EXPENSE_LIST_SELECT_PERIOD_PREFIX${filter.toToken()}"

    fun selectExpenseListCategory(filter: ExpenseListFilter): String = "$EXPENSE_LIST_SELECT_CATEGORY_PREFIX${filter.toToken()}"

    fun showExpenseList(filter: ExpenseListFilter): String = "$EXPENSE_LIST_SHOW_PREFIX${filter.toToken()}"

    fun expenseListPage(
        filter: ExpenseListFilter,
        page: Int,
    ): String = "$EXPENSE_LIST_PAGE_PREFIX${filter.toToken()}:$page"

    fun openExpenseFromList(expenseId: UUID): String = "$EXPENSE_LIST_OPEN_PREFIX$expenseId"

    private fun ExpenseListFilter.toToken(): String = "${period.code}:${categoryId?.toString() ?: EXPENSE_LIST_CATEGORY_ALL_TOKEN}"

    internal fun filterFromTokens(
        periodCode: String,
        categoryToken: String,
    ): ExpenseListFilter? {
        val period = ExpenseListPeriod.fromCode(periodCode) ?: return null
        val categoryId =
            when (categoryToken) {
                EXPENSE_LIST_CATEGORY_ALL_TOKEN -> null
                else -> runCatching { UUID.fromString(categoryToken) }.getOrNull() ?: return null
            }

        return ExpenseListFilter(
            period = period,
            categoryId = categoryId,
        )
    }
}
