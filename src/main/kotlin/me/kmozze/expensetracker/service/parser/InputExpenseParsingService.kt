package me.kmozze.expensetracker.service.parser

import me.kmozze.expensetracker.exception.ExpenseInvalidFormatException
import me.kmozze.expensetracker.exception.ExpenseValidationException
import me.kmozze.expensetracker.model.Expense
import me.kmozze.expensetracker.model.ParsedExpense
import org.springframework.stereotype.Service
import java.math.BigDecimal

@Service
class InputExpenseParsingService {

    fun parse(text: String): ParsedExpense {
        val words = text.split(Regex("""\s+""")).filter { it.isNotBlank() }

        if (words.size < 2) throw ExpenseInvalidFormatException(text)

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

            else -> throw ExpenseInvalidFormatException(text)
        }
    }

    private fun parseAmountOrNull(word: String): BigDecimal? {
        return try {
            BigDecimal(word.replace(',', '.'))
        } catch (e: NumberFormatException) {
            null
        }
    }

    private fun createExpense(category: String, amount: BigDecimal): ParsedExpense {
        if (amount <= BigDecimal.ZERO) {
            throw ExpenseValidationException("Amount must be positive: $amount")
        }
        return ParsedExpense(category.trim(), amount)
    }
}
