package me.kmozze.expensetracker.bot

import me.kmozze.expensetracker.exception.BusinessException
import me.kmozze.expensetracker.service.ExpenseService
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import org.telegram.telegrambots.client.okhttp.OkHttpTelegramClient
import org.telegram.telegrambots.longpolling.interfaces.LongPollingUpdateConsumer
import org.telegram.telegrambots.longpolling.starter.SpringLongPollingBot
import org.telegram.telegrambots.longpolling.util.LongPollingSingleThreadUpdateConsumer
import org.telegram.telegrambots.meta.api.methods.send.SendMessage
import org.telegram.telegrambots.meta.api.objects.Update
import org.telegram.telegrambots.meta.exceptions.TelegramApiException
import org.telegram.telegrambots.meta.generics.TelegramClient

@Component
class ExpenseTrackerBot(
    @param:Value("\${bot.token}") private val botToken: String,
    @param:Value("\${bot.name}") private val botName: String,
    private val expenseService: ExpenseService,
) : SpringLongPollingBot,
    LongPollingSingleThreadUpdateConsumer {
    private val logger = LoggerFactory.getLogger(this::class.java)

    private val telegramClient: TelegramClient = OkHttpTelegramClient(botToken)

    override fun getBotToken(): String = botToken

    override fun getUpdatesConsumer(): LongPollingUpdateConsumer = this

    override fun consume(update: Update) {
        if (update.hasMessage() && update.message.hasText()) {
            val chatId = update.message.chatId.toString()
            val text = update.message.text

            logger.info("Message from [{}]: {}", chatId, text)

            if (text.startsWith("/")) {
                handleCommand(chatId, text)
            } else {
                handleAddExpense(chatId, text)
            }
        }
    }

    private fun handleCommand(
        chatId: String,
        text: String,
    ) {
        val response =
            when (text.split(" ")[0]) {
                "/start" ->
                    """
                    Добро пожаловать в менеджер расходов.
                    Введи /help, чтобы ознакомиться со списком доступных команд.
                    """.trimIndent()

                "/help" ->
                    """
                    Доступные команды:
                    /start - запустить бота
                    /help - список команд
                    Можешь добавить расходы просто написав 'еда 100' или '100 еда'
                    """.trimIndent()

                else -> "Неизвестная команда. Введи /help, чтобы ознакомиться со списком доступных команд."
            }
        sendNotification(chatId, response)
    }

    private fun handleAddExpense(
        chatId: String,
        text: String,
    ) {
        try {
            val expense = expenseService.addExpense(text)
            sendNotification(chatId, "Сохранено!\nКатегория: ${expense.category}\nСумма: ${expense.amount}")
        } catch (e: BusinessException) {
            logger.warn("User input error [{}]: {}", chatId, e.message)
            sendNotification(chatId, "Не понимаю, попробуй так:\n'100 продукты' или 'продукты 100'")
        } catch (e: Exception) {
            logger.error("Unexpected error for {}: ", chatId, e)
            sendNotification(chatId, "Произошла ошибка на сервере. Попробуйте позже.")
        }
    }

    private fun sendNotification(
        chatId: String,
        text: String,
    ) {
        val response = SendMessage(chatId, text)
        try {
            telegramClient.execute(response)
            logger.info("Response sent to [{}]", chatId)
        } catch (e: TelegramApiException) {
            logger.error("Error sending message to [{}]: {}", chatId, e.message)
        }
    }
}
