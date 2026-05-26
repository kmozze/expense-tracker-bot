package me.kmozze.expensetracker.handler.statehandler

import me.kmozze.expensetracker.model.domain.BotAction
import me.kmozze.expensetracker.model.domain.ResponseDelivery

internal fun responseDeliveryForExpenseCard(cardMessageId: Int?): ResponseDelivery =
    cardMessageId
        ?.let { ResponseDelivery.EditMessage(it) }
        ?: ResponseDelivery.SendNewMessage

internal fun actionsForCompletedExpenseCard(delivery: ResponseDelivery): List<BotAction> =
    when (delivery) {
        is ResponseDelivery.EditMessage -> listOf(BotAction.ClearInlineKeyboard)
        ResponseDelivery.SendNewMessage -> listOf(BotAction.ShowMainMenu)
    }
