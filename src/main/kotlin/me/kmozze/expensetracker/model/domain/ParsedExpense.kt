package me.kmozze.expensetracker.model.domain

import java.math.BigDecimal

data class ParsedExpense(
    val amount: BigDecimal,
    val description: String,
)
