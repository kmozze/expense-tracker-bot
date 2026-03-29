package me.kmozze.expensetracker.repository

import me.kmozze.expense.tracker.jooq.tables.references.EXPENSE
import me.kmozze.expensetracker.model.Expense
import org.jooq.DSLContext
import org.springframework.stereotype.Repository
import java.time.OffsetDateTime
import java.util.UUID

@Repository
class JooqExpenseRepository (
    private val dsl: DSLContext
) : IExpenseRepository {

    override fun findById(id: UUID): Expense? {
        return dsl.selectFrom(EXPENSE)
            .where(EXPENSE.ID.eq(id))
            .fetchOneInto(Expense::class.java)
    }

    override fun save(expense: Expense): Expense? {
        return dsl.insertInto(EXPENSE)
            .set(EXPENSE.ID, expense.id)
            .set(EXPENSE.AMOUNT, expense.amount)
            .set(EXPENSE.CATEGORY_ID, expense.categoryId)
            .set(EXPENSE.CHAT_ID, expense.chatId)
            .set(EXPENSE.DESCRIPTION, expense.description)
            .returning()
            .fetchOneInto(Expense::class.java)
    }

    override fun update(expense: Expense): Expense? {
        return dsl.update(EXPENSE)
            .set(EXPENSE.AMOUNT, expense.amount)
            .set(EXPENSE.CATEGORY_ID, expense.categoryId)
            .set(EXPENSE.DESCRIPTION, expense.description)
            .where(EXPENSE.ID.eq(expense.id))
            .returning()
            .fetchOneInto(Expense::class.java)
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
            .fetchInto(Expense::class.java)
    }
}