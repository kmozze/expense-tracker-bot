package me.kmozze.expensetracker.unit.adapter.ui

import me.kmozze.expensetracker.adapter.ui.MessageFormatter
import me.kmozze.expensetracker.exception.BusinessErrorCode
import me.kmozze.expensetracker.model.domain.bot.BotText
import me.kmozze.expensetracker.model.domain.expense.Money
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
    fun `select category prompt text`() {
        val text = formatter.format(BotText.SelectCategory)

        assertThat(text).isEqualTo("Куда запишем?")
    }

    @Test
    fun `expense card omits description line when description is missing`() {
        val text =
            formatter.format(
                BotText.ExpenseView(
                    amount = Money.of(BigDecimal("500.00")),
                    categoryName = "Еда",
                    expenseDate = LocalDate.parse("2026-05-24"),
                    description = null,
                ),
            )

        assertThat(text)
            .isEqualTo("💰 Сумма: 500.00 ₽\n📂 Категория: Еда\n📅 Дата: 24.05.2026")
    }

    @Test
    fun `expense card renders non-null fields in stable order`() {
        val text =
            formatter.format(
                BotText.ExpenseView(
                    amount = Money.of(BigDecimal("500.00")),
                    categoryName = "Еда",
                    expenseDate = LocalDate.parse("2026-05-24"),
                    description = "обед",
                ),
            )

        assertThat(text)
            .isEqualTo(
                "💰 Сумма: 500.00 ₽\n" +
                    "📂 Категория: Еда\n" +
                    "📅 Дата: 24.05.2026\n" +
                    "📝 обед",
            )
    }

    @Test
    fun `expense deletion result texts`() {
        assertThat(formatter.format(BotText.ExpenseDeleted)).isEqualTo("Расход удален")
        assertThat(formatter.format(BotText.ExpenseUnavailable)).isEqualTo("Расход уже удален или недоступен.")
    }

    @Test
    fun `select expense date prompt text`() {
        val text = formatter.format(BotText.SelectExpenseDate)

        assertThat(text).isEqualTo("Когда была трата?")
    }

    @Test
    fun `manual expense date prompt text`() {
        val text = formatter.format(BotText.EnterExpenseDateManually)

        assertThat(text).isEqualTo("Введите дату траты в формате ДД.ММ.ГГГГ.")
    }

    @Test
    fun `expense saved status text`() {
        val text = formatter.format(BotText.ExpenseSaved)

        assertThat(text).isEqualTo("✅ Сохранено!")
    }

    @Test
    fun `edit amount and description prompts`() {
        assertThat(formatter.format(BotText.EditExpenseFieldSelection)).isEqualTo("Что изменить?")
        assertThat(formatter.format(BotText.EnterExpenseAmount)).isEqualTo("Введите новую сумму")
        assertThat(formatter.format(BotText.EnterExpenseDescription)).isEqualTo("Введите новое описание")
    }

    @Test
    fun `amount error text covers invalid text and non-positive amount`() {
        assertThat(formatter.format(BotText.Error(BusinessErrorCode.INVALID_AMOUNT)))
            .isEqualTo("❌ Сумма должна быть числом больше нуля")
    }
}
