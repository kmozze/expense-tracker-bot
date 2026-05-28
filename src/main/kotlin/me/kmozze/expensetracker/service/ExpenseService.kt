package me.kmozze.expensetracker.service

import me.kmozze.expensetracker.exception.SystemErrorCode
import me.kmozze.expensetracker.exception.exception
import me.kmozze.expensetracker.model.domain.ExpenseDraft
import me.kmozze.expensetracker.model.entity.Expense
import me.kmozze.expensetracker.repository.IExpenseRepository
import me.kmozze.expensetracker.service.parser.InputExpenseDateParsingService
import me.kmozze.expensetracker.service.parser.InputExpenseParsingService
import org.jooq.exception.DataAccessException
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
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
}
