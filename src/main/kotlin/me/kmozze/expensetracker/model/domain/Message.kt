package me.kmozze.expensetracker.model.domain

import me.kmozze.expensetracker.exception.ErrorCode
import java.math.BigDecimal

sealed class Message {
    data object WelcomeFirstTime : Message()

    data object WelcomeBack : Message()

    data object UnknownCommand : Message()

    data object AddExpenseInstructions : Message()

    data class SelectCategory(
        val amount: BigDecimal,
        val description: String?,
    ) : Message()

    data class ExpenseSaved(
        val amount: BigDecimal,
        val categoryName: String,
        val description: String,
    ) : Message()

    data object ExpenseCanceled : Message()

    data object FeatureInProgress : Message()

    data class Error(
        val errorCode: ErrorCode,
    ) : Message()
}
