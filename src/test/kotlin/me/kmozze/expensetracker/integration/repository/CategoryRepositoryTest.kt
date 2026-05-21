package me.kmozze.expensetracker.integration.repository

import me.kmozze.expensetracker.integration.AbstractIntegrationTest
import me.kmozze.expensetracker.model.entity.Category
import me.kmozze.expensetracker.repository.ICategoryRepository
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Transactional
class CategoryRepositoryTest : AbstractIntegrationTest() {
    @Autowired
    private lateinit var categoryRepository: ICategoryRepository

    @Test
    fun `create if absent ignores duplicate category name for user`() {
        val userId = 30001L
        val category =
            Category(
                id = UUID.randomUUID(),
                name = "Еда",
                userId = userId,
            )
        val duplicate =
            Category(
                id = UUID.randomUUID(),
                name = category.name,
                userId = userId,
            )

        val created = categoryRepository.createIfAbsent(category)
        val duplicateCreated = categoryRepository.createIfAbsent(duplicate)

        val categories = categoryRepository.findAllByUserId(userId)
        assertThat(created).isTrue()
        assertThat(duplicateCreated).isFalse()
        assertThat(categories).hasSize(1)
        assertThat(categories.single().id).isEqualTo(category.id)
    }
}
