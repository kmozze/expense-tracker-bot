package me.kmozze.expensetracker.repository

import me.kmozze.expensetracker.model.domain.expense.ExpenseListItem
import me.kmozze.expensetracker.model.entity.Expense
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.UUID

interface IExpenseRepository {
    fun create(expense: Expense): Expense

    fun updateForUser(
        expense: Expense,
        userId: Long,
    ): Expense?

    fun deleteByIdForUser(
        id: UUID,
        userId: Long,
    ): Boolean

    fun findByIdForUser(
        id: UUID,
        userId: Long,
    ): Expense?

    /**
     * Returns expenses created from newest to oldest, including `from` and excluding `to`.
     *
     * User-facing period queries will switch to `expenseDate` in the date selection flow.
     */
    fun findAllByUserIdAndPeriod(
        userId: Long,
        from: OffsetDateTime,
        to: OffsetDateTime,
    ): List<Expense>

    fun findListItemsForUser(
        userId: Long,
        from: LocalDate?,
        to: LocalDate?,
        categoryId: UUID?,
        limit: Int,
        offset: Int,
    ): List<ExpenseListItem>

    fun countListItemsForUser(
        userId: Long,
        from: LocalDate?,
        to: LocalDate?,
        categoryId: UUID?,
    ): Int

    fun existsByCategoryId(categoryId: UUID): Boolean
}
