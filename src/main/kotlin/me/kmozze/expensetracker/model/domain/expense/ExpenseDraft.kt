package me.kmozze.expensetracker.model.domain.expense

import java.time.LocalDate
import java.util.UUID

data class ExpenseDraftCategory(
    val categoryId: UUID,
    val name: String,
)

data class ExpenseDraft(
    val amount: Money,
    val description: String?,
    val category: ExpenseDraftCategory? = null,
    val expenseDate: LocalDate? = null,
) {
    fun requireCategoryId(): UUID =
        requireNotNull(category?.categoryId) {
            "ExpenseDraft.category must be set before saving"
        }

    fun requireExpenseDate(): LocalDate =
        requireNotNull(expenseDate) {
            "ExpenseDraft.expenseDate must be set before saving"
        }
}

data class ExpenseEditSession(
    val expenseId: UUID,
    val expenseDraft: ExpenseDraft,
)
