package me.kmozze.expensetracker.repository

import me.kmozze.expensetracker.model.entity.Expense
import java.time.OffsetDateTime
import java.util.UUID

interface IExpenseRepository {
    fun create(expense: Expense): Expense

    fun update(expense: Expense): Expense

    fun delete(id: UUID)

    fun findById(id: UUID): Expense

    fun findAllByUserIdAndPeriod(
        userId: Long,
        from: OffsetDateTime,
        to: OffsetDateTime,
    ): List<Expense>

    fun existsByCategoryId(categoryId: UUID): Boolean
}
