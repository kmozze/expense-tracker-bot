package me.kmozze.expensetracker.unit.service

import io.mockk.confirmVerified
import io.mockk.every
import io.mockk.junit5.MockKExtension
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import me.kmozze.expensetracker.exception.SystemErrorCode
import me.kmozze.expensetracker.exception.SystemException
import me.kmozze.expensetracker.model.domain.ExpenseDraft
import me.kmozze.expensetracker.model.domain.Money
import me.kmozze.expensetracker.model.entity.Expense
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
    private lateinit var service: ExpenseService

    @BeforeEach
    fun setUp() {
        service =
            ExpenseService(
                expenseTextParser = expenseTextParser,
                expenseDateParser = expenseDateParser,
                expenseRepository = expenseRepository,
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
    fun `find expense for user delegates to user scoped repository query`() {
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

        val result = service.findExpenseForUser(userId, expenseId)

        assertEquals(expense, result)
        verify(exactly = 1) { expenseRepository.findByIdForUser(expenseId, userId) }
        confirmVerified(expenseTextParser, expenseDateParser, expenseRepository)
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
    fun `delete expense for user delegates to user scoped repository delete`() {
        val userId = 123L
        val expenseId = UUID.randomUUID()
        every { expenseRepository.deleteByIdForUser(expenseId, userId) } returns true

        val result = service.deleteExpenseForUser(userId, expenseId)

        assertEquals(true, result)
        verify(exactly = 1) { expenseRepository.deleteByIdForUser(expenseId, userId) }
        confirmVerified(expenseTextParser, expenseDateParser, expenseRepository)
    }

    @Test
    fun `delete expense wraps DataAccessException into SystemException`() {
        val userId = 123L
        val expenseId = UUID.randomUUID()
        val cause = DataAccessException("DB connection failed")
        every { expenseRepository.deleteByIdForUser(expenseId, userId) } throws cause

        val exception =
            assertThrows<SystemException> {
                service.deleteExpenseForUser(userId, expenseId)
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

    private companion object {
        val EXPENSE_AMOUNT: Money = Money.of(BigDecimal("500.00"))
    }
}
