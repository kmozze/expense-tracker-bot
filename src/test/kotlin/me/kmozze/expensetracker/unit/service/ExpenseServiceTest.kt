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
import me.kmozze.expensetracker.service.parser.InputExpenseParsingService
import org.assertj.core.api.Assertions.assertThat
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
    private val expenseRepository: IExpenseRepository = mockk()
    private lateinit var service: ExpenseService

    @BeforeEach
    fun setUp() {
        service =
            ExpenseService(
                expenseTextParser = expenseTextParser,
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
        confirmVerified(expenseTextParser, expenseRepository)
    }

    @Test
    fun `save expense creates expense from draft`() {
        val userId = 123L
        val categoryId = UUID.randomUUID()
        val expenseDraft = ExpenseDraft(EXPENSE_AMOUNT, "такси", categoryId = categoryId)
        val expenseSlot = slot<Expense>()
        val savedExpenseId = UUID.randomUUID()
        val savedAt = OffsetDateTime.parse("2026-05-22T10:00:00Z")
        val expenseDateBeforeSave = LocalDate.now()
        every { expenseRepository.create(capture(expenseSlot)) } answers {
            expenseSlot.captured.copy(id = savedExpenseId, createdAt = savedAt)
        }

        val result =
            service.saveExpense(
                userId = userId,
                expenseDraft = expenseDraft,
            )
        val expenseDateAfterSave = LocalDate.now()

        val createdExpense = expenseSlot.captured
        assertEquals(categoryId, createdExpense.categoryId)
        assertEquals(EXPENSE_AMOUNT, createdExpense.amount)
        assertEquals(userId, createdExpense.userId)
        assertThat(createdExpense.expenseDate).isBetween(expenseDateBeforeSave, expenseDateAfterSave)
        assertEquals("такси", createdExpense.description)
        assertNull(createdExpense.createdAt)

        assertEquals(savedExpenseId, result.id)
        assertEquals(categoryId, result.categoryId)
        assertEquals(EXPENSE_AMOUNT, result.amount)
        assertEquals(userId, result.userId)
        assertThat(result.expenseDate).isBetween(expenseDateBeforeSave, expenseDateAfterSave)
        assertEquals("такси", result.description)
        assertEquals(savedAt, result.createdAt)
        verify(exactly = 1) { expenseRepository.create(any()) }
        confirmVerified(expenseTextParser, expenseRepository)
    }

    @Test
    fun `save expense wraps DataAccessException into SystemException`() {
        val userId = 123L
        val categoryId = UUID.randomUUID()
        val expenseDraft = ExpenseDraft(EXPENSE_AMOUNT, "такси", categoryId = categoryId)
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
        confirmVerified(expenseTextParser, expenseRepository)
    }

    @Test
    fun `save expense requires category in draft`() {
        assertThrows<IllegalArgumentException> {
            service.saveExpense(
                userId = 123L,
                expenseDraft = ExpenseDraft(EXPENSE_AMOUNT, "такси"),
            )
        }

        confirmVerified(expenseTextParser, expenseRepository)
    }

    private companion object {
        val EXPENSE_AMOUNT: Money = Money.of(BigDecimal("500.00"))
    }
}
