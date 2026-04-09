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

            defaultCategories.forEach { name ->
                val category =
                    Category(
                        id = UUID.randomUUID(),
                        name = name,
                        userId = userId,
                    )
                categoryRepository.create(category)
            }

            logger.info("Successfully initialized {} categories for user {}", defaultCategories.size, userId)
            true
        } catch (e: DataAccessException) {
            logger.error("Failed to initialize categories for user $userId", e)

            throw SystemErrorCode.DATABASE_ERROR.exception(
                customMessage = "Ошибка при создании базовых категорий для пользователя $userId",
                cause = e,
            )
        }
    }
}
