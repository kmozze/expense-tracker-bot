package me.kmozze.expensetracker.adapter.ui

import me.kmozze.expensetracker.exception.BusinessErrorCode
import me.kmozze.expensetracker.exception.ErrorCode
import me.kmozze.expensetracker.exception.SystemErrorCode
import me.kmozze.expensetracker.model.domain.Message
import org.springframework.stereotype.Component

@Component
class MessageFormatter {
    fun format(message: Message): String =
        when (message) {
            is Message.WelcomeFirstTime ->
                "👋 Добро пожаловать! Я создал для тебя базовые категории расходов."

            is Message.WelcomeBack ->
                "С возвращением! 💸 Я готов записывать твои новые траты."

            is Message.UnknownCommand ->
                "Я не понял команду 😕\nЧто бы вернуться в главное меню напиши /start"

            is Message.Error -> "❌ ${formatError(message.errorCode)}"
        }

    private fun formatError(errorCode: ErrorCode): String {
        return when (errorCode) {
            BusinessErrorCode.EXPENSE_INVALID_FORMAT -> "Неверный формат. Используйте: 'Еда 500' или '500 Еда'"
            BusinessErrorCode.INVALID_AMOUNT -> "Сумма должна быть больше нуля"
            SystemErrorCode.DATABASE_ERROR -> "Ошибка базы данных. Попробуйте позже."
            SystemErrorCode.INTERNAL_ERROR -> "Непредвиденная системная ошибка."
            else -> "Произошла неизвестная ошибка."
        }
    }
}
