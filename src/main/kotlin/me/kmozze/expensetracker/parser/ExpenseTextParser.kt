package me.kmozze.expensetracker.parser;

import me.kmozze.expensetracker.dto.ParsedExpense
import org.springframework.stereotype.Component
import java.math.BigDecimal

@Component
public class ExpenseTextParser {
    fun parse(text: String): ParsedExpense? {
        if (text.isBlank()) return null

        val normalText = text.trim().replace(Regex("""\s+"""), " ")

        val amountPattern = """(\d+(?:[.,]\d+)?)"""
        val categoryPattern = """(.+)"""
        val amountFirstRegex = Regex("""^$amountPattern\s+$categoryPattern$""")
        val categoryFirstRegex = Regex("""^$categoryPattern\s+$amountPattern$""")

        amountFirstRegex.find(normalText)?.let {
            return ParsedExpense(
                amount = BigDecimal(it.groupValues[1]),
                category = it.groupValues[2]
            )
        }

        categoryFirstRegex.find(normalText)?.let {
            return ParsedExpense(
                amount = toBigDecimal(it.groupValues[2]),
                category = it.groupValues[1]
            )
        }

        return null
    }

    private fun toBigDecimal(value: String): BigDecimal {
        return BigDecimal(value.replace(',', '.'))
    }
}
