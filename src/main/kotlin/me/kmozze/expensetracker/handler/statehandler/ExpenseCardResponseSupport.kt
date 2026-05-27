package me.kmozze.expensetracker.handler.statehandler

import me.kmozze.expensetracker.model.domain.BotAction
import me.kmozze.expensetracker.model.domain.BotMessage
import me.kmozze.expensetracker.model.domain.ResponseDelivery
import me.kmozze.expensetracker.model.entity.Expense
import java.util.UUID

internal fun responseDeliveryForExpenseCard(cardMessageId: Int?): ResponseDelivery =
    cardMessageId
        ?.let { ResponseDelivery.EditMessage(it) }
        ?: ResponseDelivery.SendNewMessage

internal fun actionsForCompletedExpenseCard(delivery: ResponseDelivery): List<BotAction> =
    when (delivery) {
        is ResponseDelivery.EditMessage -> listOf(BotAction.ClearInlineKeyboard)
        ResponseDelivery.SendNewMessage -> listOf(BotAction.ShowMainMenu)
    }

internal fun actionsForSavedExpenseCard(expenseId: UUID): List<BotAction> = listOf(BotAction.ShowExpenseCardActions(expenseId))

internal fun savedExpenseMessage(
    expense: Expense,
    categoryName: String,
    showDeletionConfirmation: Boolean = false,
): BotMessage.ExpenseSaved =
    BotMessage.ExpenseSaved(
        amount = expense.amount,
        categoryName = categoryName,
        expenseDate = expense.expenseDate,
        description = expense.description,
        showDeletionConfirmation = showDeletionConfirmation,
    )
