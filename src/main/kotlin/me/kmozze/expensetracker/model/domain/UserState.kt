package me.kmozze.expensetracker.model.domain

sealed class UserState {
    data object Idle : UserState()

    data object AwaitingExpenseInput : UserState()

    data class AwaitingCategorySelection(
        val parsedExpense: ParsedExpense,
    ) : UserState()
}
