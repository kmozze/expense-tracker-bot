package me.kmozze.expensetracker.repository

import me.kmozze.expense.tracker.jooq.tables.records.ExpenseRecord
import me.kmozze.expense.tracker.jooq.tables.references.EXPENSE
import me.kmozze.expensetracker.exception.DatabaseOperationException
import me.kmozze.expensetracker.exception.EntityNotFoundException
import me.kmozze.expensetracker.model.Expense
import org.jooq.DSLContext
import org.springframework.stereotype.Repository
import java.time.OffsetDateTime
import java.util.UUID

@Repository
class JooqExpenseRepository(
    private val dsl: DSLContext,
) : IExpenseRepository {
    private fun ExpenseRecord.toDomain(): Expense =
        Expense(
            id = this.id,
            categoryId = this.categoryId,
            amount = this.amount,
            chatId = this.chatId,
            description = this.description,
            createdAt = this.createdAt,
        )

    override fun findById(id: UUID): Expense? =
        dsl
            .selectFrom(EXPENSE)
            .where(EXPENSE.ID.eq(id))
            .fetchOne()
            ?.toDomain()

    override fun create(expense: Expense): Expense =
        dsl
            .insertInto(EXPENSE)
            .set(EXPENSE.ID, expense.id)
            .set(EXPENSE.AMOUNT, expense.amount)
            .set(EXPENSE.CATEGORY_ID, expense.categoryId)
            .set(EXPENSE.CHAT_ID, expense.chatId)
            .set(EXPENSE.DESCRIPTION, expense.description)
            .returning()
            .fetchOne()
            ?.toDomain()
            ?: throw DatabaseOperationException("Failed to create expense: $expense")

    override fun update(expense: Expense): Expense =
        dsl
            .update(EXPENSE)
            .set(EXPENSE.AMOUNT, expense.amount)
            .set(EXPENSE.CATEGORY_ID, expense.categoryId)
            .set(EXPENSE.DESCRIPTION, expense.description)
            .where(EXPENSE.ID.eq(expense.id))
            .returning()
            .fetchOne()
            ?.toDomain()
            ?: throw EntityNotFoundException("Expense not found for update with id: ${expense.id}")

    override fun delete(id: UUID) {
        dsl
            .deleteFrom(EXPENSE)
            .where(EXPENSE.ID.eq(id))
            .execute()
    }

    override fun findAllByChatIdAndPeriod(
        chatId: Long,
        from: OffsetDateTime,
        to: OffsetDateTime,
    ): List<Expense> =
        dsl
            .selectFrom(EXPENSE)
            .where(EXPENSE.CHAT_ID.eq(chatId))
            .and(EXPENSE.CREATED_AT.ge(from))
            .and(EXPENSE.CREATED_AT.lt(to))
            .fetch()
            .map { it.toDomain() }

    override fun existsByCategoryId(categoryId: UUID): Boolean =
        dsl.fetchExists(
            dsl
                .selectFrom(EXPENSE)
                .where(EXPENSE.CATEGORY_ID.eq(categoryId)),
        )
}
