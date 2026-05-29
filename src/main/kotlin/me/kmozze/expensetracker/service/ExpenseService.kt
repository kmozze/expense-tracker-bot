package me.kmozze.expensetracker.service

import me.kmozze.expensetracker.exception.BusinessErrorCode
import me.kmozze.expensetracker.exception.SystemErrorCode
import me.kmozze.expensetracker.exception.exception
import me.kmozze.expensetracker.model.domain.ExpenseDraft
import me.kmozze.expensetracker.model.domain.Money
import me.kmozze.expensetracker.model.entity.Expense
import me.kmozze.expensetracker.repository.IExpenseRepository
import me.kmozze.expensetracker.service.parser.InputExpenseDateParsingService
import me.kmozze.expensetracker.service.parser.InputExpenseParsingService
import org.jooq.exception.DataAccessException
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.time.LocalDate
import java.util.UUID

@Service
class ExpenseService(
    private val expenseTextParser: InputExpenseParsingService,
    private val expenseDateParser: InputExpenseDateParsingService,
    private val expenseRepository: IExpenseRepository,
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    fun parseExpense(text: String): ExpenseDraft {
        val expenseDraft = expenseTextParser.parse(text)
        logger.info("Expense draft parsed successfully: {}", expenseDraft)

        return expenseDraft
    }

    fun parseExpenseDate(text: String): LocalDate = expenseDateParser.parse(text)

    fun parseExpenseAmount(text: String): Money {
        val amount =
            try {
                BigDecimal(text.trim().replace(',', '.').trim())
            } catch (e: NumberFormatException) {
                throw BusinessErrorCode.INVALID_EXPENSE_AMOUNT.exception()
            }

        val money = Money.of(amount)
        if (money.value <= BigDecimal.ZERO) {
            throw BusinessErrorCode.INVALID_AMOUNT.exception()
        }
        return money
    }

    fun findExpenseForUser(
        userId: Long,
        expenseId: UUID,
    ): Expense? =
        try {
            expenseRepository.findByIdForUser(
                id = expenseId,
                userId = userId,
            )
        } catch (e: DataAccessException) {
            logger.error("Failed to load expense $expenseId for user $userId", e)

            throw SystemErrorCode.DATABASE_ERROR.exception(
                customMessage = "Ошибка при получении расхода $expenseId",
                cause = e,
            )
        }

    @Transactional
    fun updateExpenseAmountForUser(
        userId: Long,
        expenseId: UUID,
        amount: Money,
    ): Expense? = updateExpense(userId, expenseId) { it.copy(amount = amount) }

    @Transactional
    fun updateExpenseCategoryForUser(
        userId: Long,
        expenseId: UUID,
        categoryId: UUID,
    ): Expense? = updateExpense(userId, expenseId) { it.copy(categoryId = categoryId) }

    @Transactional
    fun updateExpenseDateForUser(
        userId: Long,
        expenseId: UUID,
        expenseDate: LocalDate,
    ): Expense? = updateExpense(userId, expenseId) { it.copy(expenseDate = expenseDate) }

    @Transactional
    fun updateExpenseDescriptionForUser(
        userId: Long,
        expenseId: UUID,
        description: String?,
    ): Expense? = updateExpense(userId, expenseId) { it.copy(description = description?.trim()?.ifEmpty { null }) }

    @Transactional
    fun saveExpense(
        userId: Long,
        expenseDraft: ExpenseDraft,
    ): Expense =
        try {
            val categoryId = expenseDraft.requireCategoryId()
            val expenseDate = expenseDraft.requireExpenseDate()
            val expense =
                Expense(
                    categoryId = categoryId,
                    amount = expenseDraft.amount,
                    userId = userId,
                    expenseDate = expenseDate,
                    description = expenseDraft.description,
                )

            expenseRepository.create(expense)
        } catch (e: DataAccessException) {
            logger.error("Failed to save expense for user $userId", e)

            throw SystemErrorCode.DATABASE_ERROR.exception(
                customMessage = "Ошибка при сохранении расхода пользователя $userId",
                cause = e,
            )
        }

    @Transactional
    fun deleteExpenseForUser(
        userId: Long,
        expenseId: UUID,
    ): Boolean =
        try {
            expenseRepository.deleteByIdForUser(
                id = expenseId,
                userId = userId,
            )
        } catch (e: DataAccessException) {
            logger.error("Failed to delete expense $expenseId for user $userId", e)

            throw SystemErrorCode.DATABASE_ERROR.exception(
                customMessage = "Ошибка при удалении расхода $expenseId",
                cause = e,
            )
        }

    private fun updateExpense(
        userId: Long,
        expenseId: UUID,
        transform: (Expense) -> Expense,
    ): Expense? =
        try {
            val existingExpense = expenseRepository.findByIdForUser(expenseId, userId) ?: return null
            expenseRepository.updateForUser(transform(existingExpense), userId)
        } catch (e: DataAccessException) {
            logger.error("Failed to update expense $expenseId for user $userId", e)

            throw SystemErrorCode.DATABASE_ERROR.exception(
                customMessage = "Ошибка при обновлении расхода $expenseId",
                cause = e,
            )
        }
}
