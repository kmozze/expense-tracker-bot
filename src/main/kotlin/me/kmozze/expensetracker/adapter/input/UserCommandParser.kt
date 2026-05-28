package me.kmozze.expensetracker.adapter.input

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
            text != null -> UserCommand.PlainText(text)
            else -> UserCommand.Unsupported
        }
}
