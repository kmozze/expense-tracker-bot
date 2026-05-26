package me.kmozze.expensetracker.adapter.ui

import me.kmozze.expensetracker.adapter.callback.CallbackData
import me.kmozze.expensetracker.model.entity.Category
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardRow
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

    fun categorySelection(categories: List<Category>): InlineKeyboardMarkup {
        val buttons =
            categories.map { category ->
                InlineKeyboardButton
                    .builder()
                    .text(category.name)
                    .callbackData(CallbackData.selectCategory(category.id))
                    .build()
            }
        val rows: List<InlineKeyboardRow> =
            buttons
                .chunked(2)
                .map { InlineKeyboardRow(it) } +
                listOf(
                    cancelRow(),
                )

        return InlineKeyboardMarkup
            .builder()
            .keyboard(rows)
            .build()
    }

    fun expenseDateSelection(): InlineKeyboardMarkup {
        val rows =
            listOf(
                InlineKeyboardRow(
                    listOf(
                        InlineKeyboardButton
                            .builder()
                            .text(Buttons.TODAY)
                            .callbackData(CallbackData.selectExpenseDateToday())
                            .build(),
                        InlineKeyboardButton
                            .builder()
                            .text(Buttons.YESTERDAY)
                            .callbackData(CallbackData.selectExpenseDateYesterday())
                            .build(),
                    ),
                ),
                InlineKeyboardRow(
                    listOf(
                        InlineKeyboardButton
                            .builder()
                            .text(Buttons.ENTER_DATE_MANUALLY)
                            .callbackData(CallbackData.enterExpenseDateManually())
                            .build(),
                    ),
                ),
                cancelRow(),
            )

        return InlineKeyboardMarkup
            .builder()
            .keyboard(rows)
            .build()
    }

    fun cancel(): InlineKeyboardMarkup =
        InlineKeyboardMarkup
            .builder()
            .keyboard(listOf(cancelRow()))
            .build()

    private fun cancelRow(): InlineKeyboardRow =
        InlineKeyboardRow(
            listOf(
                InlineKeyboardButton
                    .builder()
                    .text(Buttons.CANCEL)
                    .callbackData(CallbackData.cancel())
                    .build(),
            ),
        )
}
