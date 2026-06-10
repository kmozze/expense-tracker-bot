package me.kmozze.expensetracker.model.domain.bot

import me.kmozze.expensetracker.model.domain.expense.ExpenseDateChoice
import me.kmozze.expensetracker.model.domain.expense.ExpenseEditField
import me.kmozze.expensetracker.model.domain.expense.ExpenseListFilter
import java.util.UUID

sealed class UserCommand {
    data object Start : UserCommand()

    data object Menu : UserCommand()

    data object AddExpense : UserCommand()

    data object ViewExpenses : UserCommand()

    data object Categories : UserCommand()

    data object Statistics : UserCommand()

    data object Cancel : UserCommand()

    data object FinishExpenseEdit : UserCommand()

    data class RequestExpenseListPeriodSelection(
        val filter: ExpenseListFilter,
    ) : UserCommand()

    data class RequestExpenseListCategorySelection(
        val filter: ExpenseListFilter,
    ) : UserCommand()

    data class SelectExpenseListPeriod(
        val filter: ExpenseListFilter,
    ) : UserCommand()

    data class SelectExpenseListCategory(
        val filter: ExpenseListFilter,
    ) : UserCommand()

    data class ShowExpenseList(
        val filter: ExpenseListFilter,
        val page: Int,
        val shouldEditCurrentMessage: Boolean = false,
    ) : UserCommand()

    data class OpenExpenseFromList(
        val expenseId: UUID,
    ) : UserCommand()

    data class RequestExpenseEdit(
        val expenseId: UUID,
    ) : UserCommand()

    data class RequestExpenseDeletion(
        val expenseId: UUID,
    ) : UserCommand()

    data class ConfirmExpenseDeletion(
        val expenseId: UUID,
    ) : UserCommand()

    data class CancelExpenseDeletion(
        val expenseId: UUID,
    ) : UserCommand()

    data class SelectExpenseDate(
        val choice: ExpenseDateChoice,
    ) : UserCommand()

    data class SelectExpenseEditField(
        val field: ExpenseEditField,
    ) : UserCommand()

    data object InvalidExpenseAction : UserCommand()

    data object InvalidExpenseListAction : UserCommand()

    data class PlainText(
        val value: String,
    ) : UserCommand()

    data object Unsupported : UserCommand()
}
