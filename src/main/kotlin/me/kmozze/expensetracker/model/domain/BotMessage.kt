package me.kmozze.expensetracker.model.domain

import me.kmozze.expensetracker.exception.ErrorCode
import java.math.BigDecimal

sealed class BotMessage {
    data object WelcomeFirstTime : BotMessage()

    data object WelcomeBack : BotMessage()

    data object UnknownCommand : BotMessage()

    data object AddExpenseInstructions : BotMessage()

    data class SelectCategory(
        val amount: BigDecimal,
        val description: String?,
    ) : BotMessage()

    data class ExpenseSaved(
        val amount: BigDecimal,
        val categoryName: String,
        val description: String,
    ) : BotMessage()

    data object ExpenseCanceled : BotMessage()

    data object FeatureInProgress : BotMessage()

    data class Error(
        val errorCode: ErrorCode,
    ) : BotMessage()
}
