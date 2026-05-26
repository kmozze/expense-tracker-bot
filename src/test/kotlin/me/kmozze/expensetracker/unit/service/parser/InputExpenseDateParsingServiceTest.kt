package me.kmozze.expensetracker.unit.service.parser

import me.kmozze.expensetracker.exception.BusinessErrorCode
import me.kmozze.expensetracker.exception.BusinessException
import me.kmozze.expensetracker.service.parser.InputExpenseDateParsingService
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.LocalDate

class InputExpenseDateParsingServiceTest {
    private val service = InputExpenseDateParsingService()

    @Test
    fun `parse valid user date`() {
        val result = service.parse("24.05.2026")

        assertThat(result).isEqualTo(LocalDate.parse("2026-05-24"))
    }

    @Test
    fun `parse trims spaces around valid date`() {
        val result = service.parse(" 24.05.2026 ")

        assertThat(result).isEqualTo(LocalDate.parse("2026-05-24"))
    }

    @Test
    fun `invalid date throws business exception`() {
        val exception =
            assertThrows<BusinessException> {
                service.parse("31.02.2026")
            }

        assertThat(exception.errorCode).isEqualTo(BusinessErrorCode.EXPENSE_DATE_INVALID_FORMAT)
    }
}
