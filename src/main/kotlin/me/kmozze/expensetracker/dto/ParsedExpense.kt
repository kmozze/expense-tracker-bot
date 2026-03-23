package me.kmozze.expensetracker.dto

import java.math.BigDecimal

data class ParsedExpense(
    val category: String,
    val amount: BigDecimal
)
