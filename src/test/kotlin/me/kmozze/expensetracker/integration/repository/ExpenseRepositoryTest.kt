package me.kmozze.expensetracker.integration.repository

import me.kmozze.expense.tracker.jooq.tables.references.EXPENSE
import me.kmozze.expensetracker.integration.AbstractIntegrationTest
import me.kmozze.expensetracker.model.domain.Money
import me.kmozze.expensetracker.model.entity.Category
import me.kmozze.expensetracker.model.entity.Expense
import me.kmozze.expensetracker.repository.ICategoryRepository
import me.kmozze.expensetracker.repository.IExpenseRepository
import org.assertj.core.api.Assertions.assertThat
import org.jooq.DSLContext
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.time.OffsetDateTime
import java.util.UUID

@Transactional
class ExpenseRepositoryTest : AbstractIntegrationTest() {
    @Autowired
    private lateinit var categoryRepository: ICategoryRepository

    @Autowired
    private lateinit var expenseRepository: IExpenseRepository

    @Autowired
    private lateinit var dsl: DSLContext

    @Test
    fun `find all by user id and period returns expenses from newest to oldest`() {
        val userId = 40001L
        val category =
            categoryRepository.create(
                Category(
                    name = "Еда",
                    userId = userId,
                ),
            )
        val oldest =
            createExpense(
                userId = userId,
                categoryId = category.id,
                amount = "100.00",
                createdAt = OffsetDateTime.parse("2026-05-20T10:00:00Z"),
            )
        val newest =
            createExpense(
                userId = userId,
                categoryId = category.id,
                amount = "300.00",
                createdAt = OffsetDateTime.parse("2026-05-20T12:00:00Z"),
            )
        val middle =
            createExpense(
                userId = userId,
                categoryId = category.id,
                amount = "200.00",
                createdAt = OffsetDateTime.parse("2026-05-20T11:00:00Z"),
            )

        val result =
            expenseRepository.findAllByUserIdAndPeriod(
                userId = userId,
                from = OffsetDateTime.parse("2026-05-20T00:00:00Z"),
                to = OffsetDateTime.parse("2026-05-21T00:00:00Z"),
            )

        assertThat(result.map { it.id }).containsExactly(newest.id, middle.id, oldest.id)
    }

    private fun createExpense(
        userId: Long,
        categoryId: UUID,
        amount: String,
        createdAt: OffsetDateTime,
    ): Expense {
        val expense =
            expenseRepository.create(
                Expense(
                    categoryId = categoryId,
                    amount = Money.of(BigDecimal(amount)),
                    userId = userId,
                ),
            )

        dsl
            .update(EXPENSE)
            .set(EXPENSE.CREATED_AT, createdAt)
            .where(EXPENSE.ID.eq(expense.id))
            .execute()

        return expense.copy(createdAt = createdAt)
    }
}
