package me.kmozze.expensetracker.adapter.callback

import java.util.UUID

object CallbackData {
    internal const val SELECT_CATEGORY_PREFIX = "select_category:"
    internal const val SELECT_EXPENSE_DATE_PREFIX = "select_expense_date:"
    internal const val EXPENSE_DATE_TODAY_VALUE = "today"
    internal const val EXPENSE_DATE_YESTERDAY_VALUE = "yesterday"
    internal const val EXPENSE_DATE_MANUAL_VALUE = "manual"
    internal const val CANCEL_VALUE = "cancel"

    fun cancel(): String = CANCEL_VALUE

    fun selectCategory(categoryId: UUID): String = "$SELECT_CATEGORY_PREFIX$categoryId"

    fun selectExpenseDateToday(): String = "$SELECT_EXPENSE_DATE_PREFIX$EXPENSE_DATE_TODAY_VALUE"

    fun selectExpenseDateYesterday(): String = "$SELECT_EXPENSE_DATE_PREFIX$EXPENSE_DATE_YESTERDAY_VALUE"

    fun enterExpenseDateManually(): String = "$SELECT_EXPENSE_DATE_PREFIX$EXPENSE_DATE_MANUAL_VALUE"
}
