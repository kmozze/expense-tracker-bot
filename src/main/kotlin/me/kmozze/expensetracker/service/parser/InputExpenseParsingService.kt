package me.kmozze.expensetracker.service.parser

import me.kmozze.expensetracker.exception.BusinessErrorCode
import me.kmozze.expensetracker.exception.exception
import me.kmozze.expensetracker.model.domain.ParsedExpense
import org.springframework.stereotype.Service
import java.math.BigDecimal

@Service
class InputExpenseParsingService {
    fun parse(text: String): ParsedExpense {
        val words = text.split(Regex("""\s+""")).filter { it.isNotBlank() }

        if (words.size < 2) throw BusinessErrorCode.EXPENSE_INVALID_FORMAT.exception()

        val amountFirst = parseAmountOrNull(words.first())
        val amountLast = parseAmountOrNull(words.last())

        return when {
            amountFirst != null -> {
                val category = words.drop(1).joinToString(" ")
                createExpense(category, amountFirst)
            }

            amountLast != null -> {
                val category = words.dropLast(1).joinToString(" ")
                createExpense(category, amountLast)
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
        category: String,
        amount: BigDecimal,
    ): ParsedExpense {
        if (amount <= BigDecimal.ZERO) {
            throw BusinessErrorCode.INVALID_AMOUNT.exception()
        }
        return ParsedExpense(category.trim(), amount)
    }
}
