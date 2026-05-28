package me.kmozze.expensetracker.unit.adapter.ui

import me.kmozze.expensetracker.adapter.ui.KeyboardApplier
import me.kmozze.expensetracker.model.domain.BotAction
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.telegram.telegrambots.meta.api.methods.send.SendMessage
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardRemove
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

    private companion object {
        val EXPENSE_ID: UUID = UUID.fromString("00000000-0000-0000-0000-000000000001")
    }
}
