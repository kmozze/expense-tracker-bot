package me.kmozze.expensetracker.unit.adapter

import me.kmozze.expensetracker.adapter.TelegramMessageSender
import me.kmozze.expensetracker.adapter.ui.KeyboardApplier
import me.kmozze.expensetracker.model.domain.BotAction
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.telegram.telegrambots.meta.api.methods.send.SendMessage
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText
import org.telegram.telegrambots.meta.api.objects.message.Message
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup
import org.telegram.telegrambots.meta.exceptions.TelegramApiException
import org.telegram.telegrambots.meta.generics.TelegramClient
import java.lang.reflect.InvocationHandler
import java.lang.reflect.Method
import java.lang.reflect.Proxy

class TelegramMessageSenderTest {
    @Test
    fun `edit failure sends fallback message with same text`() {
        val fakeTelegramClient = EditFailingTelegramClient(TelegramApiException("message to edit not found"))
        val sender = TelegramMessageSender(fakeTelegramClient.client, KeyboardApplier())

        sender.editMessage(
            chatId = CHAT_ID,
            messageId = MESSAGE_ID,
            text = TEXT,
            actions = listOf(BotAction.ClearInlineKeyboard),
        )

        assertThat(fakeTelegramClient.calls).hasSize(2)
        assertThat(fakeTelegramClient.calls[0])
            .isInstanceOfSatisfying(EditMessageText::class.java) {
                assertThat(it.chatId).isEqualTo(CHAT_ID.toString())
                assertThat(it.messageId).isEqualTo(MESSAGE_ID)
                assertThat(it.text).isEqualTo(TEXT)
            }
        assertThat(fakeTelegramClient.calls[1])
            .isInstanceOfSatisfying(SendMessage::class.java) {
                assertThat(it.chatId).isEqualTo(CHAT_ID.toString())
                assertThat(it.text).isEqualTo(TEXT)
                assertThat(it.replyMarkup).isInstanceOf(ReplyKeyboardMarkup::class.java)
            }
    }

    @Test
    fun `edit failure keeps inline actions for fallback message`() {
        val fakeTelegramClient = EditFailingTelegramClient(TelegramApiException("message to edit not found"))
        val sender = TelegramMessageSender(fakeTelegramClient.client, KeyboardApplier())

        sender.editMessage(
            chatId = CHAT_ID,
            messageId = MESSAGE_ID,
            text = TEXT,
            actions = listOf(BotAction.ShowExpenseDateSelection),
        )

        assertThat(fakeTelegramClient.calls).hasSize(2)
        assertThat(fakeTelegramClient.calls[1])
            .isInstanceOfSatisfying(SendMessage::class.java) {
                assertThat(it.replyMarkup).isInstanceOf(InlineKeyboardMarkup::class.java)
            }
    }

    @Test
    fun `message not modified edit failure does not send fallback message`() {
        val fakeTelegramClient = EditFailingTelegramClient(TelegramApiException("message is not modified"))
        val sender = TelegramMessageSender(fakeTelegramClient.client, KeyboardApplier())

        sender.editMessage(
            chatId = CHAT_ID,
            messageId = MESSAGE_ID,
            text = TEXT,
            actions = listOf(BotAction.ShowExpenseDateSelection),
        )

        assertThat(fakeTelegramClient.calls).hasSize(1)
        assertThat(fakeTelegramClient.calls[0]).isInstanceOf(EditMessageText::class.java)
    }

    private class EditFailingTelegramClient(
        private val editException: TelegramApiException,
    ) : InvocationHandler {
        val calls = mutableListOf<Any>()
        val client: TelegramClient =
            Proxy.newProxyInstance(
                TelegramClient::class.java.classLoader,
                arrayOf(TelegramClient::class.java),
                this,
            ) as TelegramClient

        override fun invoke(
            proxy: Any,
            method: Method,
            args: Array<out Any>?,
        ): Any? {
            if (method.declaringClass == Any::class.java) {
                return handleAnyMethod(proxy, method, args)
            }

            val argument = args?.singleOrNull()
            require(method.name == "execute" && argument != null) {
                "Unexpected TelegramClient call: ${method.name}"
            }

            calls += argument
            return when (argument) {
                is EditMessageText -> throw editException
                is SendMessage -> Message()
                else -> error("Unexpected execute argument: ${argument::class.java.name}")
            }
        }

        private fun handleAnyMethod(
            proxy: Any,
            method: Method,
            args: Array<out Any>?,
        ): Any? =
            when (method.name) {
                "equals" -> proxy === args?.singleOrNull()
                "hashCode" -> System.identityHashCode(proxy)
                "toString" -> "EditFailingTelegramClient"
                else -> error("Unexpected Any method: ${method.name}")
            }
    }

    private companion object {
        const val CHAT_ID = 123L
        const val MESSAGE_ID = 456
        const val TEXT = "Расход сохранен"
    }
}
