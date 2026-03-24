package me.kmozze.expensetracker.model

import java.math.BigDecimal

data class Expense(
    val category: String,
    val amount: BigDecimal
)

