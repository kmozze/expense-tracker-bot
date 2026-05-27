package me.kmozze.expensetracker.adapter.input

import me.kmozze.expensetracker.adapter.callback.CallbackData
import me.kmozze.expensetracker.model.domain.ExpenseDateSelection
import me.kmozze.expensetracker.model.domain.UserCommand
import java.util.UUID

object CallbackDataParser {
    fun parse(value: String?): UserCommand =
        when {
            value == CallbackData.MENU_ADD_EXPENSE_VALUE -> UserCommand.AddExpense
            value == CallbackData.MENU_VIEW_EXPENSES_VALUE -> UserCommand.ViewExpenses
            value == CallbackData.MENU_CATEGORIES_VALUE -> UserCommand.Categories
            value == CallbackData.MENU_STATISTICS_VALUE -> UserCommand.Statistics
            value == CallbackData.CANCEL_VALUE -> UserCommand.Cancel
            value?.startsWith(CallbackData.SELECT_CATEGORY_PREFIX) == true -> parseSelectCategory(value)
            value?.startsWith(CallbackData.SELECT_EXPENSE_DATE_PREFIX) == true -> parseSelectExpenseDate(value)
            else -> UserCommand.Unsupported
        }

    private fun parseSelectCategory(value: String): UserCommand =
        value
            .removePrefix(CallbackData.SELECT_CATEGORY_PREFIX)
            .let { runCatching { UUID.fromString(it) }.getOrNull() }
            ?.let { UserCommand.SelectCategory(it) }
            ?: UserCommand.InvalidCategorySelection

    private fun parseSelectExpenseDate(value: String): UserCommand =
        when (value.removePrefix(CallbackData.SELECT_EXPENSE_DATE_PREFIX)) {
            CallbackData.EXPENSE_DATE_TODAY_VALUE -> UserCommand.SelectExpenseDate(ExpenseDateSelection.TODAY)
            CallbackData.EXPENSE_DATE_YESTERDAY_VALUE -> UserCommand.SelectExpenseDate(ExpenseDateSelection.YESTERDAY)
            CallbackData.EXPENSE_DATE_MANUAL_VALUE -> UserCommand.SelectExpenseDate(ExpenseDateSelection.MANUAL)
            else -> UserCommand.InvalidExpenseDateSelection
        }
}
