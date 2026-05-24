package me.kmozze.expensetracker.adapter.ui

import me.kmozze.expensetracker.exception.BusinessErrorCode
import me.kmozze.expensetracker.exception.ErrorCode
import me.kmozze.expensetracker.exception.SystemErrorCode
import me.kmozze.expensetracker.model.domain.BotMessage
import org.springframework.stereotype.Component
import java.time.LocalDate
import java.time.format.DateTimeFormatter

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
                "Введите сумму одним сообщением. Можно добавить описание.\nНапример: `500`, `500 такси` или `такси 500`."

            is BotMessage.SelectCategory ->
                buildString {
                    append("💰 *${message.amount.format()} ₽*")
                    appendDescription(message.description)
                    append("\n\nКуда запишем?")
                }

            is BotMessage.ExpenseSaved ->
                buildString {
                    append("✅ Сохранено!\n")
                    append("💰 Сумма: ${message.amount.format()} ₽\n")
                    append("📂 Категория: ${message.categoryName}\n")
                    append("📅 Дата: ${message.expenseDate.formatForUser()}")
                    appendDescription(message.description)
                }

            is BotMessage.ExpenseCanceled ->
                "Добавление расхода отменено."

            is BotMessage.NoCategories ->
                "У вас нет ни одной категории. Создайте хотя бы одну, чтобы записывать расходы."

            is BotMessage.FeatureInProgress ->
                "Этот раздел пока в работе."

            is BotMessage.Error -> "❌ ${formatError(message.errorCode)}"
        }

    private fun formatError(errorCode: ErrorCode): String =
        when (errorCode) {
            BusinessErrorCode.EXPENSE_INVALID_FORMAT -> "Неверный формат. Используйте: '500', 'Еда 500' или '500 Еда'"
            BusinessErrorCode.INVALID_AMOUNT -> "Сумма должна быть больше нуля"
            BusinessErrorCode.CATEGORY_NOT_FOUND -> "Категория не найдена"
            BusinessErrorCode.INVALID_CATEGORY_SELECTION -> "Не получилось выбрать категорию"
            SystemErrorCode.DATABASE_ERROR -> "Ошибка базы данных. Попробуйте позже."
            SystemErrorCode.INTERNAL_ERROR -> "Непредвиденная системная ошибка."
            else -> "Произошла неизвестная ошибка."
        }

    private fun StringBuilder.appendDescription(description: String?) {
        if (!description.isNullOrBlank()) {
            append("\n📝 $description")
        }
    }

    private fun LocalDate.formatForUser(): String = format(USER_DATE_FORMATTER)

    private companion object {
        val USER_DATE_FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy")
    }
}
