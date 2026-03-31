package me.kmozze.expensetracker.model

import java.math.BigDecimal
import java.time.OffsetDateTime
import java.util.UUID

data class Expense(
    val id: UUID = UUID.randomUUID(),
    val categoryId: UUID,
    val amount: BigDecimal,
    val chatId: Long,
    val description: String? = null,
    val createdAt: OffsetDateTime? = null,
)
