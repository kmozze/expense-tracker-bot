package me.kmozze.expensetracker.handler

import me.kmozze.expensetracker.model.domain.bot.BotAction
import me.kmozze.expensetracker.model.domain.bot.BotText
import me.kmozze.expensetracker.model.domain.bot.CallbackAnswer
import me.kmozze.expensetracker.model.domain.bot.HandlerResponse
import me.kmozze.expensetracker.model.domain.bot.OutgoingMessage
import me.kmozze.expensetracker.model.domain.bot.ResponseDelivery
import me.kmozze.expensetracker.model.domain.bot.UserState

internal fun outgoingMessage(
    text: BotText,
    actions: List<BotAction>,
    delivery: ResponseDelivery = ResponseDelivery.SendNewMessage,
): OutgoingMessage =
    OutgoingMessage(
        text = text,
        actions = actions,
        delivery = delivery,
    )

internal fun handlerResponse(
    message: OutgoingMessage,
    nextState: UserState? = null,
    callbackAnswer: CallbackAnswer? = null,
): HandlerResponse =
    handlerResponse(
        messages = listOf(message),
        nextState = nextState,
        callbackAnswer = callbackAnswer,
    )

internal fun handlerResponse(
    messages: List<OutgoingMessage>,
    nextState: UserState? = null,
    callbackAnswer: CallbackAnswer? = null,
): HandlerResponse =
    HandlerResponse(
        outgoingMessages = messages,
        callbackAnswer = callbackAnswer,
        nextState = nextState,
    )

internal fun callbackAnswerResponse(callbackAnswer: CallbackAnswer): HandlerResponse =
    handlerResponse(
        messages = emptyList(),
        callbackAnswer = callbackAnswer,
    )
