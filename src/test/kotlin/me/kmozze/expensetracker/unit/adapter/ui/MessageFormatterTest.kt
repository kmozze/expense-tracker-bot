package me.kmozze.expensetracker.unit.adapter.ui

import me.kmozze.expensetracker.adapter.ui.MessageFormatter
import me.kmozze.expensetracker.exception.BusinessErrorCode
import me.kmozze.expensetracker.model.domain.BotText
import me.kmozze.expensetracker.model.domain.Money
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.LocalDate

class MessageFormatterTest {
    private val formatter = MessageFormatter()

    @Test
    fun `main menu text`() {
        val text = formatter.format(BotText.MainMenu)

        assertThat(text).isEqualTo("Главное меню")
    }

    @Test
    fun `main menu info text`() {
        val text = formatter.format(BotText.MainMenuInfo)

        assertThat(text)
            .isEqualTo(
                "🧭 Главное меню\n\n" +
                    "/menu — открыть это меню в любой момент.\n" +
                    "/start — запустить бота. Если категорий нет, будут созданы базовые.",
            )
    }

    @Test
    fun `main menu actions text`() {
        val text = formatter.format(BotText.MainMenuActions)

        assertThat(text).isEqualTo("Что хотите сделать?")
    }

    @Test
    fun `done text`() {
        val text = formatter.format(BotText.Done)

        assertThat(text).isEqualTo("Готово")
    }

    @Test
    fun `finish current dialog callback text`() {
        val text = formatter.format(BotText.FinishCurrentDialog)

        assertThat(text).isEqualTo("Сначала завершите или отмените текущий диалог.")
    }

    @Test
    fun `select category message omits description line when description is missing`() {
        val text =
            formatter.format(
                BotText.SelectCategory(
                    amount = Money.of(BigDecimal("500.00")),
                    description = null,
                ),
            )

        assertThat(text).isEqualTo("💰 *500.00 ₽*\n\nКуда запишем?")
    }

    @Test
    fun `saved expense message omits description line when description is missing`() {
        val text =
            formatter.format(
                BotText.ExpenseSaved(
                    amount = Money.of(BigDecimal("500.00")),
                    categoryName = "Еда",
                    expenseDate = LocalDate.parse("2026-05-24"),
                    description = null,
                ),
            )

        assertThat(text)
            .isEqualTo("✅ Сохранено!\n💰 Сумма: 500.00 ₽\n📂 Категория: Еда\n📅 Дата: 24.05.2026")
    }

    @Test
    fun `expense deletion confirmation includes card and question`() {
        val text =
            formatter.format(
                BotText.ExpenseDeletionConfirmation(
                    amount = Money.of(BigDecimal("500.00")),
                    categoryName = "Еда",
                    expenseDate = LocalDate.parse("2026-05-24"),
                    description = "обед",
                ),
            )

        assertThat(text)
            .isEqualTo(
                "✅ Сохранено!\n" +
                    "💰 Сумма: 500.00 ₽\n" +
                    "📂 Категория: Еда\n" +
                    "📅 Дата: 24.05.2026\n" +
                    "📝 обед\n\n" +
                    "Точно хотите удалить расход?",
            )
    }

    @Test
    fun `expense deletion result texts`() {
        assertThat(formatter.format(BotText.ExpenseDeleted)).isEqualTo("Расход удален")
        assertThat(formatter.format(BotText.ExpenseUnavailable)).isEqualTo("Расход уже удален или недоступен.")
    }

    @Test
    fun `select expense date message includes category and description`() {
        val text =
            formatter.format(
                BotText.SelectExpenseDate(
                    amount = Money.of(BigDecimal("500.00")),
                    categoryName = "Транспорт",
                    description = "такси",
                ),
            )

        assertThat(text)
            .isEqualTo("💰 Сумма: 500.00 ₽\n📂 Категория: Транспорт\n📝 такси\n\nКогда была трата?")
    }

    @Test
    fun `manual expense date message includes input format`() {
        val text =
            formatter.format(
                BotText.EnterExpenseDateManually(
                    amount = Money.of(BigDecimal("500.00")),
                    categoryName = "Транспорт",
                    description = null,
                ),
            )

        assertThat(text)
            .isEqualTo(
                "💰 Сумма: 500.00 ₽\n" +
                    "📂 Категория: Транспорт\n\n" +
                    "Введите дату траты в формате ДД.ММ.ГГГГ.",
            )
    }

    @Test
    fun `editable expense message adds edit hint`() {
        val text =
            formatter.format(
                BotText.ExpenseEditable(
                    amount = Money.of(BigDecimal("500.00")),
                    categoryName = "Транспорт",
                    expenseDate = LocalDate.parse("2026-05-24"),
                    description = "такси",
                ),
            )

        assertThat(text)
            .isEqualTo(
                "✅ Сохранено!\n" +
                    "💰 Сумма: 500.00 ₽\n" +
                    "📂 Категория: Транспорт\n" +
                    "📅 Дата: 24.05.2026\n" +
                    "📝 такси\n\n" +
                    "Эта карточка открыта для редактирования ниже.",
            )
    }

    @Test
    fun `edit amount and description prompts`() {
        assertThat(formatter.format(BotText.EditExpenseFieldSelection)).isEqualTo("Что изменить?")
        assertThat(formatter.format(BotText.EnterExpenseAmount)).isEqualTo("Введите новую сумму")
        assertThat(formatter.format(BotText.EnterExpenseDescription)).isEqualTo("Введите новое описание")
    }

    @Test
    fun `amount error texts distinguish invalid text from non-positive amount`() {
        assertThat(formatter.format(BotText.Error(BusinessErrorCode.INVALID_EXPENSE_AMOUNT)))
            .isEqualTo("❌ Введите только число")
        assertThat(formatter.format(BotText.Error(BusinessErrorCode.INVALID_AMOUNT)))
            .isEqualTo("❌ Сумма должна быть больше нуля")
    }
}
