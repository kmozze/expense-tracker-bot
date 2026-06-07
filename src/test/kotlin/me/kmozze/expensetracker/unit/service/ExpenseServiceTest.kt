package me.kmozze.expensetracker.unit.service

import io.mockk.confirmVerified
import io.mockk.every
import io.mockk.junit5.MockKExtension
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import me.kmozze.expensetracker.exception.BusinessErrorCode
import me.kmozze.expensetracker.exception.BusinessException
import me.kmozze.expensetracker.exception.SystemErrorCode
import me.kmozze.expensetracker.exception.SystemException
import me.kmozze.expensetracker.model.domain.ExpenseDraft
import me.kmozze.expensetracker.model.domain.Money
import me.kmozze.expensetracker.model.entity.Category
import me.kmozze.expensetracker.model.entity.Expense
import me.kmozze.expensetracker.repository.ICategoryRepository
import me.kmozze.expensetracker.repository.IExpenseRepository
import me.kmozze.expensetracker.service.ExpenseService
import me.kmozze.expensetracker.service.parser.InputExpenseDateParsingService
import me.kmozze.expensetracker.service.parser.InputExpenseParsingService
import org.jooq.exception.DataAccessException
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import java.math.BigDecimal
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.UUID

@ExtendWith(MockKExtension::class)
class ExpenseServiceTest {
    private val expenseTextParser: InputExpenseParsingService = mockk()
    private val expenseDateParser: InputExpenseDateParsingService = mockk()
    private val expenseRepository: IExpenseRepository = mockk()
    private val categoryRepository: ICategoryRepository = mockk()
    private lateinit var service: ExpenseService

    @BeforeEach
    fun setUp() {
        service =
            ExpenseService(
                expenseTextParser = expenseTextParser,
                expenseDateParser = expenseDateParser,
                expenseRepository = expenseRepository,
                categoryRepository = categoryRepository,
            )
    }

    @Test
    fun `parse expense returns draft from parser`() {
        val text = "500 такси"
        val expenseDraft = ExpenseDraft(EXPENSE_AMOUNT, "такси")
        every { expenseTextParser.parse(text) } returns expenseDraft

        val result = service.parseExpense(text)

        assertEquals(expenseDraft, result)
        verify(exactly = 1) { expenseTextParser.parse(text) }
        confirmVerified(expenseTextParser, expenseDateParser, expenseRepository)
    }

    @Test
    fun `parse expense date returns date from parser`() {
        val text = "24.05.2026"
        val expenseDate = LocalDate.parse("2026-05-24")
        every { expenseDateParser.parse(text) } returns expenseDate

        val result = service.parseExpenseDate(text)

        assertEquals(expenseDate, result)
        verify(exactly = 1) { expenseDateParser.parse(text) }
        confirmVerified(expenseTextParser, expenseDateParser, expenseRepository)
    }

    @Test
    fun `find expense for user returns repository result`() {
        val userId = 123L
        val expenseId = UUID.randomUUID()
        val expense =
            Expense(
                id = expenseId,
                categoryId = UUID.randomUUID(),
                amount = EXPENSE_AMOUNT,
                userId = userId,
                expenseDate = LocalDate.parse("2026-05-24"),
            )
        every { expenseRepository.findByIdForUser(expenseId, userId) } returns expense

        val result = service.findExpenseForUser(userId = userId, expenseId = expenseId)

        assertEquals(expense, result)
        verify(exactly = 1) { expenseRepository.findByIdForUser(expenseId, userId) }
        confirmVerified(expenseTextParser, expenseDateParser, expenseRepository)
    }

    @Test
    fun `find expense for user wraps DataAccessException into SystemException`() {
        val userId = 123L
        val expenseId = UUID.randomUUID()
        val cause = DataAccessException("DB connection failed")
        every { expenseRepository.findByIdForUser(expenseId, userId) } throws cause

        val exception =
            assertThrows<SystemException> {
                service.findExpenseForUser(userId = userId, expenseId = expenseId)
            }

        assertEquals(SystemErrorCode.DATABASE_ERROR, exception.errorCode)
        assertEquals(cause, exception.cause)
        verify(exactly = 1) { expenseRepository.findByIdForUser(expenseId, userId) }
        confirmVerified(expenseTextParser, expenseDateParser, expenseRepository)
    }

    @Test
    fun `update expense from draft updates all editable fields`() {
        val userId = 123L
        val expenseId = UUID.randomUUID()
        val originalCategoryId = UUID.randomUUID()
        val updatedCategoryId = UUID.randomUUID()
        val original =
            Expense(
                id = expenseId,
                categoryId = originalCategoryId,
                amount = EXPENSE_AMOUNT,
                userId = userId,
                expenseDate = LocalDate.parse("2026-05-24"),
                description = "такси",
            )
        val draft =
            ExpenseDraft(
                amount = Money.of(BigDecimal("650.00")),
                description = "автобус",
                categoryId = updatedCategoryId,
                expenseDate = LocalDate.parse("2026-05-20"),
            )
        val updated =
            original.copy(
                amount = draft.amount,
                categoryId = updatedCategoryId,
                expenseDate = draft.requireExpenseDate(),
                description = "автобус",
            )
        every { expenseRepository.findByIdForUser(expenseId, userId) } returns original
        every { categoryRepository.findByIdForUser(updatedCategoryId, userId) } returns
            Category(updatedCategoryId, "Транспорт", userId)
        every { expenseRepository.updateForUser(updated, userId) } returns updated

        val result =
            service.updateExpenseFromDraftForUser(
                userId = userId,
                expenseId = expenseId,
                expenseDraft = draft,
            )

        assertEquals(updated, result)
        verify(exactly = 1) { expenseRepository.findByIdForUser(expenseId, userId) }
        verify(exactly = 1) { categoryRepository.findByIdForUser(updatedCategoryId, userId) }
        verify(exactly = 1) { expenseRepository.updateForUser(updated, userId) }
        confirmVerified(expenseTextParser, expenseDateParser, expenseRepository, categoryRepository)
    }

    @Test
    fun `update expense from draft returns null when expense missing`() {
        val userId = 123L
        val expenseId = UUID.randomUUID()
        val categoryId = UUID.randomUUID()
        val draft = completeDraft(categoryId)
        every { expenseRepository.findByIdForUser(expenseId, userId) } returns null

        val result = service.updateExpenseFromDraftForUser(userId, expenseId, draft)

        assertNull(result)
        verify(exactly = 1) { expenseRepository.findByIdForUser(expenseId, userId) }
        verify(exactly = 0) { categoryRepository.findByIdForUser(any(), any()) }
        verify(exactly = 0) { expenseRepository.updateForUser(any(), any()) }
        confirmVerified(expenseTextParser, expenseDateParser, expenseRepository, categoryRepository)
    }

    @Test
    fun `update expense from draft returns null when category does not belong to user`() {
        val userId = 123L
        val expenseId = UUID.randomUUID()
        val categoryId = UUID.randomUUID()
        val original =
            Expense(
                id = expenseId,
                categoryId = UUID.randomUUID(),
                amount = EXPENSE_AMOUNT,
                userId = userId,
                expenseDate = LocalDate.parse("2026-05-24"),
            )
        val draft = completeDraft(categoryId)
        every { expenseRepository.findByIdForUser(expenseId, userId) } returns original
        every { categoryRepository.findByIdForUser(categoryId, userId) } returns null

        val result = service.updateExpenseFromDraftForUser(userId, expenseId, draft)

        assertNull(result)
        verify(exactly = 1) { expenseRepository.findByIdForUser(expenseId, userId) }
        verify(exactly = 1) { categoryRepository.findByIdForUser(categoryId, userId) }
        verify(exactly = 0) { expenseRepository.updateForUser(any(), any()) }
        confirmVerified(expenseTextParser, expenseDateParser, expenseRepository, categoryRepository)
    }

    @Test
    fun `update expense from draft normalizes blank description`() {
        val userId = 123L
        val expenseId = UUID.randomUUID()
        val categoryId = UUID.randomUUID()
        val original =
            Expense(
                id = expenseId,
                categoryId = categoryId,
                amount = EXPENSE_AMOUNT,
                userId = userId,
                expenseDate = LocalDate.parse("2026-05-24"),
                description = "такси",
            )
        val draft = completeDraft(categoryId).copy(description = "   ")
        val updated =
            original.copy(
                amount = draft.amount,
                expenseDate = draft.requireExpenseDate(),
                description = null,
            )
        every { expenseRepository.findByIdForUser(expenseId, userId) } returns original
        every { categoryRepository.findByIdForUser(categoryId, userId) } returns Category(categoryId, "Транспорт", userId)
        every { expenseRepository.updateForUser(updated, userId) } returns updated

        val result = service.updateExpenseFromDraftForUser(userId, expenseId, draft)

        assertEquals(updated, result)
        verify(exactly = 1) { expenseRepository.findByIdForUser(expenseId, userId) }
        verify(exactly = 1) { categoryRepository.findByIdForUser(categoryId, userId) }
        verify(exactly = 1) { expenseRepository.updateForUser(updated, userId) }
        confirmVerified(expenseTextParser, expenseDateParser, expenseRepository, categoryRepository)
    }

    @Test
    fun `update expense from draft wraps DataAccessException into SystemException`() {
        val userId = 123L
        val expenseId = UUID.randomUUID()
        val categoryId = UUID.randomUUID()
        val cause = DataAccessException("DB update failed")
        every { expenseRepository.findByIdForUser(expenseId, userId) } throws cause

        val exception =
            assertThrows<SystemException> {
                service.updateExpenseFromDraftForUser(
                    userId = userId,
                    expenseId = expenseId,
                    expenseDraft = completeDraft(categoryId),
                )
            }

        assertEquals(SystemErrorCode.DATABASE_ERROR, exception.errorCode)
        assertEquals(cause, exception.cause)
        verify(exactly = 1) { expenseRepository.findByIdForUser(expenseId, userId) }
        confirmVerified(expenseTextParser, expenseDateParser, expenseRepository, categoryRepository)
    }

    @Test
    fun `parse expense amount trims spaces`() {
        val amount = service.parseExpenseAmount(" 650.50 ")

        assertEquals(Money.of(BigDecimal("650.50")), amount)
    }

    @Test
    fun `parse expense amount rejects invalid text`() {
        val exception =
            assertThrows<BusinessException> {
                service.parseExpenseAmount("abc")
            }

        assertEquals(BusinessErrorCode.INVALID_AMOUNT, exception.errorCode)
    }

    @Test
    fun `parse expense amount rejects non-positive values`() {
        val exception =
            assertThrows<BusinessException> {
                service.parseExpenseAmount("0")
            }

        assertEquals(BusinessErrorCode.INVALID_AMOUNT, exception.errorCode)
    }

    @Test
    fun `save expense creates expense from complete draft`() {
        val userId = 123L
        val categoryId = UUID.randomUUID()
        val expenseDate = LocalDate.parse("2026-05-24")
        val expenseDraft =
            ExpenseDraft(
                amount = EXPENSE_AMOUNT,
                description = "такси",
                categoryId = categoryId,
                expenseDate = expenseDate,
            )
        val expenseSlot = slot<Expense>()
        val savedExpenseId = UUID.randomUUID()
        val savedAt = OffsetDateTime.parse("2026-05-22T10:00:00Z")
        every { expenseRepository.create(capture(expenseSlot)) } answers {
            expenseSlot.captured.copy(id = savedExpenseId, createdAt = savedAt)
        }

        val result =
            service.saveExpense(
                userId = userId,
                expenseDraft = expenseDraft,
            )

        val createdExpense = expenseSlot.captured
        assertEquals(categoryId, createdExpense.categoryId)
        assertEquals(EXPENSE_AMOUNT, createdExpense.amount)
        assertEquals(userId, createdExpense.userId)
        assertEquals(expenseDate, createdExpense.expenseDate)
        assertEquals("такси", createdExpense.description)
        assertNull(createdExpense.createdAt)

        assertEquals(savedExpenseId, result.id)
        assertEquals(categoryId, result.categoryId)
        assertEquals(EXPENSE_AMOUNT, result.amount)
        assertEquals(userId, result.userId)
        assertEquals(expenseDate, result.expenseDate)
        assertEquals("такси", result.description)
        assertEquals(savedAt, result.createdAt)
        verify(exactly = 1) { expenseRepository.create(any()) }
        confirmVerified(expenseTextParser, expenseDateParser, expenseRepository)
    }

    @Test
    fun `save expense wraps DataAccessException into SystemException`() {
        val userId = 123L
        val categoryId = UUID.randomUUID()
        val expenseDraft =
            ExpenseDraft(
                amount = EXPENSE_AMOUNT,
                description = "такси",
                categoryId = categoryId,
                expenseDate = LocalDate.parse("2026-05-24"),
            )
        val cause = DataAccessException("DB connection failed")
        every { expenseRepository.create(any()) } throws cause

        val exception =
            assertThrows<SystemException> {
                service.saveExpense(
                    userId = userId,
                    expenseDraft = expenseDraft,
                )
            }

        assertEquals(SystemErrorCode.DATABASE_ERROR, exception.errorCode)
        assertEquals(cause, exception.cause)
        verify(exactly = 1) { expenseRepository.create(any()) }
        confirmVerified(expenseTextParser, expenseDateParser, expenseRepository)
    }

    @Test
    fun `delete expense for user returns repository result`() {
        val userId = 123L
        val expenseId = UUID.randomUUID()
        every { expenseRepository.deleteByIdForUser(expenseId, userId) } returns true

        val result = service.deleteExpenseForUser(userId = userId, expenseId = expenseId)

        assertEquals(true, result)
        verify(exactly = 1) { expenseRepository.deleteByIdForUser(expenseId, userId) }
        confirmVerified(expenseTextParser, expenseDateParser, expenseRepository)
    }

    @Test
    fun `delete expense for user wraps DataAccessException into SystemException`() {
        val userId = 123L
        val expenseId = UUID.randomUUID()
        val cause = DataAccessException("DB connection failed")
        every { expenseRepository.deleteByIdForUser(expenseId, userId) } throws cause

        val exception =
            assertThrows<SystemException> {
                service.deleteExpenseForUser(userId = userId, expenseId = expenseId)
            }

        assertEquals(SystemErrorCode.DATABASE_ERROR, exception.errorCode)
        assertEquals(cause, exception.cause)
        verify(exactly = 1) { expenseRepository.deleteByIdForUser(expenseId, userId) }
        confirmVerified(expenseTextParser, expenseDateParser, expenseRepository)
    }

    @Test
    fun `save expense requires category in draft`() {
        assertThrows<IllegalArgumentException> {
            service.saveExpense(
                userId = 123L,
                expenseDraft =
                    ExpenseDraft(
                        amount = EXPENSE_AMOUNT,
                        description = "такси",
                        expenseDate = LocalDate.parse("2026-05-24"),
                    ),
            )
        }

        confirmVerified(expenseTextParser, expenseDateParser, expenseRepository)
    }

    @Test
    fun `save expense requires date in draft`() {
        assertThrows<IllegalArgumentException> {
            service.saveExpense(
                userId = 123L,
                expenseDraft =
                    ExpenseDraft(
                        amount = EXPENSE_AMOUNT,
                        description = "такси",
                        categoryId = UUID.randomUUID(),
                    ),
            )
        }

        confirmVerified(expenseTextParser, expenseDateParser, expenseRepository)
    }

    private fun completeDraft(categoryId: UUID): ExpenseDraft =
        ExpenseDraft(
            amount = EXPENSE_AMOUNT,
            description = "такси",
            categoryId = categoryId,
            expenseDate = LocalDate.parse("2026-05-24"),
        )

    private companion object {
        val EXPENSE_AMOUNT: Money = Money.of(BigDecimal("500.00"))
    }
}
