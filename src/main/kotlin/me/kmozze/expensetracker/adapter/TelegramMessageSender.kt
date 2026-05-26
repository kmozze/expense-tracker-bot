package me.kmozze.expensetracker.adapter

import me.kmozze.expensetracker.adapter.ui.KeyboardApplier
import me.kmozze.expensetracker.model.domain.BotAction
import org.slf4j.LoggerFactory
import org.telegram.telegrambots.meta.api.methods.send.SendMessage
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText
import org.telegram.telegrambots.meta.generics.TelegramClient

internal class TelegramMessageSender(
    private val telegramClient: TelegramClient,
    private val keyboardApplier: KeyboardApplier,
) {
    private val logger = LoggerFactory.getLogger(this::class.java)

    fun sendNewMessage(
        chatId: Long,
        text: String,
        actions: List<BotAction>,
    ) {
        val sendMessage = SendMessage(chatId.toString(), text)

        keyboardApplier.apply(sendMessage, actions)

        try {
            telegramClient.execute(sendMessage)
            logger.info("Successful to send message to chat $chatId")
        } catch (e: Exception) {
            logger.error("Failed to send message to chat $chatId", e)
        }
    }

    fun editMessage(
        chatId: Long,
        messageId: Int,
        text: String,
        actions: List<BotAction>,
    ) {
        val editMessage =
            EditMessageText(text).apply {
                this.chatId = chatId.toString()
                this.messageId = messageId
            }

        keyboardApplier.apply(editMessage, actions)

        try {
            telegramClient.execute(editMessage)
            logger.info("Successful to edit message $messageId in chat $chatId")
        } catch (e: Exception) {
            if (e.isMessageNotModifiedError()) {
                logger.info("Message $messageId in chat $chatId is already up to date")
                return
            }

            logger.error("Failed to edit message $messageId in chat $chatId", e)
            sendNewMessage(chatId, text, fallbackActionsForSendMessage(actions))
        }
    }

    private fun fallbackActionsForSendMessage(actions: List<BotAction>): List<BotAction> {
        val actionsWithoutInlineCleanup = actions.filterNot { it is BotAction.ClearInlineKeyboard }
        if (actionsWithoutInlineCleanup.size == actions.size) {
            return actionsWithoutInlineCleanup
        }

        return if (actionsWithoutInlineCleanup.any { it is BotAction.ShowMainMenu }) {
            actionsWithoutInlineCleanup
        } else {
            actionsWithoutInlineCleanup + BotAction.ShowMainMenu
        }
    }

    private fun Exception.isMessageNotModifiedError(): Boolean = message?.contains("message is not modified", ignoreCase = true) == true
}
