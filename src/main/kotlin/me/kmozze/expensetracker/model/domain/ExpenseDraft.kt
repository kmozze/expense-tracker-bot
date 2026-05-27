package me.kmozze.expensetracker.model.domain

import java.time.LocalDate
import java.util.UUID

data class ExpenseDraft(
    val amount: Money,
    val description: String?,
    val categoryId: UUID? = null,
    val categoryName: String? = null,
    val expenseDate: LocalDate? = null,
) {
    fun requireCategoryId(): UUID =
        requireNotNull(categoryId) {
            "ExpenseDraft.categoryId must be set before saving"
        }

    fun requireCategoryName(): String =
        requireNotNull(categoryName) {
            "ExpenseDraft.categoryName must be set before showing expense date selection"
        }

    fun requireExpenseDate(): LocalDate =
        requireNotNull(expenseDate) {
            "ExpenseDraft.expenseDate must be set before saving"
        }
}
