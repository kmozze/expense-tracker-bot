package me.kmozze.expensetracker.bot

import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import org.telegram.telegrambots.bots.TelegramLongPollingBot
import org.telegram.telegrambots.meta.api.methods.send.SendMessage
import org.telegram.telegrambots.meta.api.objects.Update
import org.telegram.telegrambots.meta.exceptions.TelegramApiException

@Component
class ExpenseTrackerBot (
    @param:Value("\${bot.token}") private val botToken: String,
    @param:Value("\${bot.name}") private val botName: String,
) : TelegramLongPollingBot(botToken) {

    private val logger = LoggerFactory.getLogger(this::class.java)

    override fun getBotUsername(): String = botName

    override fun onUpdateReceived(update: Update) {
        if (update.hasMessage() && update.message.hasText()) {
            val chatId = update.message.chatId.toString()
            val text = update.message.text

            logger.info("Received message from chat [$chatId]: $text")

            val sendMessage = SendMessage(chatId, text)

            try {
                execute(sendMessage)
                logger.info("Successfully send message to chat [$chatId]: $text")
            } catch (e: TelegramApiException) {
                logger.error("Failed send message to chat [$chatId]! Reason: ${e.message}", e)
            }
        }
    }

}