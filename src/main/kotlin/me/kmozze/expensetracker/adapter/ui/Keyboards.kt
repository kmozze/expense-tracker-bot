package me.kmozze.expensetracker.adapter.ui

import me.kmozze.expensetracker.adapter.callback.CallbackData
import me.kmozze.expensetracker.model.entity.Category
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardRow

object Keyboards {
    fun mainMenu(): InlineKeyboardMarkup {
        val rows =
            listOf(
                menuRow(Buttons.ADD_EXPENSE, CallbackData.menuAddExpense()),
                menuRow(Buttons.VIEW_EXPENSES, CallbackData.menuViewExpenses()),
                menuRow(Buttons.CATEGORIES, CallbackData.menuCategories()),
                menuRow(Buttons.STATISTICS, CallbackData.menuStatistics()),
            )

        return InlineKeyboardMarkup
            .builder()
            .keyboard(rows)
            .build()
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

    private fun menuRow(
        text: String,
        callbackData: String,
    ): InlineKeyboardRow =
        InlineKeyboardRow(
            listOf(
                InlineKeyboardButton
                    .builder()
                    .text(text)
                    .callbackData(callbackData)
                    .build(),
            ),
        )
}
