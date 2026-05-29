package me.kmozze.expensetracker.exception

interface ErrorCode {
    val code: String
}

enum class BusinessErrorCode : ErrorCode {
    EXPENSE_INVALID_FORMAT,
    INVALID_AMOUNT,
    CATEGORY_NOT_FOUND,
    INVALID_CATEGORY_SELECTION,
    INVALID_EXPENSE_DATE_SELECTION,
    INVALID_EXPENSE_AMOUNT,
    EXPENSE_DATE_INVALID_FORMAT,
    ;

    override val code: String get() = name
}

enum class SystemErrorCode : ErrorCode {
    DATABASE_ERROR,
    INTERNAL_ERROR,
    ;

    override val code: String get() = name
}

fun BusinessErrorCode.exception(customMessage: String? = null): BusinessException = BusinessException(this, customMessage)

fun SystemErrorCode.exception(
    customMessage: String? = null,
    cause: Throwable? = null,
): SystemException = SystemException(this, customMessage, cause)
