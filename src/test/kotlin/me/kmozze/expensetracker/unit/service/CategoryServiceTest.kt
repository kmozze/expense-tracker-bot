package me.kmozze.expensetracker.unit.service

import io.mockk.every
import io.mockk.junit5.MockKExtension
import io.mockk.mockk
import io.mockk.verify
import me.kmozze.expensetracker.exception.BusinessErrorCode
import me.kmozze.expensetracker.exception.BusinessException
import me.kmozze.expensetracker.exception.SystemErrorCode
import me.kmozze.expensetracker.exception.SystemException
import me.kmozze.expensetracker.model.entity.Category
import me.kmozze.expensetracker.repository.ICategoryRepository
import me.kmozze.expensetracker.service.CategoryService
import org.jooq.exception.DataAccessException
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import java.util.UUID

@ExtendWith(MockKExtension::class)
class CategoryServiceTest {
    private val categoryRepository: ICategoryRepository = mockk(relaxed = true)
    private lateinit var service: CategoryService

    @BeforeEach
    fun setUp() {
        service = CategoryService(categoryRepository)
    }

    @Test
    fun `user already has categories`() {
        every { categoryRepository.existsByUserId(123L) } returns true

        val result = service.initDefaultCategories(123L)

        assertFalse(result, "Должен вернуть false при повторной инициализации")
        verify(exactly = 0) { categoryRepository.create(any()) }
    }

    @Test
    fun `create 5 default categories`() {
        every { categoryRepository.existsByUserId(123L) } returns false
        every { categoryRepository.create(any()) } returns mockk()

        val result = service.initDefaultCategories(123L)

        assertTrue(result, "Должен вернуть true при первом запуске")
        verify(exactly = 5) { categoryRepository.create(any()) }
    }

    @Test
    fun `wrap DataAccessException into SystemException`() {
        every { categoryRepository.existsByUserId(123L) } returns false
        every { categoryRepository.create(any()) } throws DataAccessException("DB connection failed")

        val exception =
            assertThrows<SystemException> {
                service.initDefaultCategories(123L)
            }

        assertEquals(SystemErrorCode.DATABASE_ERROR, exception.errorCode)
    }

    @Test
    fun `get category for user returns category found by id in user scope`() {
        val categoryId = UUID.randomUUID()
        val userId = 123L
        val category =
            Category(
                id = categoryId,
                name = "Еда",
                userId = userId,
            )
        every { categoryRepository.findByIdForUser(categoryId, userId) } returns category

        val result = service.getCategoryForUser(categoryId, userId)

        assertEquals(category, result)
        verify(exactly = 1) { categoryRepository.findByIdForUser(categoryId, userId) }
        verify(exactly = 0) { categoryRepository.findById(categoryId) }
    }

    @Test
    fun `get category for user throws not found when category does not belong to user`() {
        val categoryId = UUID.randomUUID()
        val userId = 123L
        every { categoryRepository.findByIdForUser(categoryId, userId) } returns null

        val exception =
            assertThrows<BusinessException> {
                service.getCategoryForUser(categoryId, userId)
            }

        assertEquals(BusinessErrorCode.CATEGORY_NOT_FOUND, exception.errorCode)
    }

    @Test
    fun `get category for user wraps DataAccessException into SystemException`() {
        val categoryId = UUID.randomUUID()
        val userId = 123L
        val cause = DataAccessException("DB connection failed")
        every { categoryRepository.findByIdForUser(categoryId, userId) } throws cause

        val exception =
            assertThrows<SystemException> {
                service.getCategoryForUser(categoryId, userId)
            }

        assertEquals(SystemErrorCode.DATABASE_ERROR, exception.errorCode)
        assertEquals(cause, exception.cause)
    }
}
