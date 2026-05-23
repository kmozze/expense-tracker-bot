package me.kmozze.expensetracker.unit.service

import io.mockk.confirmVerified
import io.mockk.every
import io.mockk.junit5.MockKExtension
import io.mockk.mockk
import io.mockk.verify
import me.kmozze.expensetracker.exception.SystemErrorCode
import me.kmozze.expensetracker.exception.SystemException
import me.kmozze.expensetracker.model.entity.Category
import me.kmozze.expensetracker.repository.ICategoryRepository
import me.kmozze.expensetracker.service.CategoryService
import org.jooq.exception.DataAccessException
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import java.util.UUID

@ExtendWith(MockKExtension::class)
class CategoryServiceTest {
    private val categoryRepository: ICategoryRepository = mockk()
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
        verify(exactly = 1) { categoryRepository.existsByUserId(123L) }
        verify(exactly = 0) { categoryRepository.createIfAbsent(any()) }
        confirmVerified(categoryRepository)
    }

    @Test
    fun `create 5 default categories`() {
        val createdCategories = mutableListOf<Category>()
        every { categoryRepository.existsByUserId(123L) } returns false
        every { categoryRepository.createIfAbsent(capture(createdCategories)) } returns true

        val result = service.initDefaultCategories(123L)

        assertTrue(result, "Должен вернуть true при первом запуске")
        assertEquals(listOf("Еда", "Транспорт", "Жильё", "Развлечения", "Прочее"), createdCategories.map { it.name })
        assertTrue(createdCategories.all { it.userId == 123L })
        verify(exactly = 1) { categoryRepository.existsByUserId(123L) }
        verify(exactly = 5) { categoryRepository.createIfAbsent(any()) }
        confirmVerified(categoryRepository)
    }

    @Test
    fun `init default categories wraps DataAccessException into SystemException`() {
        val cause = DataAccessException("DB connection failed")
        every { categoryRepository.existsByUserId(123L) } returns false
        every { categoryRepository.createIfAbsent(any()) } throws cause

        val exception =
            assertThrows<SystemException> {
                service.initDefaultCategories(123L)
            }

        assertEquals(SystemErrorCode.DATABASE_ERROR, exception.errorCode)
        assertEquals(cause, exception.cause)
        verify(exactly = 1) { categoryRepository.existsByUserId(123L) }
        verify(exactly = 1) { categoryRepository.createIfAbsent(any()) }
        confirmVerified(categoryRepository)
    }

    @Test
    fun `get categories returns categories for user`() {
        val userId = 123L
        val categories =
            listOf(
                Category(name = "Еда", userId = userId),
                Category(name = "Транспорт", userId = userId),
            )
        every { categoryRepository.findAllByUserId(userId) } returns categories

        val result = service.getCategories(userId)

        assertEquals(categories, result)
        verify(exactly = 1) { categoryRepository.findAllByUserId(userId) }
        confirmVerified(categoryRepository)
    }

    @Test
    fun `get categories wraps DataAccessException into SystemException`() {
        val userId = 123L
        val cause = DataAccessException("DB connection failed")
        every { categoryRepository.findAllByUserId(userId) } throws cause

        val exception =
            assertThrows<SystemException> {
                service.getCategories(userId)
            }

        assertEquals(SystemErrorCode.DATABASE_ERROR, exception.errorCode)
        assertEquals(cause, exception.cause)
        verify(exactly = 1) { categoryRepository.findAllByUserId(userId) }
        confirmVerified(categoryRepository)
    }

    @Test
    fun `find category for user returns category found by id in user scope`() {
        val categoryId = UUID.randomUUID()
        val userId = 123L
        val category =
            Category(
                id = categoryId,
                name = "Еда",
                userId = userId,
            )
        every { categoryRepository.findByIdForUser(categoryId, userId) } returns category

        val result = service.findCategoryForUser(categoryId, userId)

        assertEquals(category, result)
        verify(exactly = 1) { categoryRepository.findByIdForUser(categoryId, userId) }
        confirmVerified(categoryRepository)
    }

    @Test
    fun `find category for user returns null when category does not belong to user`() {
        val categoryId = UUID.randomUUID()
        val userId = 123L
        every { categoryRepository.findByIdForUser(categoryId, userId) } returns null

        val result = service.findCategoryForUser(categoryId, userId)

        assertNull(result)
        verify(exactly = 1) { categoryRepository.findByIdForUser(categoryId, userId) }
        confirmVerified(categoryRepository)
    }

    @Test
    fun `find category for user wraps DataAccessException into SystemException`() {
        val categoryId = UUID.randomUUID()
        val userId = 123L
        val cause = DataAccessException("DB connection failed")
        every { categoryRepository.findByIdForUser(categoryId, userId) } throws cause

        val exception =
            assertThrows<SystemException> {
                service.findCategoryForUser(categoryId, userId)
            }

        assertEquals(SystemErrorCode.DATABASE_ERROR, exception.errorCode)
        assertEquals(cause, exception.cause)
        verify(exactly = 1) { categoryRepository.findByIdForUser(categoryId, userId) }
        confirmVerified(categoryRepository)
    }
}
