package me.kmozze.expensetracker.service

import me.kmozze.expensetracker.exception.SystemErrorCode
import me.kmozze.expensetracker.exception.exception
import me.kmozze.expensetracker.model.entity.Category
import me.kmozze.expensetracker.repository.ICategoryRepository
import org.jooq.exception.DataAccessException
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
class CategoryService(
    private val categoryRepository: ICategoryRepository,
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    private val defaultCategories =
        listOf(
            "Еда",
            "Транспорт",
            "Жильё",
            "Развлечения",
            "Прочее",
        )

    @Transactional
    fun initDefaultCategories(userId: Long): Boolean {
        return try {
            if (categoryRepository.existsByUserId(userId)) {
                logger.debug("User {} already has categories. Skipping initialization.", userId)
                return false
            }

            logger.info("Starting default categories initialization for user {}", userId)

            val createdCount = createMissingDefaultCategories(userId)

            logger.info("Successfully initialized {} categories for user {}", createdCount, userId)
            createdCount > 0
        } catch (e: DataAccessException) {
            logger.error("Failed to initialize categories for user $userId", e)

            throw SystemErrorCode.DATABASE_ERROR.exception(
                customMessage = "Ошибка при создании базовых категорий для пользователя $userId",
                cause = e,
            )
        }
    }

    fun getCategories(userId: Long): List<Category> =
        try {
            categoryRepository.findAllByUserId(userId)
        } catch (e: DataAccessException) {
            logger.error("Failed to load categories for user $userId", e)

            throw SystemErrorCode.DATABASE_ERROR.exception(
                customMessage = "Ошибка при получении категорий пользователя $userId",
                cause = e,
            )
        }

    fun findCategoryForUser(
        categoryId: UUID,
        userId: Long,
    ): Category? =
        try {
            categoryRepository.findByIdForUser(categoryId, userId)
        } catch (e: DataAccessException) {
            logger.error("Failed to load category $categoryId for user $userId", e)

            throw SystemErrorCode.DATABASE_ERROR.exception(
                customMessage = "Ошибка при получении категории $categoryId",
                cause = e,
            )
        }

    private fun createMissingDefaultCategories(userId: Long): Int {
        var createdCount = 0

        for (name in defaultCategories) {
            val category =
                Category(
                    id = UUID.randomUUID(),
                    name = name,
                    userId = userId,
                )

            if (categoryRepository.createIfAbsent(category)) {
                createdCount++
            }
        }

        return createdCount
    }
}
