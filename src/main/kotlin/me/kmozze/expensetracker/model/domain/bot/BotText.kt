package me.kmozze.expensetracker.model.domain.bot

import me.kmozze.expensetracker.exception.ErrorCode
import me.kmozze.expensetracker.model.domain.expense.ExpenseListFilter
import me.kmozze.expensetracker.model.domain.expense.ExpenseListPage
import me.kmozze.expensetracker.model.domain.expense.Money
import java.time.LocalDate

sealed class BotText {
    data object WelcomeFirstTime : BotText()

    data object WelcomeBack : BotText()

    data object MainMenu : BotText()

    data object MainMenuInfo : BotText()

    data object MainMenuActions : BotText()

    data object Done : BotText()

    data object FinishCurrentDialog : BotText()

    data object UnknownCommand : BotText()

    data object AddExpenseInstructions : BotText()

    data class ExpenseView(
        val amount: Money?,
        val categoryName: String?,
        val expenseDate: LocalDate?,
        val description: String?,
    ) : BotText()

    data class ExpenseListSettings(
        val filter: ExpenseListFilter,
        val categoryName: String?,
    ) : BotText()

    data object ExpenseListPeriodSelection : BotText()

    data object ExpenseListCategorySelection : BotText()

    data class ExpenseListView(
        val page: ExpenseListPage,
    ) : BotText()

    data object SelectCategory : BotText()

    data object SelectExpenseDate : BotText()

    data object EnterExpenseDateManually : BotText()

    data object ExpenseSaved : BotText()

    data object EditExpenseFieldSelection : BotText()

    data object EnterExpenseAmount : BotText()

    data object EnterExpenseDescription : BotText()

    data object ExpenseDeleted : BotText()

    data object ExpenseUnavailable : BotText()

    data object ExpenseCanceled : BotText()

    data object SelectionExpired : BotText()

    data object NoCategories : BotText()

    data object FeatureInProgress : BotText()

    data class Error(
        val errorCode: ErrorCode,
    ) : BotText()
}
