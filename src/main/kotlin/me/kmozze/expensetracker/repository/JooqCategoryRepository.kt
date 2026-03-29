package me.kmozze.expensetracker.repository


import me.kmozze.expense.tracker.jooq.tables.references.CATEGORY
import me.kmozze.expense.tracker.jooq.tables.references.EXPENSE
import me.kmozze.expensetracker.model.Category
import org.jooq.DSLContext
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
class JooqCategoryRepository (
    private val dsl: DSLContext,
) : ICategoryRepository {

    override fun findById(id: UUID): Category? {
        return dsl.selectFrom(CATEGORY)
            .where(CATEGORY.ID.eq(id))
            .fetchOneInto(Category::class.java)
    }

    override fun findByNameAndChatId(name: String, chatId: Long): Category? {
        return dsl.selectFrom(CATEGORY)
            .where(CATEGORY.NAME.eq(name))
            .and(CATEGORY.CHAT_ID.eq(chatId))
            .fetchOneInto(Category::class.java)
    }

    override fun save(category: Category): Category? {
        return dsl.insertInto(CATEGORY)
            .set(CATEGORY.ID, category.id)
            .set(CATEGORY.NAME, category.name)
            .set(CATEGORY.CHAT_ID, category.chatId)
            .returning()
            .fetchOneInto(Category::class.java)
    }

    override fun update(category: Category): Category? {
        return dsl.update(CATEGORY)
            .set(CATEGORY.NAME, category.name)
            .where(CATEGORY.ID.eq(category.id))
            .returning()
            .fetchOneInto(Category::class.java)
    }

    override fun delete(id: UUID) {
        dsl.deleteFrom(CATEGORY)
            .where(CATEGORY.ID.eq(id))
            .execute()
    }

    override fun hasExpenses(categoryId: UUID): Boolean {
        return dsl.fetchExists(
            dsl.selectFrom(EXPENSE)
                .where(EXPENSE.CATEGORY_ID.eq(categoryId))
        )
    }

}