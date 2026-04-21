package me.kmozze.expensetracker.exception

sealed class AppException(
    val errorCode: ErrorCode,
    override val message: String?,
    cause: Throwable? = null,
) : RuntimeException(message, cause)

class BusinessException(
    errorCode: BusinessErrorCode,
    message: String? = null,
) : AppException(errorCode, message)

class SystemException(
    errorCode: SystemErrorCode,
    message: String? = null,
    cause: Throwable? = null,
) : AppException(errorCode, message, cause)
