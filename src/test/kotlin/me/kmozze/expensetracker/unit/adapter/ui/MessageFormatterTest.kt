package me.kmozze.expensetracker.unit.adapter.ui

import me.kmozze.expensetracker.adapter.ui.MessageFormatter
import me.kmozze.expensetracker.model.domain.BotMessage
import me.kmozze.expensetracker.model.domain.Money
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.LocalDate

class MessageFormatterTest {
    private val formatter = MessageFormatter()

    @Test
    fun `select category message omits description line when description is missing`() {
        val text =
            formatter.format(
                BotMessage.SelectCategory(
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
                BotMessage.ExpenseSaved(
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
    fun `select expense date message includes category and description`() {
        val text =
            formatter.format(
                BotMessage.SelectExpenseDate(
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
                BotMessage.EnterExpenseDateManually(
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
}
