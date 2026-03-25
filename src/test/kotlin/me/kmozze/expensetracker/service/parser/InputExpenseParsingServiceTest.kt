package me.kmozze.expensetracker.service.parser

import me.kmozze.expensetracker.exception.ExpenseInvalidFormatException
import me.kmozze.expensetracker.exception.ExpenseValidationException
import org.assertj.core.api.Assertions
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import java.math.BigDecimal
import java.util.stream.Stream

class InputExpenseParsingServiceTest {

    private val service = InputExpenseParsingService()

    @ParameterizedTest(name = "{0}")
    @MethodSource("validInputs")
    fun `should parse valid inputs`(input: String, expectedCategory: String, expectedAmount: BigDecimal) {
        val result = service.parse(input)

        Assertions.assertThat(result.category).isEqualTo(expectedCategory)
        Assertions.assertThat(result.amount).isEqualByComparingTo(expectedAmount)
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("invalidFormatInputs")
    fun `should throw format exception`(input: String) {
        Assertions.assertThatThrownBy { service.parse(input) }
            .isInstanceOf(ExpenseInvalidFormatException::class.java)
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("invalidAmountInputs")
    fun `should throw validation exception`(input: String) {
        Assertions.assertThatThrownBy { service.parse(input) }
            .isInstanceOf(ExpenseValidationException::class.java)
    }

    companion object {
        @JvmStatic
        fun validInputs(): Stream<Arguments> = Stream.of(
            Arguments.arguments("150.50 Кофе", "Кофе", BigDecimal("150.50")),
            Arguments.arguments("10,50 Milk", "Milk", BigDecimal("10.50")),
            Arguments.arguments("Lunch 500", "Lunch", BigDecimal("500")),
            Arguments.arguments("  Taxi   400  ", "Taxi", BigDecimal("400")),
            Arguments.arguments("Dinner at restaurant 2500", "Dinner at restaurant", BigDecimal("2500")),
        )

        @JvmStatic
        fun invalidFormatInputs(): Stream<String> = Stream.of(
            "", "   ", "Coffee", "100", "No digits", "10.5.5 Bread"
        )

        @JvmStatic
        fun invalidAmountInputs(): Stream<String> = Stream.of(
            "Coffee 0", "-10 Taxi", "Gym -5.50", "0.00 Gift"
        )
    }
}