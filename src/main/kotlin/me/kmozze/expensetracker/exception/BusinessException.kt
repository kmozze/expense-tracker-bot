package me.kmozze.expensetracker.exception

sealed class BusinessException(
    message: String,
) : RuntimeException(message)

class ExpenseValidationException(
    message: String,
) : BusinessException(message)

class ExpenseInvalidFormatException(
    message: String,
) : BusinessException(message)
