package me.kmozze.expensetracker.model.domain

sealed class UserState {
    data object Idle : UserState()

    data object AwaitingExpenseInput : UserState()

    data class AwaitingCategorySelection(
        val expenseDraft: ExpenseDraft,
    ) : UserState()

    data class AwaitingExpenseDateSelection(
        val expenseDraft: ExpenseDraft,
    ) : UserState()

    data class AwaitingExpenseManualDateInput(
        val expenseDraft: ExpenseDraft,
    ) : UserState()

    data class AwaitingExpenseEditFieldSelection(
        val editSession: ExpenseEditSession,
    ) : UserState()

    data class AwaitingExpenseAmountEdit(
        val editSession: ExpenseEditSession,
    ) : UserState()

    data class AwaitingExpenseCategoryEdit(
        val editSession: ExpenseEditSession,
    ) : UserState()

    data class AwaitingExpenseDateEditSelection(
        val editSession: ExpenseEditSession,
    ) : UserState()

    data class AwaitingExpenseDateEditManualInput(
        val editSession: ExpenseEditSession,
    ) : UserState()

    data class AwaitingExpenseDescriptionEdit(
        val editSession: ExpenseEditSession,
    ) : UserState()
}
