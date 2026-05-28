package me.kmozze.expensetracker.model.domain

sealed class UserState {
    data object Idle : UserState()

    data object AwaitingExpenseInput : UserState()

    data class AwaitingCategorySelection(
        val expenseDraft: ExpenseDraft,
    ) : UserState()

    data class AwaitingExpenseDateSelection(
        val expenseDraft: ExpenseDraft,
        val categoryName: String,
    ) : UserState()

    data class AwaitingExpenseManualDateInput(
        val expenseDraft: ExpenseDraft,
        val categoryName: String,
    ) : UserState()
}
