package me.kmozze.expensetracker.unit.adapter.ui

import me.kmozze.expensetracker.adapter.ui.Buttons
import me.kmozze.expensetracker.adapter.ui.KeyboardApplier
import me.kmozze.expensetracker.model.domain.bot.BotAction
import me.kmozze.expensetracker.model.domain.expense.ExpenseListFilter
import me.kmozze.expensetracker.model.domain.expense.ExpenseListItem
import me.kmozze.expensetracker.model.domain.expense.ExpenseListPage
import me.kmozze.expensetracker.model.domain.expense.ExpenseListPeriod
import me.kmozze.expensetracker.model.domain.expense.Money
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.telegram.telegrambots.meta.api.methods.send.SendMessage
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardRemove
import java.math.BigDecimal
import java.time.LocalDate
import java.util.UUID

class KeyboardApplierTest {
    private val keyboardApplier = KeyboardApplier()

    @Test
    fun `send message uses inline keyboard for main menu`() {
        val sendMessage = SendMessage("123", "text")

        keyboardApplier.apply(sendMessage, listOf(BotAction.ShowMainMenu))

        val keyboard = sendMessage.replyMarkup as InlineKeyboardMarkup
        assertThat(keyboard.keyboard).hasSize(4)
    }

    @Test
    fun `send message removes reply keyboard`() {
        val sendMessage = SendMessage("123", "text")

        keyboardApplier.apply(sendMessage, listOf(BotAction.RemoveReplyKeyboard))

        assertThat(sendMessage.replyMarkup).isInstanceOf(ReplyKeyboardRemove::class.java)
    }

    @Test
    fun `send message uses reply keyboard for date selection`() {
        val sendMessage = SendMessage("123", "text")

        keyboardApplier.apply(sendMessage, listOf(BotAction.ShowExpenseDateSelection))

        val keyboard = sendMessage.replyMarkup as ReplyKeyboardMarkup
        assertThat(keyboard.keyboard).hasSize(3)
    }

    @Test
    fun `send message uses reply keyboard for category names`() {
        val sendMessage = SendMessage("123", "text")

        keyboardApplier.apply(sendMessage, listOf(BotAction.ShowCategorySelection(listOf("Еда", "Транспорт", "Жильё"))))

        val keyboard = sendMessage.replyMarkup as ReplyKeyboardMarkup
        assertThat(keyboard.keyboard[0].map { it.text }).containsExactly("Еда", "Транспорт")
        assertThat(keyboard.keyboard[1].map { it.text }).containsExactly("Жильё")
    }

    @Test
    fun `send message uses reply keyboard for edit field selection`() {
        val sendMessage = SendMessage("123", "text")

        keyboardApplier.apply(sendMessage, listOf(BotAction.ShowExpenseEditFieldSelection))

        val keyboard = sendMessage.replyMarkup as ReplyKeyboardMarkup
        assertThat(keyboard.keyboard).hasSize(3)
        assertThat(keyboard.keyboard[0]).hasSize(2)
        assertThat(keyboard.keyboard[2].map { it.text }).containsExactly(Buttons.FINISH_EXPENSE_EDIT, Buttons.CANCEL)
    }

    @Test
    fun `send message uses inline keyboard for expense card actions`() {
        val sendMessage = SendMessage("123", "text")

        keyboardApplier.apply(sendMessage, listOf(BotAction.ShowExpenseCardActions(EXPENSE_ID)))

        val keyboard = sendMessage.replyMarkup as InlineKeyboardMarkup
        assertThat(keyboard.keyboard).hasSize(1)
    }

    @Test
    fun `edit message uses inline keyboard for expense deletion confirmation`() {
        val editMessage = EditMessageText("text")

        keyboardApplier.apply(editMessage, listOf(BotAction.ShowExpenseDeletionConfirmation(EXPENSE_ID)))

        val keyboard = editMessage.replyMarkup as InlineKeyboardMarkup
        assertThat(keyboard.keyboard).hasSize(1)
        assertThat(keyboard.keyboard.first()).hasSize(2)
    }

    @Test
    fun `edit message clears inline keyboard for finish actions`() {
        val editMessage = EditMessageText("text")

        keyboardApplier.apply(editMessage, listOf(BotAction.ClearInlineKeyboard))

        assertThat(editMessage.replyMarkup).isNull()
    }

    @Test
    fun `edit message uses inline keyboard for main menu`() {
        val editMessage = EditMessageText("text")

        keyboardApplier.apply(editMessage, listOf(BotAction.ShowMainMenu))

        val keyboard = editMessage.replyMarkup as InlineKeyboardMarkup
        assertThat(keyboard.keyboard).hasSize(4)
    }

    @Test
    fun `send message uses inline keyboard for expense list settings`() {
        val sendMessage = SendMessage("123", "text")

        keyboardApplier.apply(sendMessage, listOf(BotAction.ShowExpenseListSettings(DEFAULT_LIST_FILTER)))

        val keyboard = sendMessage.replyMarkup as InlineKeyboardMarkup
        assertThat(keyboard.keyboard).hasSize(2)
        assertThat(keyboard.keyboard[0].map { it.text })
            .containsExactly(Buttons.CHANGE_EXPENSE_LIST_PERIOD, Buttons.CHANGE_EXPENSE_LIST_CATEGORY)
        assertThat(keyboard.keyboard[1].single().text).isEqualTo(Buttons.SHOW_EXPENSE_LIST)
    }

    @Test
    fun `edit message uses inline keyboard for expense list page rows and pagination`() {
        val editMessage = EditMessageText("text")
        val page =
            ExpenseListPage(
                filter = DEFAULT_LIST_FILTER,
                items =
                    listOf(
                        ExpenseListItem(
                            expenseId = EXPENSE_ID,
                            expenseDate = LocalDate.parse("2026-05-24"),
                            categoryName = "Транспорт",
                            amount = Money.of(BigDecimal("500.00")),
                        ),
                    ),
                page = 1,
                pageSize = 10,
                totalCount = 25,
            )

        keyboardApplier.apply(editMessage, listOf(BotAction.ShowExpenseListPage(page)))

        val keyboard = editMessage.replyMarkup as InlineKeyboardMarkup
        assertThat(keyboard.keyboard).hasSize(2)
        assertThat(keyboard.keyboard[0].single().text).isEqualTo("24.05 · Транспорт · 500 ₽")
        assertThat(keyboard.keyboard[1].map { it.text })
            .containsExactly(Buttons.EXPENSE_LIST_PREVIOUS, Buttons.EXPENSE_LIST_NEXT)
    }

    private companion object {
        val EXPENSE_ID: UUID = UUID.fromString("00000000-0000-0000-0000-000000000001")
        val DEFAULT_LIST_FILTER: ExpenseListFilter = ExpenseListFilter(period = ExpenseListPeriod.Month)
    }
}
