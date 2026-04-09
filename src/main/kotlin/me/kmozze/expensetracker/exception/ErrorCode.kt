package me.kmozze.expensetracker.exception

interface ErrorCode {
    val code: String
    val message: String
}

enum class BusinessErrorCode(
    override val message: String,
) : ErrorCode {
    EXPENSE_INVALID_FORMAT("Неверный формат. Используйте: 'Еда 500' или '500 Еда'"),
    INVALID_AMOUNT("Сумма должна быть больше нуля"),
    ;

    override val code: String get() = name
}

enum class SystemErrorCode(
    override val message: String,
) : ErrorCode {
    DATABASE_ERROR("Ошибка базы данных"),
    INTERNAL_ERROR("Непредвиденная системная ошибка"),
    ;

    override val code: String get() = name
}

fun BusinessErrorCode.exception(customMessage: String? = null): BusinessException = BusinessException(this, message)

fun SystemErrorCode.exception(
    customMessage: String? = null,
    cause: Throwable? = null,
): SystemException = SystemException(this, customMessage, cause)
