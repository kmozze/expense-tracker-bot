package me.kmozze.expensetracker.adapter.ui

import me.kmozze.expensetracker.exception.BusinessErrorCode
import me.kmozze.expensetracker.exception.ErrorCode
import me.kmozze.expensetracker.exception.SystemErrorCode
import me.kmozze.expensetracker.model.domain.BotText
import me.kmozze.expensetracker.model.domain.Money
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

            is BotText.SelectCategory ->
                buildString {
                    append("💰 *${message.amount.format()} ₽*")
                    appendDescription(message.description)
                    append("\n\nКуда запишем?")
                }

            is BotText.SelectExpenseDate ->
                buildString {
                    append("💰 Сумма: ${message.amount.format()} ₽\n")
                    append("📂 Категория: ${message.categoryName}")
                    appendDescription(message.description)
                    append("\n\nКогда была трата?")
                }

            is BotText.EnterExpenseDateManually ->
                buildString {
                    append("💰 Сумма: ${message.amount.format()} ₽\n")
                    append("📂 Категория: ${message.categoryName}")
                    appendDescription(message.description)
                    append("\n\nВведите дату траты в формате ДД.ММ.ГГГГ.")
                }

            is BotText.ExpenseSaved ->
                formatExpenseCard(
                    amount = message.amount,
                    categoryName = message.categoryName,
                    expenseDate = message.expenseDate,
                    description = message.description,
                )

            is BotText.ExpenseDeletionConfirmation ->
                formatExpenseCard(
                    amount = message.amount,
                    categoryName = message.categoryName,
                    expenseDate = message.expenseDate,
                    description = message.description,
                ) + "\n\nТочно хотите удалить расход?"

            is BotText.ExpenseEditable ->
                formatExpenseCard(
                    amount = message.amount,
                    categoryName = message.categoryName,
                    expenseDate = message.expenseDate,
                    description = message.description,
                ) + "\n\nЭта карточка открыта для редактирования ниже."

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

    private fun StringBuilder.appendDescription(description: String?) {
        if (!description.isNullOrBlank()) {
            append("\n📝 $description")
        }
    }

    private fun formatExpenseCard(
        amount: Money,
        categoryName: String,
        expenseDate: LocalDate,
        description: String?,
    ): String =
        buildString {
            append("✅ Сохранено!\n")
            append("💰 Сумма: ${amount.format()} ₽\n")
            append("📂 Категория: $categoryName\n")
            append("📅 Дата: ${expenseDate.formatForUser()}")
            appendDescription(description)
        }

    private fun LocalDate.formatForUser(): String = format(USER_DATE_FORMATTER)

    private companion object {
        val USER_DATE_FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy")
    }
}
