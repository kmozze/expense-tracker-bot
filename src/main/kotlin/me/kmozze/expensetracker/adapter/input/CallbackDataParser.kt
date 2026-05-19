package me.kmozze.expensetracker.adapter.input

import me.kmozze.expensetracker.adapter.callback.CallbackData
import me.kmozze.expensetracker.model.domain.UserCommand
import java.util.UUID

object CallbackDataParser {
    fun parse(value: String?): UserCommand =
        when {
            value == CallbackData.CANCEL_VALUE -> UserCommand.Cancel
            value?.startsWith(CallbackData.SELECT_CATEGORY_PREFIX) == true -> parseSelectCategory(value)
            else -> UserCommand.Unsupported
        }

    private fun parseSelectCategory(value: String): UserCommand =
        value
            .removePrefix(CallbackData.SELECT_CATEGORY_PREFIX)
            .let { runCatching { UUID.fromString(it) }.getOrNull() }
            ?.let { UserCommand.SelectCategory(it) }
            ?: UserCommand.InvalidCategorySelection
}
