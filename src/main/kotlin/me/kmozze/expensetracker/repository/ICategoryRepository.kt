package me.kmozze.expensetracker.repository

import me.kmozze.expensetracker.model.entity.Category
import java.util.UUID

interface ICategoryRepository {
    fun findById(id: UUID): Category

    fun create(category: Category): Category

    fun update(category: Category): Category

    fun delete(id: UUID)

    fun existsByUserId(userId: Long): Boolean
}
