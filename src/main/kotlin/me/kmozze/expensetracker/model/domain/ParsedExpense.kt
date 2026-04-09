package me.kmozze.expensetracker.model.domain

import java.math.BigDecimal

data class ParsedExpense(
    val category: String,
    val amount: BigDecimal,
    val description: String? = null,
)
