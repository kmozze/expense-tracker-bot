package me.kmozze.expensetracker.service.parser

import me.kmozze.expensetracker.exception.ExpenseInvalidFormatException
import me.kmozze.expensetracker.exception.ExpenseValidationException
import me.kmozze.expensetracker.model.Expense
import org.springframework.stereotype.Service
import java.math.BigDecimal

@Service
class InputExpenseParsingService {

    fun parse(text: String): Expense {
        val words = text.split(Regex("""\s+""")).filter { it.isNotBlank() }

        if (words.size < 2) throw ExpenseInvalidFormatException(text)

        return when {
            parseAmountOrNull(words.first()) != null -> {
                val amount = parseAmountOrNull(words.first())!!
                val category = words.drop(1).joinToString(" ")
                createExpense(category, amount)
            }

            parseAmountOrNull(words.last()) != null -> {
                val amount = parseAmountOrNull(words.last())!!
                val category = words.dropLast(1).joinToString(" ")
                createExpense(category, amount)
            }

            else -> throw ExpenseInvalidFormatException(text)
        }
    }

    private fun parseAmountOrNull(word: String): BigDecimal? {
        return try {
            BigDecimal(word.replace(',', '.'))
        } catch (e: Exception) {
            null
        }
    }

    private fun createExpense(category: String, amount: BigDecimal): Expense {
        if (amount <= BigDecimal.ZERO) {
            throw ExpenseValidationException("Amount must be positive: $amount")
        }
        return Expense(category.trim(), amount)
    }
}