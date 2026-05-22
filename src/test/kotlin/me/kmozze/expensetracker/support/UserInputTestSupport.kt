package me.kmozze.expensetracker.support

import me.kmozze.expensetracker.adapter.input.UserCommandParser
import me.kmozze.expensetracker.handler.DialogueRouter
import me.kmozze.expensetracker.model.domain.HandlerResult
import me.kmozze.expensetracker.model.domain.UserInput

fun makeUserInput(
    userId: Long,
    chatId: Long,
    text: String? = null,
    callbackData: String? = null,
): UserInput =
    UserInput(
        userId = userId,
        chatId = chatId,
        text = text,
        callbackData = callbackData,
        command = UserCommandParser.parse(text = text, callbackData = callbackData),
    )

fun DialogueRouter.processUserInput(
    userId: Long,
    chatId: Long,
    text: String? = null,
    callbackData: String? = null,
): HandlerResult = process(makeUserInput(userId = userId, chatId = chatId, text = text, callbackData = callbackData))
