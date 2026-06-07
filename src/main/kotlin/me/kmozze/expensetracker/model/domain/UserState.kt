package me.kmozze.expensetracker.model.domain

import java.util.UUID

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

    data class AwaitingExpenseEditFieldSelection(
        val expenseId: UUID,
        val expenseDraft: ExpenseDraft,
        val categoryName: String,
    ) : UserState()

    data class AwaitingExpenseAmountEdit(
        val expenseId: UUID,
        val expenseDraft: ExpenseDraft,
        val categoryName: String,
    ) : UserState()

    data class AwaitingExpenseCategoryEdit(
        val expenseId: UUID,
        val expenseDraft: ExpenseDraft,
        val categoryName: String,
    ) : UserState()

    data class AwaitingExpenseDateEditSelection(
        val expenseId: UUID,
        val expenseDraft: ExpenseDraft,
        val categoryName: String,
    ) : UserState()

    data class AwaitingExpenseDateEditManualInput(
        val expenseId: UUID,
        val expenseDraft: ExpenseDraft,
        val categoryName: String,
    ) : UserState()

    data class AwaitingExpenseDescriptionEdit(
        val expenseId: UUID,
        val expenseDraft: ExpenseDraft,
        val categoryName: String,
    ) : UserState()
}
