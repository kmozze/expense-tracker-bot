package me.kmozze.expensetracker.adapter.ui

import me.kmozze.expensetracker.adapter.callback.CallbackData
import me.kmozze.expensetracker.model.entity.Category
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardRow
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardButton
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow
import java.util.UUID

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

    fun categorySelection(categories: List<Category>): ReplyKeyboardMarkup {
        val rows =
            categories
                .chunked(2)
                .map { categoriesChunk ->
                    KeyboardRow(categoriesChunk.map { KeyboardButton(it.name) })
                } +
                listOf(cancelReplyRow())

        return dialogKeyboard(rows)
    }

    fun expenseDateSelection(): ReplyKeyboardMarkup {
        val rows =
            listOf(
                KeyboardRow(
                    listOf(
                        KeyboardButton(Buttons.TODAY),
                        KeyboardButton(Buttons.YESTERDAY),
                    ),
                ),
                KeyboardRow(KeyboardButton(Buttons.ENTER_DATE_MANUALLY)),
                cancelReplyRow(),
            )

        return dialogKeyboard(rows)
    }

    fun cancel(): ReplyKeyboardMarkup = dialogKeyboard(listOf(cancelReplyRow()))

    fun expenseCardActions(expenseId: UUID): InlineKeyboardMarkup =
        InlineKeyboardMarkup
            .builder()
            .keyboard(
                listOf(
                    InlineKeyboardRow(
                        listOf(
                            InlineKeyboardButton
                                .builder()
                                .text(Buttons.EDIT_EXPENSE)
                                .callbackData(CallbackData.editExpense(expenseId))
                                .build(),
                            InlineKeyboardButton
                                .builder()
                                .text(Buttons.DELETE_EXPENSE)
                                .callbackData(CallbackData.deleteExpense(expenseId))
                                .build(),
                        ),
                    ),
                ),
            ).build()

    fun expenseDeletionConfirmation(expenseId: UUID): InlineKeyboardMarkup =
        InlineKeyboardMarkup
            .builder()
            .keyboard(
                listOf(
                    InlineKeyboardRow(
                        listOf(
                            InlineKeyboardButton
                                .builder()
                                .text(Buttons.CONFIRM_DELETE_EXPENSE)
                                .callbackData(CallbackData.confirmExpenseDeletion(expenseId))
                                .build(),
                            InlineKeyboardButton
                                .builder()
                                .text(Buttons.CANCEL_DELETE_EXPENSE)
                                .callbackData(CallbackData.cancelExpenseDeletion(expenseId))
                                .build(),
                        ),
                    ),
                ),
            ).build()

    fun expenseEditFieldSelection(): ReplyKeyboardMarkup {
        val rows =
            listOf(
                KeyboardRow(
                    listOf(
                        KeyboardButton(Buttons.EDIT_EXPENSE_AMOUNT),
                        KeyboardButton(Buttons.EDIT_EXPENSE_CATEGORY),
                    ),
                ),
                KeyboardRow(
                    listOf(
                        KeyboardButton(Buttons.EDIT_EXPENSE_DATE),
                        KeyboardButton(Buttons.EDIT_EXPENSE_DESCRIPTION),
                    ),
                ),
                cancelReplyRow(),
            )

        return dialogKeyboard(rows)
    }

    private fun cancelReplyRow(): KeyboardRow = KeyboardRow(KeyboardButton(Buttons.CANCEL))

    private fun dialogKeyboard(rows: List<KeyboardRow>): ReplyKeyboardMarkup =
        ReplyKeyboardMarkup(rows).apply {
            resizeKeyboard = true
        }

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
