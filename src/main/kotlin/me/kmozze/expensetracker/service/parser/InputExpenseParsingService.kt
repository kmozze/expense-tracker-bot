package me.kmozze.expensetracker.service.parser

import me.kmozze.expensetracker.exception.BusinessErrorCode
import me.kmozze.expensetracker.exception.exception
import me.kmozze.expensetracker.model.domain.Money
import me.kmozze.expensetracker.model.domain.ParsedExpense
import org.springframework.stereotype.Service
import java.math.BigDecimal

@Service
class InputExpenseParsingService {
    fun parse(text: String): ParsedExpense {
        val words = text.split(Regex("""\s+""")).filter { it.isNotBlank() }

        if (words.isEmpty()) throw BusinessErrorCode.EXPENSE_INVALID_FORMAT.exception()

        val amountFirst = parseAmountOrNull(words.first())

        if (words.size == 1) {
            return if (amountFirst != null) {
                createExpense(description = null, amountFirst)
            } else {
                throw BusinessErrorCode.EXPENSE_INVALID_FORMAT.exception()
            }
        }

        val amountLast = parseAmountOrNull(words.last())

        return when {
            amountFirst != null -> {
                val description = words.drop(1).joinToString(" ")
                createExpense(description, amountFirst)
            }

            amountLast != null -> {
                val description = words.dropLast(1).joinToString(" ")
                createExpense(description, amountLast)
            }

            else -> throw BusinessErrorCode.EXPENSE_INVALID_FORMAT.exception()
        }
    }

    private fun parseAmountOrNull(word: String): BigDecimal? =
        try {
            BigDecimal(word.replace(',', '.'))
        } catch (e: NumberFormatException) {
            null
        }

    private fun createExpense(
        description: String?,
        amount: BigDecimal,
    ): ParsedExpense {
        val money = Money.of(amount)

        if (money.value <= BigDecimal.ZERO) {
            throw BusinessErrorCode.INVALID_AMOUNT.exception()
        }
        return ParsedExpense(
            amount = money,
            description = description?.trim()?.takeIf { it.isNotEmpty() },
        )
    }
}
