package me.kmozze.expensetracker.repository

import me.kmozze.expensetracker.model.entity.Category
import java.util.UUID

interface ICategoryRepository {
    fun findByIdForUser(
        id: UUID,
        userId: Long,
    ): Category?

    fun findAllByUserId(userId: Long): List<Category>

    fun create(category: Category): Category

    /**
     * Returns true if the category was inserted, false if the same user already has a category with this name.
     */
    fun createIfAbsent(category: Category): Boolean

    fun update(category: Category): Category

    fun delete(id: UUID): Boolean

    fun existsByUserId(userId: Long): Boolean
}
