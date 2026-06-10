package me.kmozze.expensetracker.model.domain.expense

import java.time.LocalDate
import java.util.UUID

enum class ExpenseListPeriod(
    val code: String,
) {
    Day("d"),
    Week("w"),
    Month("m"),
    AllTime("a"),
    ;

    fun dateRange(today: LocalDate): ExpenseListDateRange =
        when (this) {
            Day -> ExpenseListDateRange(from = today, to = today)
            Week -> ExpenseListDateRange(from = today.minusDays(6), to = today)
            Month -> ExpenseListDateRange(from = today.minusMonths(1), to = today)
            AllTime -> ExpenseListDateRange(from = null, to = null)
        }

    companion object {
        fun fromCode(code: String): ExpenseListPeriod? = entries.firstOrNull { it.code == code }
    }
}

data class ExpenseListDateRange(
    val from: LocalDate?,
    val to: LocalDate?,
)

data class ExpenseListFilter(
    val period: ExpenseListPeriod,
    val categoryId: UUID? = null,
)

data class ExpenseListItem(
    val expenseId: UUID,
    val expenseDate: LocalDate,
    val categoryName: String,
    val amount: Money,
)

data class ExpenseListPage(
    val filter: ExpenseListFilter,
    val items: List<ExpenseListItem>,
    val page: Int,
    val pageSize: Int,
    val totalCount: Int,
) {
    val hasPreviousPage: Boolean = page > 0
    val hasNextPage: Boolean = (page + 1) * pageSize < totalCount
}

data class ExpenseListCategoryOption(
    val categoryId: UUID,
    val name: String,
)
