package me.kmozze.expensetracker.integration.repository

import com.github.database.rider.core.api.configuration.DBUnit
import com.github.database.rider.core.api.configuration.Orthography
import com.github.database.rider.core.api.dataset.DataSet
import com.github.database.rider.core.api.dataset.SeedStrategy
import com.github.database.rider.junit5.api.DBRider
import me.kmozze.expensetracker.integration.AbstractIntegrationTest
import me.kmozze.expensetracker.repository.IExpenseRepository
import org.assertj.core.api.Assertions.assertThat
import org.dbunit.ext.postgresql.PostgresqlDataTypeFactory
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.UUID

@DBRider
@DBUnit(
    caseInsensitiveStrategy = Orthography.LOWERCASE,
    dataTypeFactoryClass = PostgresqlDataTypeFactory::class,
)
@DataSet(
    value = ["datasets/repository/category-empty.yml"],
    strategy = SeedStrategy.INSERT,
    executeScriptsBefore = ["datasets/repository/expense-period.sql"],
    executeStatementsAfter = ["delete from expense", "delete from category"],
)
class ExpenseRepositoryTest : AbstractIntegrationTest() {
    @Autowired
    private lateinit var expenseRepository: IExpenseRepository

    @Test
    fun `find all by user id and period returns only matching expenses from newest to oldest`() {
        val userId = 40001L
        val from = OffsetDateTime.parse("2026-05-20T00:00:00Z")
        val to = OffsetDateTime.parse("2026-05-21T00:00:00Z")

        val result =
            expenseRepository.findAllByUserIdAndPeriod(
                userId = userId,
                from = from,
                to = to,
            )

        assertThat(result.map { it.id })
            .containsExactly(
                UUID.fromString("00000000-0000-0000-0000-000000000103"),
                UUID.fromString("00000000-0000-0000-0000-000000000102"),
                UUID.fromString("00000000-0000-0000-0000-000000000101"),
            )
        assertThat(result.first().expenseDate).isEqualTo(LocalDate.parse("2026-05-20"))
    }

    @Test
    fun `find all by user id and period returns empty list when nothing matches`() {
        val userId = 40003L

        val result =
            expenseRepository.findAllByUserIdAndPeriod(
                userId = userId,
                from = OffsetDateTime.parse("2026-05-21T00:00:00Z"),
                to = OffsetDateTime.parse("2026-05-22T00:00:00Z"),
            )

        assertThat(result).isEmpty()
    }

    @Test
    fun `find by id for user returns only own expense`() {
        val expenseId = UUID.fromString("00000000-0000-0000-0000-000000000101")

        val ownExpense =
            expenseRepository.findByIdForUser(
                id = expenseId,
                userId = 40001L,
            )
        val anotherUserExpense =
            expenseRepository.findByIdForUser(
                id = expenseId,
                userId = 40002L,
            )

        assertThat(ownExpense).isNotNull()
        assertThat(ownExpense?.id).isEqualTo(expenseId)
        assertThat(anotherUserExpense).isNull()
    }

    @Test
    fun `delete by id for user deletes only own expense`() {
        val expenseId = UUID.fromString("00000000-0000-0000-0000-000000000101")

        val deletedForAnotherUser =
            expenseRepository.deleteByIdForUser(
                id = expenseId,
                userId = 40002L,
            )
        val deletedForOwner =
            expenseRepository.deleteByIdForUser(
                id = expenseId,
                userId = 40001L,
            )

        assertThat(deletedForAnotherUser).isFalse()
        assertThat(deletedForOwner).isTrue()
        assertThat(expenseRepository.findByIdForUser(expenseId, 40001L)).isNull()
    }

    @Test
    fun `find list items uses expense date category filter projection and pagination`() {
        val categoryId = UUID.fromString("00000000-0000-0000-0000-000000000003")

        val result =
            expenseRepository.findListItemsForUser(
                userId = 40001L,
                from = LocalDate.parse("2026-05-20"),
                to = LocalDate.parse("2026-05-21"),
                categoryId = categoryId,
                limit = 1,
                offset = 0,
            )
        val count =
            expenseRepository.countListItemsForUser(
                userId = 40001L,
                from = LocalDate.parse("2026-05-20"),
                to = LocalDate.parse("2026-05-21"),
                categoryId = categoryId,
            )

        assertThat(result).hasSize(1)
        assertThat(result.single().expenseId).isEqualTo(UUID.fromString("00000000-0000-0000-0000-000000000108"))
        assertThat(result.single().categoryName).isEqualTo("Транспорт")
        assertThat(
            result
                .single()
                .amount
                .value
                .toPlainString(),
        ).isEqualTo("800.00")
        assertThat(count).isEqualTo(2)
    }
}
