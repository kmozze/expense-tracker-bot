package me.kmozze.expensetracker.model.domain

data class ParsedExpense(
    val amount: Money,
    val description: String?,
)
