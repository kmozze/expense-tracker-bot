package me.kmozze.expensetracker.service

import me.kmozze.expensetracker.model.Expense
import me.kmozze.expensetracker.model.ParsedExpense
import me.kmozze.expensetracker.service.parser.InputExpenseParsingService
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class ExpenseService(
    private val expenseTextParser: InputExpenseParsingService
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    fun addExpense(text: String): ParsedExpense {
        val parsedExpense = expenseTextParser.parse(text)
        logger.info("Expense parsed successfully: {}", parsedExpense)

        return parsedExpense
    }
}
