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
            text?.equals("/menu", ignoreCase = true) == true -> UserCommand.Menu
            text == Buttons.CANCEL -> UserCommand.Cancel
            text != null -> UserCommand.PlainText(text)
            else -> UserCommand.Unsupported
        }
}
