package me.kmozze.expensetracker.model.domain

import java.util.UUID

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

    data class PlainText(
        val value: String,
    ) : UserCommand()

    data object Unsupported : UserCommand()
}
