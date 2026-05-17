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

            is Message.AddExpenseInstructions ->
                "Введите сумму и описание одним сообщением.\nНапример: `500 такси` или  `такси 500`."

            is Message.SelectCategory ->
                "💰 *${message.amount} ₽*\n📝 ${message.description}\n\nКуда запишем?"

            is Message.ExpenseSaved ->
                "✅ Сохранено!\n" +
                    "💰 Сумма: ${message.amount} ₽\n" +
                    "📂 Категория: ${message.categoryName}\n" +
                    "📝 Описание: ${message.description}"

            is Message.ExpenseCanceled ->
                "Добавление расхода отменено."

            is Message.FeatureInProgress ->
                "Этот раздел пока в работе."

            is Message.Error -> "❌ ${formatError(message.errorCode)}"
        }

    private fun formatError(errorCode: ErrorCode): String =
        when (errorCode) {
            BusinessErrorCode.EXPENSE_INVALID_FORMAT -> "Неверный формат. Используйте: 'Еда 500' или '500 Еда'"
            BusinessErrorCode.INVALID_AMOUNT -> "Сумма должна быть больше нуля"
            BusinessErrorCode.CATEGORY_NOT_FOUND -> "Категория не найдена"
            BusinessErrorCode.INVALID_CATEGORY_SELECTION -> "Не получилось выбрать категорию"
            SystemErrorCode.DATABASE_ERROR -> "Ошибка базы данных. Попробуйте позже."
            SystemErrorCode.INTERNAL_ERROR -> "Непредвиденная системная ошибка."
            else -> "Произошла неизвестная ошибка."
        }
}
