package me.kmozze.expensetracker.adapter.ui

import me.kmozze.expensetracker.exception.BusinessErrorCode
import me.kmozze.expensetracker.exception.ErrorCode
import me.kmozze.expensetracker.exception.SystemErrorCode
import me.kmozze.expensetracker.model.domain.BotText
import org.springframework.stereotype.Component
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Component
class MessageFormatter {
    fun format(message: BotText): String =
        when (message) {
            is BotText.WelcomeFirstTime ->
                "👋 Добро пожаловать! Я создал для тебя базовые категории расходов."

            is BotText.WelcomeBack ->
                "С возвращением! 💸 Я готов записывать твои новые траты."

            is BotText.MainMenu ->
                "Главное меню"

            is BotText.MainMenuInfo ->
                "🧭 Главное меню\n\n" +
                    "/menu — открыть это меню в любой момент.\n" +
                    "/start — запустить бота. Если категорий нет, будут созданы базовые."

            is BotText.MainMenuActions ->
                "Что хотите сделать?"

            is BotText.Done ->
                "Готово"

            is BotText.FinishCurrentDialog ->
                "Сначала завершите или отмените текущий диалог."

            is BotText.UnknownCommand ->
                "Я не понял команду 😕\nЧтобы вернуться в главное меню, напишите /menu"

            is BotText.AddExpenseInstructions ->
                "Введите сумму одним сообщением. Можно добавить описание.\nНапример: `500`, `500 такси` или `такси 500`."

            is BotText.ExpenseView ->
                formatExpenseView(message)

            is BotText.SelectExpenseDate ->
                "Когда была трата?"

            is BotText.SelectCategory ->
                "Куда запишем?"

            is BotText.EnterExpenseDateManually ->
                "Введите дату траты в формате ДД.ММ.ГГГГ."

            is BotText.ExpenseSaved ->
                "✅ Сохранено!"

            is BotText.EditExpenseFieldSelection ->
                "Что изменить?"

            is BotText.EnterExpenseAmount ->
                "Введите новую сумму"

            is BotText.EnterExpenseDescription ->
                "Введите новое описание"

            is BotText.ExpenseDeleted ->
                "Расход удален"

            is BotText.ExpenseUnavailable ->
                "Расход уже удален или недоступен."

            is BotText.ExpenseCanceled ->
                "Добавление расхода отменено."

            is BotText.SelectionExpired ->
                "Этот выбор уже неактуален. Используйте главное меню."

            is BotText.NoCategories ->
                "У вас нет ни одной категории. Создайте хотя бы одну, чтобы записывать расходы."

            is BotText.FeatureInProgress ->
                "Этот раздел пока в работе."

            is BotText.Error -> "❌ ${formatError(message.errorCode)}"
        }

    private fun formatError(errorCode: ErrorCode): String =
        when (errorCode) {
            BusinessErrorCode.EXPENSE_INVALID_FORMAT -> "Неверный формат. Используйте: '500', 'Еда 500' или '500 Еда'"
            BusinessErrorCode.INVALID_AMOUNT -> "Сумма должна быть числом больше нуля"
            BusinessErrorCode.CATEGORY_NOT_FOUND -> "Категория не найдена"
            BusinessErrorCode.INVALID_CATEGORY_SELECTION -> "Не получилось выбрать категорию"
            BusinessErrorCode.INVALID_EXPENSE_DATE_SELECTION -> "Не получилось выбрать дату"
            BusinessErrorCode.EXPENSE_DATE_INVALID_FORMAT -> "Введите дату в формате ДД.ММ.ГГГГ"
            SystemErrorCode.DATABASE_ERROR -> "Ошибка базы данных. Попробуйте позже."
            SystemErrorCode.INTERNAL_ERROR -> "Непредвиденная системная ошибка."
            else -> "Произошла неизвестная ошибка."
        }

    private fun MutableList<String>.addDescription(description: String?) {
        if (!description.isNullOrBlank()) {
            add("📝 $description")
        }
    }

    private fun formatExpenseView(message: BotText.ExpenseView): String {
        val lines =
            mutableListOf<String>().apply {
                message.amount?.let { add("💰 Сумма: ${it.format()} ₽") }
                message.categoryName?.let { add("📂 Категория: $it") }
                message.expenseDate?.let { add("📅 Дата: ${it.formatForUser()}") }
                addDescription(message.description)
            }

        require(lines.isNotEmpty()) {
            "Expense view must contain at least one visible field"
        }

        return lines.joinToString("\n")
    }

    private fun LocalDate.formatForUser(): String = format(USER_DATE_FORMATTER)

    private companion object {
        val USER_DATE_FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy")
    }
}
