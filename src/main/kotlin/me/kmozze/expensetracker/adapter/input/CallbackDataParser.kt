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
            value?.startsWith(CallbackData.EXPENSE_LIST_PERIOD_SELECTION_PREFIX) == true ->
                parseExpenseListFilterCommand(value, CallbackData.EXPENSE_LIST_PERIOD_SELECTION_PREFIX) {
                    UserCommand.RequestExpenseListPeriodSelection(it)
                }
            value?.startsWith(CallbackData.EXPENSE_LIST_CATEGORY_SELECTION_PREFIX) == true ->
                parseExpenseListFilterCommand(value, CallbackData.EXPENSE_LIST_CATEGORY_SELECTION_PREFIX) {
                    UserCommand.RequestExpenseListCategorySelection(it)
                }
            value?.startsWith(CallbackData.EXPENSE_LIST_SELECT_PERIOD_PREFIX) == true ->
                parseExpenseListFilterCommand(value, CallbackData.EXPENSE_LIST_SELECT_PERIOD_PREFIX) {
                    UserCommand.SelectExpenseListPeriod(it)
                }
            value?.startsWith(CallbackData.EXPENSE_LIST_SELECT_CATEGORY_PREFIX) == true ->
                parseExpenseListFilterCommand(value, CallbackData.EXPENSE_LIST_SELECT_CATEGORY_PREFIX) {
                    UserCommand.SelectExpenseListCategory(it)
                }
            value?.startsWith(CallbackData.EXPENSE_LIST_SHOW_PREFIX) == true ->
                parseExpenseListFilterCommand(value, CallbackData.EXPENSE_LIST_SHOW_PREFIX) {
                    UserCommand.ShowExpenseList(filter = it, page = 0)
                }
            value?.startsWith(CallbackData.EXPENSE_LIST_PAGE_PREFIX) == true -> parseExpenseListPage(value)
            value?.startsWith(CallbackData.EXPENSE_LIST_OPEN_PREFIX) == true -> parseExpenseListOpen(value)
            value?.startsWith(CallbackData.EXPENSE_LIST_PREFIX) == true -> UserCommand.InvalidExpenseListAction
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

    private fun parseExpenseListFilterCommand(
        value: String,
        prefix: String,
        commandFactory: (me.kmozze.expensetracker.model.domain.expense.ExpenseListFilter) -> UserCommand,
    ): UserCommand {
        val parts = value.removePrefix(prefix).split(":")
        if (parts.size != 2) {
            return UserCommand.InvalidExpenseListAction
        }

        return CallbackData
            .filterFromTokens(periodCode = parts[0], categoryToken = parts[1])
            ?.let(commandFactory)
            ?: UserCommand.InvalidExpenseListAction
    }

    private fun parseExpenseListPage(value: String): UserCommand {
        val parts = value.removePrefix(CallbackData.EXPENSE_LIST_PAGE_PREFIX).split(":")
        if (parts.size != 3) {
            return UserCommand.InvalidExpenseListAction
        }

        val filter =
            CallbackData.filterFromTokens(
                periodCode = parts[0],
                categoryToken = parts[1],
            ) ?: return UserCommand.InvalidExpenseListAction
        val page =
            parts[2]
                .toIntOrNull()
                ?.takeIf { it in 0..MAX_EXPENSE_LIST_PAGE }
                ?: return UserCommand.InvalidExpenseListAction

        return UserCommand.ShowExpenseList(
            filter = filter,
            page = page,
            shouldEditCurrentMessage = true,
        )
    }

    private fun parseExpenseListOpen(value: String): UserCommand =
        value
            .removePrefix(CallbackData.EXPENSE_LIST_OPEN_PREFIX)
            .let { runCatching { UUID.fromString(it) }.getOrNull() }
            ?.let { UserCommand.OpenExpenseFromList(it) }
            ?: UserCommand.InvalidExpenseListAction

    private const val MAX_EXPENSE_LIST_PAGE = 100_000
}
