package me.kmozze.expensetracker.exception

sealed class DatabaseException(
    message: String,
) : RuntimeException(message)

class EntityNotFoundException(
    message: String,
) : DatabaseException(message)

class DatabaseOperationException(
    message: String,
) : DatabaseException(message)
