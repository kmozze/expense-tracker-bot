package me.kmozze.expensetracker.adapter.input

import me.kmozze.expensetracker.adapter.ui.Buttons
import me.kmozze.expensetracker.model.domain.UserCommand

object UserCommandParser {
    fun parse(
        text: String?,
        callbackData: String?,
    ): UserCommand =
        when {
            callbackData != null -> CallbackDataParser.parse(callbackData)
            text?.equals("/start", ignoreCase = true) == true -> UserCommand.Start
            text == Buttons.ADD_EXPENSE -> UserCommand.AddExpense
            text == Buttons.VIEW_EXPENSES -> UserCommand.ViewExpenses
            text == Buttons.CATEGORIES -> UserCommand.Categories
            text == Buttons.STATISTICS -> UserCommand.Statistics
            text != null -> UserCommand.PlainText(text)
            else -> UserCommand.Unsupported
        }
}
