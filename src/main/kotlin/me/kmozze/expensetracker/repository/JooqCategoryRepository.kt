package me.kmozze.expensetracker.repository


import me.kmozze.expense.tracker.jooq.Tables.CATEGORY
import me.kmozze.expense.tracker.jooq.tables.records.CategoryRecord
import me.kmozze.expensetracker.model.Category
import org.jooq.DSLContext
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
class JooqCategoryRepository (
    private val dsl: DSLContext,
) : ICategoryRepository {

    private fun CategoryRecord.toDomain(): Category = Category(
        id = this.id,
        name = this.name,
        chatId = this.chatId,
        createdAt = this.createdAt,
        updatedAt = this.updatedAt
    )

    override fun findById(id: UUID): Category? {
        return dsl.selectFrom(CATEGORY)
            .where(CATEGORY.ID.eq(id))
            .fetchOne()
            ?.toDomain()
    }

    override fun findByNameAndChatId(name: String, chatId: Long): Category? {
        return dsl.selectFrom(CATEGORY)
            .where(CATEGORY.NAME.eq(name))
            .and(CATEGORY.CHAT_ID.eq(chatId))
            .fetchOne()
            ?.toDomain()
    }

    override fun create(category: Category): Category? {
        return dsl.insertInto(CATEGORY)
            .set(CATEGORY.ID, category.id)
            .set(CATEGORY.NAME, category.name)
            .set(CATEGORY.CHAT_ID, category.chatId)
            .returning()
            .fetchOne()
            ?.toDomain()
    }

    override fun update(category: Category): Category? {
        return dsl.update(CATEGORY)
            .set(CATEGORY.NAME, category.name)
            .where(CATEGORY.ID.eq(category.id))
            .returning()
            .fetchOne()
            ?.toDomain()
    }

    override fun delete(id: UUID) {
        dsl.deleteFrom(CATEGORY)
            .where(CATEGORY.ID.eq(id))
            .execute()
    }
}
