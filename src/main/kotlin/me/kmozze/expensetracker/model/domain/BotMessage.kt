package me.kmozze.expensetracker.model.domain

import me.kmozze.expensetracker.exception.ErrorCode
import java.time.LocalDate

sealed class BotMessage {
    data object WelcomeFirstTime : BotMessage()

    data object WelcomeBack : BotMessage()

    data object UnknownCommand : BotMessage()

    data object AddExpenseInstructions : BotMessage()

    data class SelectCategory(
        val amount: Money,
        val description: String?,
    ) : BotMessage()

    data class ExpenseSaved(
        val amount: Money,
        val categoryName: String,
        val expenseDate: LocalDate,
        val description: String?,
    ) : BotMessage()

    data object ExpenseCanceled : BotMessage()

    data object NoCategories : BotMessage()

    data object FeatureInProgress : BotMessage()

    data class Error(
        val errorCode: ErrorCode,
    ) : BotMessage()
}
