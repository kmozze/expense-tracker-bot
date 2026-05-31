package me.kmozze.expensetracker.repository

import me.kmozze.expense.tracker.jooq.tables.records.ExpenseRecord
import me.kmozze.expense.tracker.jooq.tables.references.EXPENSE
import me.kmozze.expensetracker.model.domain.Money
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
            amount = Money.of(this.amount),
            userId = this.userId,
            expenseDate = this.expenseDate,
            description = this.description,
            createdAt = this.createdAt,
        )

    override fun findByIdForUser(
        id: UUID,
        userId: Long,
    ): Expense? =
        dsl
            .selectFrom(EXPENSE)
            .where(EXPENSE.ID.eq(id).and(EXPENSE.USER_ID.eq(userId)))
            .fetchOne()
            ?.toDomain()

    override fun create(expense: Expense): Expense =
        dsl
            .insertInto(EXPENSE)
            .set(EXPENSE.ID, expense.id)
            .set(EXPENSE.AMOUNT, expense.amount.value)
            .set(EXPENSE.CATEGORY_ID, expense.categoryId)
            .set(EXPENSE.USER_ID, expense.userId)
            .set(EXPENSE.EXPENSE_DATE, expense.expenseDate)
            .set(EXPENSE.DESCRIPTION, expense.description)
            .returning()
            .fetchSingle()
            .toDomain()

    override fun updateForUser(
        expense: Expense,
        userId: Long,
    ): Expense? =
        dsl
            .update(EXPENSE)
            .set(EXPENSE.AMOUNT, expense.amount.value)
            .set(EXPENSE.CATEGORY_ID, expense.categoryId)
            .set(EXPENSE.EXPENSE_DATE, expense.expenseDate)
            .set(EXPENSE.DESCRIPTION, expense.description)
            .where(EXPENSE.ID.eq(expense.id).and(EXPENSE.USER_ID.eq(userId)))
            .returning()
            .fetchOne()
            ?.toDomain()

    override fun deleteByIdForUser(
        id: UUID,
        userId: Long,
    ): Boolean {
        val affectedRows =
            dsl
                .deleteFrom(EXPENSE)
                .where(EXPENSE.ID.eq(id).and(EXPENSE.USER_ID.eq(userId)))
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
            .orderBy(EXPENSE.CREATED_AT.desc(), EXPENSE.ID.desc())
            .fetch()
            .map { it.toDomain() }

    override fun existsByCategoryId(categoryId: UUID): Boolean =
        dsl.fetchExists(
            dsl
                .selectFrom(EXPENSE)
                .where(EXPENSE.CATEGORY_ID.eq(categoryId)),
        )
}
