package me.kmozze.expensetracker.adapter.ui

import me.kmozze.expensetracker.adapter.callback.CallbackData
import me.kmozze.expensetracker.model.domain.expense.ExpenseListCategoryOption
import me.kmozze.expensetracker.model.domain.expense.ExpenseListFilter
import me.kmozze.expensetracker.model.domain.expense.ExpenseListItem
import me.kmozze.expensetracker.model.domain.expense.ExpenseListPage
import me.kmozze.expensetracker.model.domain.expense.ExpenseListPeriod
import me.kmozze.expensetracker.model.domain.expense.Money
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardRow
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardButton
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow
import java.math.BigDecimal
import java.time.LocalDate
import java.time.format.DateTimeFormatter
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

    fun categorySelection(categoryNames: List<String>): ReplyKeyboardMarkup {
        val rows =
            categoryNames
                .chunked(2)
                .map { categoryNamesChunk ->
                    KeyboardRow(categoryNamesChunk.map { KeyboardButton(it) })
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

    fun expenseListSettings(filter: ExpenseListFilter): InlineKeyboardMarkup =
        InlineKeyboardMarkup
            .builder()
            .keyboard(
                listOf(
                    InlineKeyboardRow(
                        listOf(
                            inlineButton(
                                text = Buttons.CHANGE_EXPENSE_LIST_PERIOD,
                                callbackData = CallbackData.requestExpenseListPeriodSelection(filter),
                            ),
                            inlineButton(
                                text = Buttons.CHANGE_EXPENSE_LIST_CATEGORY,
                                callbackData = CallbackData.requestExpenseListCategorySelection(filter),
                            ),
                        ),
                    ),
                    InlineKeyboardRow(
                        listOf(
                            inlineButton(
                                text = Buttons.SHOW_EXPENSE_LIST,
                                callbackData = CallbackData.showExpenseList(filter),
                            ),
                        ),
                    ),
                ),
            ).build()

    fun expenseListPeriodSelection(filter: ExpenseListFilter): InlineKeyboardMarkup =
        InlineKeyboardMarkup
            .builder()
            .keyboard(
                listOf(
                    InlineKeyboardRow(
                        listOf(
                            expenseListPeriodButton(Buttons.EXPENSE_LIST_PERIOD_DAY, ExpenseListPeriod.Day, filter),
                            expenseListPeriodButton(Buttons.EXPENSE_LIST_PERIOD_WEEK, ExpenseListPeriod.Week, filter),
                        ),
                    ),
                    InlineKeyboardRow(
                        listOf(
                            expenseListPeriodButton(Buttons.EXPENSE_LIST_PERIOD_MONTH, ExpenseListPeriod.Month, filter),
                            expenseListPeriodButton(Buttons.EXPENSE_LIST_PERIOD_ALL_TIME, ExpenseListPeriod.AllTime, filter),
                        ),
                    ),
                ),
            ).build()

    fun expenseListCategorySelection(
        filter: ExpenseListFilter,
        categories: List<ExpenseListCategoryOption>,
    ): InlineKeyboardMarkup {
        val categoryButtons =
            listOf(
                inlineButton(
                    text = Buttons.EXPENSE_LIST_CATEGORY_ALL,
                    callbackData = CallbackData.selectExpenseListCategory(filter.copy(categoryId = null)),
                ),
            ) +
                categories.map { category ->
                    inlineButton(
                        text = category.name,
                        callbackData = CallbackData.selectExpenseListCategory(filter.copy(categoryId = category.categoryId)),
                    )
                }

        return InlineKeyboardMarkup
            .builder()
            .keyboard(categoryButtons.chunked(2).map { InlineKeyboardRow(it) })
            .build()
    }

    fun expenseListPage(page: ExpenseListPage): InlineKeyboardMarkup {
        val expenseRows =
            page.items.map { item ->
                InlineKeyboardRow(
                    listOf(
                        inlineButton(
                            text = item.rowText(),
                            callbackData = CallbackData.openExpenseFromList(item.expenseId),
                        ),
                    ),
                )
            }

        val paginationButtons =
            buildList {
                if (page.hasPreviousPage) {
                    add(
                        inlineButton(
                            text = Buttons.EXPENSE_LIST_PREVIOUS,
                            callbackData = CallbackData.expenseListPage(page.filter, page.page - 1),
                        ),
                    )
                }
                if (page.hasNextPage) {
                    add(
                        inlineButton(
                            text = Buttons.EXPENSE_LIST_NEXT,
                            callbackData = CallbackData.expenseListPage(page.filter, page.page + 1),
                        ),
                    )
                }
            }

        val rows =
            if (paginationButtons.isEmpty()) {
                expenseRows
            } else {
                expenseRows + listOf(InlineKeyboardRow(paginationButtons))
            }

        return InlineKeyboardMarkup
            .builder()
            .keyboard(rows)
            .build()
    }

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
                KeyboardRow(
                    listOf(
                        KeyboardButton(Buttons.FINISH_EXPENSE_EDIT),
                        KeyboardButton(Buttons.CANCEL),
                    ),
                ),
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
                inlineButton(text, callbackData),
            ),
        )

    private fun expenseListPeriodButton(
        text: String,
        period: ExpenseListPeriod,
        filter: ExpenseListFilter,
    ): InlineKeyboardButton =
        inlineButton(
            text = text,
            callbackData = CallbackData.selectExpenseListPeriod(filter.copy(period = period)),
        )

    private fun inlineButton(
        text: String,
        callbackData: String,
    ): InlineKeyboardButton =
        InlineKeyboardButton
            .builder()
            .text(text)
            .callbackData(callbackData)
            .build()

    private fun ExpenseListItem.rowText(): String = "${expenseDate.formatForList()} · $categoryName · ${amount.formatForList()} ₽"

    private fun LocalDate.formatForList(): String = format(EXPENSE_LIST_DATE_FORMATTER)

    private fun Money.formatForList(): String =
        if (value.remainder(BigDecimal.ONE).compareTo(BigDecimal.ZERO) == 0) {
            value.setScale(0).toPlainString()
        } else {
            format()
        }

    private val EXPENSE_LIST_DATE_FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern("dd.MM")
}
