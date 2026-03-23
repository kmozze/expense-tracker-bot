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

            logger.info("Message from [{}]: {}", chatId, text)

            if (text.startsWith("/")) {
                handleCommand(chatId, text)
            } else {
                sendNotification(chatId, "Я не умею распозновать текст!\nВведи /help, чтобы ознакомиться со списком доступных команд.")
            }
        }
    }

    private fun handleCommand(chatId: String, text: String) {
        val response = when (text.split(" ")[0]) {
            "/start" -> "Добро пожаловать в менеджер расходов.\nВведи /help, чтобы ознакомиться со списком доступных команд."
            "/help" -> "Доступные команды:\n/start - запустить бота\n/help - список команд"
            else -> "Неизвестная команда. Введи /help, чтобы ознакомиться со списком доступных команд."
        }
        sendNotification(chatId, response)
    }

    private fun sendNotification(chatId: String, text: String) {
        val response = SendMessage(chatId, text)
        try {
            execute(response)
            logger.info("Response sent to [{}]", chatId)
        } catch (e: TelegramApiException) {
            logger.error("Error sending message to [{}]: {}", chatId, e.message)
        }
    }
}
