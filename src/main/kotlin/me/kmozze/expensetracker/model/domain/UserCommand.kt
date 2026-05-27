package me.kmozze.expensetracker.model.domain

import java.util.UUID

enum class ExpenseDateSelection {
    TODAY,
    YESTERDAY,
    MANUAL,
}

sealed class UserCommand {
    data object Start : UserCommand()

    data object AddExpense : UserCommand()

    data object ViewExpenses : UserCommand()

    data object Categories : UserCommand()

    data object Statistics : UserCommand()

    data object Cancel : UserCommand()

    data class SelectCategory(
        val categoryId: UUID,
    ) : UserCommand()

    data object InvalidCategorySelection : UserCommand()

    data class SelectExpenseDate(
        val selection: ExpenseDateSelection,
    ) : UserCommand()

    data object InvalidExpenseDateSelection : UserCommand()

    data class RequestExpenseDeletion(
        val expenseId: UUID,
    ) : UserCommand()

    data class ConfirmExpenseDeletion(
        val expenseId: UUID,
    ) : UserCommand()

    data class CancelExpenseDeletion(
        val expenseId: UUID,
    ) : UserCommand()

    data object InvalidExpenseDeletion : UserCommand()

    data class PlainText(
        val value: String,
    ) : UserCommand()

    data object Unsupported : UserCommand()
}
