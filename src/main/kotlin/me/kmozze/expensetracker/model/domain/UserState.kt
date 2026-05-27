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
        val cardMessageId: Int? = null,
    ) : UserState()

    data class AwaitingExpenseManualDateInput(
        val expenseDraft: ExpenseDraft,
        val cardMessageId: Int? = null,
    ) : UserState()

    data class AwaitingExpenseDeletionConfirmation(
        val expenseId: UUID,
        val cardMessageId: Int? = null,
    ) : UserState()
}
