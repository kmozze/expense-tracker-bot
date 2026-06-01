package me.kmozze.expensetracker.adapter.input

import me.kmozze.expensetracker.adapter.ui.Buttons
import me.kmozze.expensetracker.model.domain.ExpenseDateChoice
import me.kmozze.expensetracker.model.domain.ExpenseEditField
import me.kmozze.expensetracker.model.domain.UserCommand

object UserCommandParser {
    fun parse(
        text: String?,
        callbackData: String?,
    ): UserCommand =
        when {
            callbackData != null -> CallbackDataParser.parse(callbackData)
            text?.equals("/start", ignoreCase = true) == true -> UserCommand.Start
            text?.equals("/menu", ignoreCase = true) == true -> UserCommand.Menu
            text == Buttons.CANCEL -> UserCommand.Cancel
            text == Buttons.TODAY -> UserCommand.SelectExpenseDate(ExpenseDateChoice.Today)
            text == Buttons.YESTERDAY -> UserCommand.SelectExpenseDate(ExpenseDateChoice.Yesterday)
            text == Buttons.ENTER_DATE_MANUALLY -> UserCommand.SelectExpenseDate(ExpenseDateChoice.ManualInput)
            text == Buttons.EDIT_EXPENSE_AMOUNT -> UserCommand.SelectExpenseEditField(ExpenseEditField.Amount)
            text == Buttons.EDIT_EXPENSE_CATEGORY -> UserCommand.SelectExpenseEditField(ExpenseEditField.Category)
            text == Buttons.EDIT_EXPENSE_DATE -> UserCommand.SelectExpenseEditField(ExpenseEditField.Date)
            text == Buttons.EDIT_EXPENSE_DESCRIPTION -> UserCommand.SelectExpenseEditField(ExpenseEditField.Description)
            text != null -> UserCommand.PlainText(text)
            else -> UserCommand.Unsupported
        }
}
