package me.kmozze.expensetracker.adapter.ui

import me.kmozze.expensetracker.exception.BusinessErrorCode
import me.kmozze.expensetracker.exception.ErrorCode
import me.kmozze.expensetracker.exception.SystemErrorCode
import me.kmozze.expensetracker.model.domain.BotMessage
import org.springframework.stereotype.Component

@Component
class MessageFormatter {
    fun format(message: BotMessage): String =
        when (message) {
            is BotMessage.WelcomeFirstTime ->
                "👋 Добро пожаловать! Я создал для тебя базовые категории расходов."

            is BotMessage.WelcomeBack ->
                "С возвращением! 💸 Я готов записывать твои новые траты."

            is BotMessage.UnknownCommand ->
                "Я не понял команду 😕\nЧто бы вернуться в главное меню напиши /start"

            is BotMessage.AddExpenseInstructions ->
                "Введите сумму и описание одним сообщением.\nНапример: `500 такси` или  `такси 500`."

            is BotMessage.SelectCategory ->
                "💰 *${message.amount.format()} ₽*\n📝 ${message.description}\n\nКуда запишем?"

            is BotMessage.ExpenseSaved ->
                "✅ Сохранено!\n" +
                    "💰 Сумма: ${message.amount.format()} ₽\n" +
                    "📂 Категория: ${message.categoryName}\n" +
                    "📝 Описание: ${message.description}"

            is BotMessage.ExpenseCanceled ->
                "Добавление расхода отменено."

            is BotMessage.FeatureInProgress ->
                "Этот раздел пока в работе."

            is BotMessage.Error -> "❌ ${formatError(message.errorCode)}"
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
