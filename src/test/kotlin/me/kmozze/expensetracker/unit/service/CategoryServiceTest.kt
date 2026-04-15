package me.kmozze.expensetracker.unit.service

import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.junit5.MockKExtension
import io.mockk.mockk
import io.mockk.verify
import me.kmozze.expensetracker.exception.SystemErrorCode
import me.kmozze.expensetracker.exception.SystemException
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

@ExtendWith(MockKExtension::class)
class CategoryServiceTest {
    private val categoryRepository: ICategoryRepository = mockk(relaxed = true)
    private lateinit var service: CategoryService

    @BeforeEach
    fun setUp() {
        clearAllMocks()
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

        assertEquals(SystemErrorCode.DATABASE_ERROR, exception.error)
    }
}
