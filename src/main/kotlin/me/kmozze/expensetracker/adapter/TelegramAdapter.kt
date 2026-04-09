package me.kmozze.expensetracker.adapter

import me.kmozze.expensetracker.adapter.ui.Keyboards
import me.kmozze.expensetracker.handler.DialogueRouter
import me.kmozze.expensetracker.model.domain.Action
import me.kmozze.expensetracker.model.domain.HandlerResponse
import me.kmozze.expensetracker.model.domain.Message
import me.kmozze.expensetracker.model.domain.UserInput
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
import org.telegram.telegrambots.meta.generics.TelegramClient

@Component
class TelegramAdapter(
    @param:Value("\${bot.token}") private val botToken: String,
    @param:Value("\${bot.name}") private val botName: String,
    private val dialogueRouter: DialogueRouter,
    private val expenseService: ExpenseService,
) : SpringLongPollingBot,
    LongPollingSingleThreadUpdateConsumer {
    private val logger = LoggerFactory.getLogger(this::class.java)

    private val telegramClient: TelegramClient = OkHttpTelegramClient(botToken)

    override fun getBotToken(): String = botToken

    override fun getUpdatesConsumer(): LongPollingUpdateConsumer = this

    override fun consume(update: Update) {
        if (update.hasMessage() && update.message.hasText()) {
            val userId = update.message.from.id
            val chatId = update.message.chatId
            val text = update.message.text

            logger.info("Message from [{}]: {}", userId, text)

            val userInput =
                UserInput(
                    userId = userId,
                    chatId = chatId,
                    text = text,
                )

            val response = dialogueRouter.process(userInput)
            sendResponse(chatId, response)
        }
    }

    private fun sendResponse(
        chatId: Long,
        response: HandlerResponse,
    ) {
        val text = getText(response.message)

        val sendMessage = SendMessage(chatId.toString(), text)
        applyActions(sendMessage, response.actions)

        try {
            telegramClient.execute(sendMessage)
            logger.debug("Message sent to chat {}", chatId)
        } catch (e: Exception) {
            logger.error("Failed to send message to chat $chatId", e)
        }
    }

    private fun getText(message: Message): String =
        when (message) {
            is Message.WelcomeFirstTime ->
                "👋 Добро пожаловать! Я создал для тебя базовые категории расходов."

            is Message.WelcomeBack ->
                "С возвращением! 💸 Я готов записывать твои новые траты."

            is Message.UnknownCommand ->
                "Я не понял команду 😕\n Что бы вернуться в главное меню напиши /start"
        }

    private fun applyActions(
        sendMessage: SendMessage,
        actions: List<Action>,
    ) {
        actions.forEach { action ->
            when (action) {
                is Action.ShowMainMenu -> {
                    sendMessage.replyMarkup = Keyboards.mainMenu()
                }
            }
        }
    }
}
