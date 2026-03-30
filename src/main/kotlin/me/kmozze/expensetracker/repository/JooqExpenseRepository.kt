package me.kmozze.expensetracker.repository

import me.kmozze.expense.tracker.jooq.Tables.EXPENSE
import me.kmozze.expense.tracker.jooq.tables.records.ExpenseRecord
import me.kmozze.expensetracker.model.Expense
import org.jooq.DSLContext
import org.springframework.stereotype.Repository
import java.time.OffsetDateTime
import java.util.UUID

@Repository
class JooqExpenseRepository (
    private val dsl: DSLContext
) : IExpenseRepository {

    private fun ExpenseRecord.toDomain(): Expense = Expense(
        id = this.id,
        categoryId = this.categoryId,
        amount = this.amount,
        chatId = this.chatId,
        description = this.description,
        createdAt = this.createdAt
    )

    override fun findById(id: UUID): Expense? {
        return dsl.selectFrom(EXPENSE)
            .where(EXPENSE.ID.eq(id))
            .fetchOne()
            ?.toDomain()
    }

    override fun create(expense: Expense): Expense? {
        return dsl.insertInto(EXPENSE)
            .set(EXPENSE.ID, expense.id)
            .set(EXPENSE.AMOUNT, expense.amount)
            .set(EXPENSE.CATEGORY_ID, expense.categoryId)
            .set(EXPENSE.CHAT_ID, expense.chatId)
            .set(EXPENSE.DESCRIPTION, expense.description)
            .returning()
            .fetchOne()
            ?.toDomain()
    }

    override fun update(expense: Expense): Expense? {
        return dsl.update(EXPENSE)
            .set(EXPENSE.AMOUNT, expense.amount)
            .set(EXPENSE.CATEGORY_ID, expense.categoryId)
            .set(EXPENSE.DESCRIPTION, expense.description)
            .where(EXPENSE.ID.eq(expense.id))
            .returning()
            .fetchOne()
            ?.toDomain()
    }

    override fun delete(id: UUID) {
        dsl.deleteFrom(EXPENSE)
            .where(EXPENSE.ID.eq(id))
            .execute()
    }

    override fun findAllByChatIdAndPeriod(
        chatId: Long,
        from: OffsetDateTime,
        to: OffsetDateTime
    ): List<Expense> {
        return dsl.selectFrom(EXPENSE)
            .where(EXPENSE.CHAT_ID.eq(chatId))
            .and(EXPENSE.CREATED_AT.between(from, to))
            .orderBy(EXPENSE.CREATED_AT.desc())
            .fetch()
            .map { it.toDomain() }
    }

    override fun existsByCategoryId(categoryId: UUID): Boolean {
        return dsl.fetchExists(
            dsl.selectFrom(EXPENSE)
                .where(EXPENSE.CATEGORY_ID.eq(categoryId))
        )
    }
}
