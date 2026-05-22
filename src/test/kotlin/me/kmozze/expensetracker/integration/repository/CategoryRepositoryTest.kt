package me.kmozze.expensetracker.integration.repository

import com.github.database.rider.core.api.configuration.DBUnit
import com.github.database.rider.core.api.configuration.Orthography
import com.github.database.rider.core.api.dataset.DataSet
import com.github.database.rider.core.api.dataset.ExpectedDataSet
import com.github.database.rider.junit5.api.DBRider
import me.kmozze.expensetracker.integration.AbstractIntegrationTest
import me.kmozze.expensetracker.model.entity.Category
import me.kmozze.expensetracker.repository.ICategoryRepository
import org.assertj.core.api.Assertions.assertThat
import org.dbunit.ext.postgresql.PostgresqlDataTypeFactory
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.util.UUID

@DBRider
@DBUnit(
    caseInsensitiveStrategy = Orthography.LOWERCASE,
    dataTypeFactoryClass = PostgresqlDataTypeFactory::class,
)
class CategoryRepositoryTest : AbstractIntegrationTest() {
    @Autowired
    private lateinit var categoryRepository: ICategoryRepository

    @Test
    @DataSet(
        value = ["datasets/repository/category-empty.yml"],
        cleanBefore = true,
        cleanAfter = true,
    )
    @ExpectedDataSet(
        value = ["datasets/repository/category-created.yml"],
        ignoreCols = ["created_at", "updated_at"],
    )
    fun `create if absent creates category when missing`() {
        val userId = 30001L
        val category =
            Category(
                id = UUID.fromString("00000000-0000-0000-0000-000000000301"),
                name = "Еда",
                userId = userId,
            )

        val created = categoryRepository.createIfAbsent(category)

        assertThat(created).isTrue()
    }

    @Test
    @DataSet(
        value = ["datasets/repository/category-created.yml"],
        cleanBefore = true,
        cleanAfter = true,
    )
    @ExpectedDataSet(
        value = ["datasets/repository/category-created.yml"],
        ignoreCols = ["created_at", "updated_at"],
    )
    fun `create if absent ignores duplicate category name for user`() {
        val userId = 30001L
        val duplicate =
            Category(
                id = UUID.randomUUID(),
                name = "Еда",
                userId = userId,
            )

        val duplicateCreated = categoryRepository.createIfAbsent(duplicate)

        val categories = categoryRepository.findAllByUserId(userId)
        assertThat(duplicateCreated).isFalse()
        assertThat(categories).hasSize(1)
        assertThat(categories.single().id).isEqualTo(
            UUID.fromString("00000000-0000-0000-0000-000000000301"),
        )
    }
}
