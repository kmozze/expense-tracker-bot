package me.kmozze.expensetracker.unit.adapter.ui

import me.kmozze.expensetracker.adapter.ui.KeyboardApplier
import me.kmozze.expensetracker.model.domain.BotAction
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText

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
}
