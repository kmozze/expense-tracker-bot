package me.kmozze.expensetracker.model.domain

import java.math.BigDecimal
import java.math.RoundingMode

private const val MONEY_SCALE = 2
private val MONEY_ROUNDING_MODE = RoundingMode.HALF_UP

@JvmInline
value class Money private constructor(
    val value: BigDecimal,
) {
    fun format(): String = value.toPlainString()

    companion object {
        fun of(value: BigDecimal): Money = Money(value.setScale(MONEY_SCALE, MONEY_ROUNDING_MODE))
    }
}
