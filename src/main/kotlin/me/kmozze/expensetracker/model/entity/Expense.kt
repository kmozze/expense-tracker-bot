package me.kmozze.expensetracker.model.entity

import me.kmozze.expensetracker.model.domain.expense.Money
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.UUID

data class Expense(
    val id: UUID = UUID.randomUUID(),
    val categoryId: UUID,
    val amount: Money,
    val userId: Long,
    val expenseDate: LocalDate,
    val description: String? = null,
    val createdAt: OffsetDateTime? = null,
)
