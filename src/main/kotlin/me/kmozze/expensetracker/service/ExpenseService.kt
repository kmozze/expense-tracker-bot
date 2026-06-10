package me.kmozze.expensetracker.service

import me.kmozze.expensetracker.exception.BusinessErrorCode
import me.kmozze.expensetracker.exception.SystemErrorCode
import me.kmozze.expensetracker.exception.exception
import me.kmozze.expensetracker.model.domain.expense.ExpenseDraft
import me.kmozze.expensetracker.model.domain.expense.ExpenseListFilter
import me.kmozze.expensetracker.model.domain.expense.ExpenseListPage
import me.kmozze.expensetracker.model.domain.expense.Money
import me.kmozze.expensetracker.model.entity.Expense
import me.kmozze.expensetracker.repository.ICategoryRepository
import me.kmozze.expensetracker.repository.IExpenseRepository
import me.kmozze.expensetracker.service.parser.InputExpenseDateParsingService
import me.kmozze.expensetracker.service.parser.InputExpenseParsingService
import org.jooq.exception.DataAccessException
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.time.Clock
import java.time.LocalDate
import java.util.UUID

@Service
class ExpenseService(
    private val expenseTextParser: InputExpenseParsingService,
    private val expenseDateParser: InputExpenseDateParsingService,
    private val expenseRepository: IExpenseRepository,
    private val categoryRepository: ICategoryRepository,
    private val clock: Clock,
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
                throw BusinessErrorCode.INVALID_AMOUNT.exception()
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

    fun getExpenseListPageForUser(
        userId: Long,
        filter: ExpenseListFilter,
        page: Int,
        pageSize: Int,
    ): ExpenseListPage {
        require(page >= 0) {
            "Expense list page must not be negative"
        }
        require(pageSize > 0) {
            "Expense list page size must be positive"
        }

        return try {
            val dateRange = filter.period.dateRange(LocalDate.now(clock))
            val totalCount =
                expenseRepository.countListItemsForUser(
                    userId = userId,
                    from = dateRange.from,
                    to = dateRange.to,
                    categoryId = filter.categoryId,
                )
            val effectivePage =
                if (totalCount == 0) {
                    0
                } else {
                    minOf(page, (totalCount - 1) / pageSize)
                }
            val items =
                if (totalCount == 0) {
                    emptyList()
                } else {
                    expenseRepository.findListItemsForUser(
                        userId = userId,
                        from = dateRange.from,
                        to = dateRange.to,
                        categoryId = filter.categoryId,
                        limit = pageSize,
                        offset = effectivePage * pageSize,
                    )
                }

            ExpenseListPage(
                filter = filter,
                items = items,
                page = effectivePage,
                pageSize = pageSize,
                totalCount = totalCount,
            )
        } catch (e: DataAccessException) {
            logger.error("Failed to load expense list for user $userId with filter $filter", e)

            throw SystemErrorCode.DATABASE_ERROR.exception(
                customMessage = "Ошибка при получении списка расходов пользователя $userId",
                cause = e,
            )
        }
    }

    @Transactional
    fun updateExpenseFromDraftForUser(
        userId: Long,
        expenseId: UUID,
        expenseDraft: ExpenseDraft,
    ): Expense? {
        try {
            val categoryId = expenseDraft.requireCategoryId()
            val expenseDate = expenseDraft.requireExpenseDate()
            val existingExpense = expenseRepository.findByIdForUser(expenseId, userId) ?: return null
            categoryRepository.findByIdForUser(categoryId, userId) ?: return null

            return expenseRepository.updateForUser(
                existingExpense.copy(
                    amount = expenseDraft.amount,
                    categoryId = categoryId,
                    expenseDate = expenseDate,
                    description = expenseDraft.description?.trim()?.ifEmpty { null },
                ),
                userId,
            )
        } catch (e: DataAccessException) {
            logger.error("Failed to update expense $expenseId from draft for user $userId", e)

            throw SystemErrorCode.DATABASE_ERROR.exception(
                customMessage = "Ошибка при обновлении расхода $expenseId",
                cause = e,
            )
        }
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
