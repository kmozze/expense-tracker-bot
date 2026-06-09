package me.kmozze.expensetracker.unit.model.domain.expense

import me.kmozze.expensetracker.model.domain.expense.Money
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.math.BigDecimal

class MoneyTest {
    @Test
    fun `create money with two fraction digits`() {
        val money = Money.of(BigDecimal("500"))

        assertThat(money.value).isEqualTo(BigDecimal("500.00"))
    }

    @Test
    fun `round money half up`() {
        val money = Money.of(BigDecimal("10.555"))

        assertThat(money.value).isEqualTo(BigDecimal("10.56"))
    }

    @Test
    fun `format money as plain decimal`() {
        val money = Money.of(BigDecimal("10.5"))

        assertThat(money.format()).isEqualTo("10.50")
    }
}
