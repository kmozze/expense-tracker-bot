package me.kmozze.expensetracker.model.entity

import java.time.OffsetDateTime
import java.util.UUID

data class Category(
    val id: UUID = UUID.randomUUID(),
    val name: String,
    val userId: Long,
    val createdAt: OffsetDateTime? = null,
    val updatedAt: OffsetDateTime? = null,
)
