package me.kmozze.expensetracker.model.domain

import me.kmozze.expensetracker.exception.ErrorCode
import java.time.LocalDate

sealed class BotText {
    data object WelcomeFirstTime : BotText()

    data object WelcomeBack : BotText()

    data object MainMenu : BotText()

    data object Done : BotText()

    data object FinishCurrentDialog : BotText()

    data object UnknownCommand : BotText()

    data object AddExpenseInstructions : BotText()

    data class SelectCategory(
        val amount: Money,
        val description: String?,
    ) : BotText()

    data class SelectExpenseDate(
        val amount: Money,
        val categoryName: String,
        val description: String?,
    ) : BotText()

    data class EnterExpenseDateManually(
        val amount: Money,
        val categoryName: String,
        val description: String?,
    ) : BotText()

    data class ExpenseSaved(
        val amount: Money,
        val categoryName: String,
        val expenseDate: LocalDate,
        val description: String?,
    ) : BotText()

    data object ExpenseCanceled : BotText()

    data object SelectionExpired : BotText()

    data object NoCategories : BotText()

    data object FeatureInProgress : BotText()

    data class Error(
        val errorCode: ErrorCode,
    ) : BotText()
}
