package me.kmozze.expensetracker.model.domain

import java.util.UUID

sealed class UserCommand {
    data object Start : UserCommand()

    data object Menu : UserCommand()

    data object AddExpense : UserCommand()

    data object ViewExpenses : UserCommand()

    data object Categories : UserCommand()

    data object Statistics : UserCommand()

    data object Cancel : UserCommand()

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

    data object InvalidExpenseAction : UserCommand()

    data class PlainText(
        val value: String,
    ) : UserCommand()

    data object Unsupported : UserCommand()
}
