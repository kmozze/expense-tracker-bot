package me.kmozze.expensetracker.adapter.callback

import java.util.UUID

object CallbackData {
    internal const val SELECT_CATEGORY_PREFIX = "select_category:"
    internal const val SELECT_EXPENSE_DATE_PREFIX = "select_expense_date:"
    internal const val REQUEST_EXPENSE_DELETION_PREFIX = "delete_expense:"
    internal const val CONFIRM_EXPENSE_DELETION_PREFIX = "confirm_delete_expense:"
    internal const val CANCEL_EXPENSE_DELETION_PREFIX = "cancel_delete_expense:"
    internal const val EXPENSE_DATE_TODAY_VALUE = "today"
    internal const val EXPENSE_DATE_YESTERDAY_VALUE = "yesterday"
    internal const val EXPENSE_DATE_MANUAL_VALUE = "manual"
    internal const val CANCEL_VALUE = "cancel"

    fun cancel(): String = CANCEL_VALUE

    fun selectCategory(categoryId: UUID): String = "$SELECT_CATEGORY_PREFIX$categoryId"

    fun selectExpenseDateToday(): String = "$SELECT_EXPENSE_DATE_PREFIX$EXPENSE_DATE_TODAY_VALUE"

    fun selectExpenseDateYesterday(): String = "$SELECT_EXPENSE_DATE_PREFIX$EXPENSE_DATE_YESTERDAY_VALUE"

    fun enterExpenseDateManually(): String = "$SELECT_EXPENSE_DATE_PREFIX$EXPENSE_DATE_MANUAL_VALUE"

    fun requestExpenseDeletion(expenseId: UUID): String = "$REQUEST_EXPENSE_DELETION_PREFIX$expenseId"

    fun confirmExpenseDeletion(expenseId: UUID): String = "$CONFIRM_EXPENSE_DELETION_PREFIX$expenseId"

    fun cancelExpenseDeletion(expenseId: UUID): String = "$CANCEL_EXPENSE_DELETION_PREFIX$expenseId"
}
