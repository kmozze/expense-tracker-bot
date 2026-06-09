package me.kmozze.expensetracker.adapter.input

import me.kmozze.expensetracker.adapter.callback.CallbackData
import me.kmozze.expensetracker.model.domain.bot.UserCommand
import java.util.UUID

object CallbackDataParser {
    fun parse(value: String?): UserCommand =
        when {
            value == CallbackData.MENU_ADD_EXPENSE_VALUE -> UserCommand.AddExpense
            value == CallbackData.MENU_VIEW_EXPENSES_VALUE -> UserCommand.ViewExpenses
            value == CallbackData.MENU_CATEGORIES_VALUE -> UserCommand.Categories
            value == CallbackData.MENU_STATISTICS_VALUE -> UserCommand.Statistics
            value?.startsWith(CallbackData.EXPENSE_EDIT_PREFIX) == true -> parseExpenseEdit(value)
            value?.startsWith(CallbackData.EXPENSE_DELETE_CONFIRM_PREFIX) == true -> parseExpenseDeletionConfirmation(value)
            value?.startsWith(CallbackData.EXPENSE_DELETE_CANCEL_PREFIX) == true -> parseExpenseDeletionCancellation(value)
            value?.startsWith(CallbackData.EXPENSE_DELETE_PREFIX) == true -> parseExpenseDeletion(value)
            else -> UserCommand.Unsupported
        }

    private fun parseExpenseEdit(value: String): UserCommand =
        value
            .removePrefix(CallbackData.EXPENSE_EDIT_PREFIX)
            .let { runCatching { UUID.fromString(it) }.getOrNull() }
            ?.let { UserCommand.RequestExpenseEdit(it) }
            ?: UserCommand.InvalidExpenseAction

    private fun parseExpenseDeletion(value: String): UserCommand =
        value
            .removePrefix(CallbackData.EXPENSE_DELETE_PREFIX)
            .let { runCatching { UUID.fromString(it) }.getOrNull() }
            ?.let { UserCommand.RequestExpenseDeletion(it) }
            ?: UserCommand.InvalidExpenseAction

    private fun parseExpenseDeletionConfirmation(value: String): UserCommand =
        value
            .removePrefix(CallbackData.EXPENSE_DELETE_CONFIRM_PREFIX)
            .let { runCatching { UUID.fromString(it) }.getOrNull() }
            ?.let { UserCommand.ConfirmExpenseDeletion(it) }
            ?: UserCommand.InvalidExpenseAction

    private fun parseExpenseDeletionCancellation(value: String): UserCommand =
        value
            .removePrefix(CallbackData.EXPENSE_DELETE_CANCEL_PREFIX)
            .let { runCatching { UUID.fromString(it) }.getOrNull() }
            ?.let { UserCommand.CancelExpenseDeletion(it) }
            ?: UserCommand.InvalidExpenseAction
}
