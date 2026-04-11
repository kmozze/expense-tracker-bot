package me.kmozze.expensetracker.exception

sealed class AppException(
    val error: ErrorCode,
    override val message: String,
    cause: Throwable? = null,
) : RuntimeException(message, cause)

class BusinessException(
    error: BusinessErrorCode,
    message: String? = null,
) : AppException(error, message ?: error.message)

class SystemException(
    error: SystemErrorCode,
    message: String? = null,
    cause: Throwable? = null,
) : AppException(error, message ?: error.message, cause)
