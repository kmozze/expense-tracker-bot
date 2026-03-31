package me.kmozze.expensetracker.model

import java.time.OffsetDateTime
import java.util.UUID

data class Category(
    val id: UUID = UUID.randomUUID(),
    val name: String,
    val chatId: Long,
    val createdAt: OffsetDateTime? = null,
    val updatedAt: OffsetDateTime? = null,
)
