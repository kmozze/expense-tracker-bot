package me.kmozze.expensetracker.service.parser

import me.kmozze.expensetracker.exception.BusinessErrorCode
import me.kmozze.expensetracker.exception.exception
import org.springframework.stereotype.Service
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.time.format.ResolverStyle

@Service
class InputExpenseDateParsingService {
    fun parse(text: String): LocalDate =
        try {
            LocalDate.parse(text.trim(), USER_DATE_FORMATTER)
        } catch (e: DateTimeParseException) {
            throw BusinessErrorCode.EXPENSE_DATE_INVALID_FORMAT.exception()
        }

    private companion object {
        val USER_DATE_FORMATTER: DateTimeFormatter =
            DateTimeFormatter
                .ofPattern("dd.MM.uuuu")
                .withResolverStyle(ResolverStyle.STRICT)
    }
}
