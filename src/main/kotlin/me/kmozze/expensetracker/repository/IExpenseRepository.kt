package me.kmozze.expensetracker.repository

import me.kmozze.expensetracker.model.Expense
import java.time.OffsetDateTime
import java.util.UUID

interface IExpenseRepository {
    fun save(expense: Expense): Expense?
    fun update(expense: Expense): Expense?
    fun delete(id: UUID)
    fun findById(id: UUID): Expense?
    fun findAllByChatIdAndPeriod(chatId: Long, from: OffsetDateTime, to: OffsetDateTime): List<Expense>
}