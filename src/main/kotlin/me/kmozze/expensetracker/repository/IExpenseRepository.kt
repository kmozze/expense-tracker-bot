package me.kmozze.expensetracker.repository

import me.kmozze.expensetracker.model.entity.Expense
import java.time.OffsetDateTime
import java.util.UUID

interface IExpenseRepository {
    fun create(expense: Expense): Expense

    fun update(expense: Expense): Expense

    fun findByIdForUser(
        id: UUID,
        userId: Long,
    ): Expense?

    fun deleteByIdForUser(
        id: UUID,
        userId: Long,
    ): Boolean

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

    fun existsByCategoryId(categoryId: UUID): Boolean
}
