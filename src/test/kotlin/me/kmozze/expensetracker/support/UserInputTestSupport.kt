package me.kmozze.expensetracker.support

import me.kmozze.expensetracker.adapter.input.UserCommandParser
import me.kmozze.expensetracker.handler.DialogueRouter
import me.kmozze.expensetracker.model.domain.HandlerResult
import me.kmozze.expensetracker.model.domain.UserCommand
import me.kmozze.expensetracker.model.domain.UserInput

fun makeUserInput(
    userId: Long,
    chatId: Long,
    text: String? = null,
    callbackData: String? = null,
    callbackMessageId: Int? = null,
    command: UserCommand = UserCommandParser.parse(text = text, callbackData = callbackData),
): UserInput =
    UserInput(
        userId = userId,
        chatId = chatId,
        text = text,
        callbackData = callbackData,
        callbackMessageId = callbackMessageId,
        command = command,
    )

fun DialogueRouter.processUserInput(
    userId: Long,
    chatId: Long,
    text: String? = null,
    callbackData: String? = null,
    callbackMessageId: Int? = null,
    command: UserCommand = UserCommandParser.parse(text = text, callbackData = callbackData),
): HandlerResult =
    process(
        makeUserInput(
            userId = userId,
            chatId = chatId,
            text = text,
            callbackData = callbackData,
            callbackMessageId = callbackMessageId,
            command = command,
        ),
    )
