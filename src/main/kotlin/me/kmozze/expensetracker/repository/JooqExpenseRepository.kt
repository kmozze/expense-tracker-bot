package me.kmozze.expensetracker.repository

import me.kmozze.expense.tracker.jooq.tables.records.ExpenseRecord
import me.kmozze.expense.tracker.jooq.tables.references.EXPENSE
import me.kmozze.expensetracker.model.entity.Expense
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
            userId = this.userId,
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
            .set(EXPENSE.USER_ID, expense.userId)
            .set(EXPENSE.DESCRIPTION, expense.description)
            .returning()
            .fetchSingle()
            .toDomain()

    override fun update(expense: Expense): Expense =
        dsl
            .update(EXPENSE)
            .set(EXPENSE.AMOUNT, expense.amount)
            .set(EXPENSE.CATEGORY_ID, expense.categoryId)
            .set(EXPENSE.DESCRIPTION, expense.description)
            .where(EXPENSE.ID.eq(expense.id))
            .returning()
            .fetchSingle()
            .toDomain()

    override fun delete(id: UUID): Boolean {
        val affectedRows =
            dsl
                .deleteFrom(EXPENSE)
                .where(EXPENSE.ID.eq(id))
                .execute()

        return affectedRows > 0
    }

    override fun findAllByUserIdAndPeriod(
        userId: Long,
        from: OffsetDateTime,
        to: OffsetDateTime,
    ): List<Expense> =
        dsl
            .selectFrom(EXPENSE)
            .where(EXPENSE.USER_ID.eq(userId))
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
