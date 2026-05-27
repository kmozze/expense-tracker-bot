package me.kmozze.expensetracker.unit.adapter.ui

import me.kmozze.expensetracker.adapter.ui.KeyboardApplier
import me.kmozze.expensetracker.model.domain.BotAction
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText
import java.util.UUID

class KeyboardApplierTest {
    private val keyboardApplier = KeyboardApplier()

    @Test
    fun `edit message uses inline keyboard for inline actions`() {
        val editMessage = EditMessageText("text")

        keyboardApplier.apply(editMessage, listOf(BotAction.ShowExpenseDateSelection))

        assertThat(editMessage.replyMarkup.keyboard).hasSize(3)
    }

    @Test
    fun `edit message clears inline keyboard for finish actions`() {
        val editMessage = EditMessageText("text")

        keyboardApplier.apply(editMessage, listOf(BotAction.ClearInlineKeyboard))

        assertThat(editMessage.replyMarkup).isNull()
    }

    @Test
    fun `edit message treats main menu action as inline cleanup`() {
        val editMessage = EditMessageText("text")

        keyboardApplier.apply(editMessage, listOf(BotAction.ShowMainMenu))

        assertThat(editMessage.replyMarkup).isNull()
    }

    @Test
    fun `edit message uses inline keyboard for expense card actions`() {
        val editMessage = EditMessageText("text")

        keyboardApplier.apply(editMessage, listOf(BotAction.ShowExpenseCardActions(EXPENSE_ID)))

        val callbackData =
            editMessage.replyMarkup.keyboard
                .single()
                .single()
                .callbackData
        assertThat(editMessage.replyMarkup.keyboard).hasSize(1)
        assertThat(callbackData).isEqualTo("delete_expense:$EXPENSE_ID")
    }

    @Test
    fun `edit message uses inline keyboard for expense deletion confirmation`() {
        val editMessage = EditMessageText("text")

        keyboardApplier.apply(editMessage, listOf(BotAction.ShowExpenseDeletionConfirmation(EXPENSE_ID)))

        val buttons = editMessage.replyMarkup.keyboard.single()
        assertThat(buttons).hasSize(2)
        assertThat(buttons[0].callbackData).isEqualTo("confirm_delete_expense:$EXPENSE_ID")
        assertThat(buttons[1].callbackData).isEqualTo("cancel_delete_expense:$EXPENSE_ID")
    }

    private companion object {
        val EXPENSE_ID: UUID = UUID.fromString("00000000-0000-0000-0000-000000000101")
    }
}
