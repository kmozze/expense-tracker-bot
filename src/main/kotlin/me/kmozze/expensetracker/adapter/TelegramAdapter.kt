package me.kmozze.expensetracker.adapter

import me.kmozze.expensetracker.adapter.ui.KeyboardApplier
import me.kmozze.expensetracker.adapter.ui.MessageFormatter
import me.kmozze.expensetracker.handler.DialogueRouter
import me.kmozze.expensetracker.model.domain.HandlerResponse
import me.kmozze.expensetracker.model.domain.HandlerResult
import me.kmozze.expensetracker.model.domain.UserInput
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import org.telegram.telegrambots.client.okhttp.OkHttpTelegramClient
import org.telegram.telegrambots.longpolling.interfaces.LongPollingUpdateConsumer
import org.telegram.telegrambots.longpolling.starter.SpringLongPollingBot
import org.telegram.telegrambots.longpolling.util.LongPollingSingleThreadUpdateConsumer
import org.telegram.telegrambots.meta.api.methods.AnswerCallbackQuery
import org.telegram.telegrambots.meta.api.methods.send.SendMessage
import org.telegram.telegrambots.meta.api.objects.Update
import org.telegram.telegrambots.meta.generics.TelegramClient

@Component
class TelegramAdapter(
    @param:Value("\${bot.token}") private val botToken: String,
    @param:Value("\${bot.name}") private val botName: String,
    private val dialogueRouter: DialogueRouter,
    private val messageFormatter: MessageFormatter,
    private val keyboardApplier: KeyboardApplier,
) : SpringLongPollingBot,
    LongPollingSingleThreadUpdateConsumer {
    private val logger = LoggerFactory.getLogger(this::class.java)

    private val telegramClient: TelegramClient = OkHttpTelegramClient(botToken)

    override fun getBotToken(): String = botToken

    override fun getUpdatesConsumer(): LongPollingUpdateConsumer = this

    override fun consume(update: Update) {
        if (update.hasCallbackQuery()) {
            acknowledgeCallback(update.callbackQuery)
        }

        val userInput = extractUserInput(update) ?: return

        val outcome: HandlerResult = dialogueRouter.process(userInput)
        sendResponse(userInput.chatId, outcome.response)
    }

    private fun extractUserInput(update: Update): UserInput? {
        return when {
            update.hasMessage() && update.message.hasText() -> {
                val msg = update.message
                logger.info("Message from [{}]: {}", msg.from.id, msg.text)
                UserInput(
                    userId = msg.from.id,
                    chatId = msg.chatId,
                    text = msg.text,
                    callbackData = null,
                )
            }

            update.hasCallbackQuery() -> {
                val callbackQuery = update.callbackQuery
                val msg = callbackQuery.message
                if (msg == null) {
                    logger.warn("Callback query without message from user {}", callbackQuery.from.id)
                    return null
                }
                logger.info("Callback from [{}]: {}", callbackQuery.from.id, callbackQuery.data)
                UserInput(
                    userId = callbackQuery.from.id,
                    chatId = msg.chatId,
                    text = null,
                    callbackData = callbackQuery.data,
                )
            }

            else -> {
                logger.debug("Unsupported update type: {}", update)
                null
            }
        }
    }

    private fun acknowledgeCallback(
        callbackQuery: org.telegram.telegrambots.meta.api.objects.CallbackQuery,
        notificationText: String? = null,
    ) {
        val answer =
            AnswerCallbackQuery(callbackQuery.id).apply {
                text = notificationText
                showAlert = false
            }
        try {
            telegramClient.execute(answer)
            logger.debug("Callback {} acknowledged", callbackQuery.id)
        } catch (e: Exception) {
            logger.error("Failed to acknowledge callback ${callbackQuery.id}: ${e.message}", e)
        }
    }

    private fun sendResponse(
        chatId: Long,
        response: HandlerResponse,
    ) {
        val text = messageFormatter.format(response.message)

        val sendMessage = SendMessage(chatId.toString(), text)

        keyboardApplier.apply(sendMessage, response.actions)

        try {
            telegramClient.execute(sendMessage)
            logger.info("Successful to send message to chat $chatId")
        } catch (e: Exception) {
            logger.error("Failed to send message to chat $chatId", e)
        }
    }
}
