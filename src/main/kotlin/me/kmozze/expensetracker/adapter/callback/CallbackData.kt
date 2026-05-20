package me.kmozze.expensetracker.adapter.callback

import java.util.UUID

object CallbackData {
    internal const val SELECT_CATEGORY_PREFIX = "select_category:"
    internal const val CANCEL_VALUE = "cancel"

    fun cancel(): String = CANCEL_VALUE

    fun selectCategory(categoryId: UUID): String = "$SELECT_CATEGORY_PREFIX$categoryId"
}
