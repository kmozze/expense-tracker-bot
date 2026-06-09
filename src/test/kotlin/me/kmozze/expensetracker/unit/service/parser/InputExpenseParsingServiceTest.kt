package me.kmozze.expensetracker.unit.service.parser

import me.kmozze.expensetracker.exception.BusinessErrorCode
import me.kmozze.expensetracker.exception.BusinessException
import me.kmozze.expensetracker.model.domain.expense.Money
import me.kmozze.expensetracker.service.parser.InputExpenseParsingService
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import java.math.BigDecimal
import java.util.stream.Stream

class InputExpenseParsingServiceTest {
    private val service = InputExpenseParsingService()

    @ParameterizedTest(name = "input: \"{0}\"")
    @MethodSource("validInputs")
    fun `should parse valid inputs`(
        input: String,
        expectedDescription: String?,
        expectedAmount: BigDecimal,
    ) {
        val result = service.parse(input)

        Assertions.assertThat(result.description).isEqualTo(expectedDescription)
        Assertions.assertThat(result.amount).isEqualTo(Money.of(expectedAmount))
    }

    @ParameterizedTest(name = "input: \"{0}\"")
    @MethodSource("invalidFormatInputs")
    fun `should throw format exception`(input: String) {
        val exception =
            assertThrows<BusinessException> {
                service.parse(input)
            }

        Assertions.assertThat(exception.errorCode).isEqualTo(BusinessErrorCode.EXPENSE_INVALID_FORMAT)
    }

    @ParameterizedTest(name = "input: \"{0}\"")
    @MethodSource("invalidAmountInputs")
    fun `should throw validation exception`(input: String) {
        val exception =
            assertThrows<BusinessException> {
                service.parse(input)
            }

        Assertions.assertThat(exception.errorCode).isEqualTo(BusinessErrorCode.INVALID_AMOUNT)
    }

    companion object {
        @JvmStatic
        fun validInputs(): Stream<Arguments> =
            Stream.of(
                Arguments.arguments("500", null, BigDecimal("500.00")),
                Arguments.arguments("150.50 Кофе", "Кофе", BigDecimal("150.50")),
                Arguments.arguments("10,50 Milk", "Milk", BigDecimal("10.50")),
                Arguments.arguments("Lunch 500", "Lunch", BigDecimal("500.00")),
                Arguments.arguments("  Taxi   400  ", "Taxi", BigDecimal("400.00")),
                Arguments.arguments("Dinner at restaurant 2500", "Dinner at restaurant", BigDecimal("2500.00")),
                Arguments.arguments("Coffee 10.555", "Coffee", BigDecimal("10.56")),
                Arguments.arguments("Snack 10.554", "Snack", BigDecimal("10.55")),
            )

        @JvmStatic
        fun invalidFormatInputs(): Stream<String> =
            Stream.of(
                "",
                "   ",
                "Coffee",
                "No digits",
                "10.5.5 Bread",
            )

        @JvmStatic
        fun invalidAmountInputs(): Stream<String> =
            Stream.of(
                "0",
                "Coffee 0",
                "Coffee 0.004",
                "-10 Taxi",
                "Gym -5.50",
                "0.00 Gift",
            )
    }
}
