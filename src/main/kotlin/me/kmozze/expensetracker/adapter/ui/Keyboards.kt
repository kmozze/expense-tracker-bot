package me.kmozze.expensetracker.adapter.ui

import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardButton
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow

object Keyboards {
    fun mainMenu(): ReplyKeyboardMarkup {
        val rows =
            listOf(
                KeyboardRow(KeyboardButton(Buttons.ADD_EXPENSE)),
                KeyboardRow(KeyboardButton(Buttons.VIEW_EXPENSES)),
                KeyboardRow(KeyboardButton(Buttons.CATEGORIES)),
                KeyboardRow(KeyboardButton(Buttons.STATISTICS)),
            )

        return ReplyKeyboardMarkup(rows).apply {
            resizeKeyboard = true
            isPersistent = true
        }
    }
}
