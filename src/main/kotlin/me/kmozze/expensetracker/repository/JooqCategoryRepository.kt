package me.kmozze.expensetracker.repository

import me.kmozze.expense.tracker.jooq.tables.records.CategoryRecord
import me.kmozze.expense.tracker.jooq.tables.references.CATEGORY
import me.kmozze.expensetracker.model.entity.Category
import org.jooq.DSLContext
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
class JooqCategoryRepository(
    private val dsl: DSLContext,
) : ICategoryRepository {
    private fun CategoryRecord.toDomain(): Category =
        Category(
            id = this.id,
            name = this.name,
            userId = this.userId,
            createdAt = this.createdAt,
            updatedAt = this.updatedAt,
        )

    override fun findById(id: UUID): Category =
        dsl
            .selectFrom(CATEGORY)
            .where(CATEGORY.ID.eq(id))
            .fetchSingle()
            .toDomain()

    override fun create(category: Category): Category =
        dsl
            .insertInto(CATEGORY)
            .set(CATEGORY.ID, category.id)
            .set(CATEGORY.NAME, category.name)
            .set(CATEGORY.USER_ID, category.userId)
            .returning()
            .fetchSingle()
            .toDomain()

    override fun update(category: Category): Category =
        dsl
            .update(CATEGORY)
            .set(CATEGORY.NAME, category.name)
            .where(CATEGORY.ID.eq(category.id))
            .returning()
            .fetchSingle()
            .toDomain()

    override fun delete(id: UUID) {
        dsl
            .deleteFrom(CATEGORY)
            .where(CATEGORY.ID.eq(id))
            .execute()
    }

    override fun existsByUserId(userId: Long): Boolean =
        dsl.fetchExists(
            dsl
                .selectFrom(CATEGORY)
                .where(CATEGORY.USER_ID.eq(userId)),
        )
}
