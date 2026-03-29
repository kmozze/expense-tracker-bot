package me.kmozze.expensetracker.repository

import me.kmozze.expensetracker.model.Category
import java.util.UUID

interface ICategoryRepository {
    fun findByNameAndChatId(name: String, chatId: Long): Category?
    fun findById(id: UUID): Category?
    fun save(category: Category): Category?
    fun update(category: Category): Category?
    fun delete(id: UUID)
    fun hasExpenses(categoryId: UUID): Boolean
}