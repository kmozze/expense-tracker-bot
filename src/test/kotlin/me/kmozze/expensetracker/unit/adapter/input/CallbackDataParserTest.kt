package me.kmozze.expensetracker.unit.adapter.input

import me.kmozze.expensetracker.adapter.callback.CallbackData
import me.kmozze.expensetracker.adapter.input.CallbackDataParser
import me.kmozze.expensetracker.model.domain.UserCommand
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import java.util.UUID
import java.util.stream.Stream

class CallbackDataParserTest {
    @ParameterizedTest(name = "callbackData: \"{0}\"")
    @MethodSource("menuCallbacks")
    fun `parse menu callback`(
        callbackData: String,
        expectedCommand: UserCommand,
    ) {
        val command = CallbackDataParser.parse(callbackData)

        assertThat(command).isEqualTo(expectedCommand)
    }

    @Test
    fun `parse expense edit callback`() {
        val command = CallbackDataParser.parse(CallbackData.editExpense(EXPENSE_ID))

        assertThat(command).isEqualTo(UserCommand.RequestExpenseEdit(EXPENSE_ID))
    }

    @Test
    fun `parse expense deletion callback`() {
        val command = CallbackDataParser.parse(CallbackData.deleteExpense(EXPENSE_ID))

        assertThat(command).isEqualTo(UserCommand.RequestExpenseDeletion(EXPENSE_ID))
    }

    @Test
    fun `parse expense deletion confirmation callback`() {
        val command = CallbackDataParser.parse(CallbackData.confirmExpenseDeletion(EXPENSE_ID))

        assertThat(command).isEqualTo(UserCommand.ConfirmExpenseDeletion(EXPENSE_ID))
    }

    @Test
    fun `parse expense deletion cancellation callback`() {
        val command = CallbackDataParser.parse(CallbackData.cancelExpenseDeletion(EXPENSE_ID))

        assertThat(command).isEqualTo(UserCommand.CancelExpenseDeletion(EXPENSE_ID))
    }

    @ParameterizedTest(name = "callbackData: \"{0}\"")
    @MethodSource("invalidExpenseActionCallbacks")
    fun `parse invalid expense action callback`(callbackData: String) {
        val command = CallbackDataParser.parse(callbackData)

        assertThat(command).isEqualTo(UserCommand.InvalidExpenseAction)
    }

    @ParameterizedTest(name = "callbackData: \"{0}\"")
    @MethodSource("unsupportedCallbacks")
    fun `parse unsupported callback`(callbackData: String?) {
        val command = CallbackDataParser.parse(callbackData)

        assertThat(command).isEqualTo(UserCommand.Unsupported)
    }

    private companion object {
        val EXPENSE_ID: UUID = UUID.fromString("00000000-0000-0000-0000-000000000001")

        @JvmStatic
        fun menuCallbacks(): Stream<Arguments> =
            Stream.of(
                Arguments.arguments(
                    CallbackData.menuAddExpense(),
                    UserCommand.AddExpense,
                ),
                Arguments.arguments(
                    CallbackData.menuViewExpenses(),
                    UserCommand.ViewExpenses,
                ),
                Arguments.arguments(
                    CallbackData.menuCategories(),
                    UserCommand.Categories,
                ),
                Arguments.arguments(
                    CallbackData.menuStatistics(),
                    UserCommand.Statistics,
                ),
            )

        @JvmStatic
        fun invalidExpenseActionCallbacks(): Stream<String> =
            Stream.of(
                "expense:edit:",
                "expense:edit:not-a-uuid",
                "expense:edit:00000000-0000-0000-0000-00000000000x",
                "expense:delete:",
                "expense:delete:not-a-uuid",
                "expense:delete:00000000-0000-0000-0000-00000000000x",
                "expense:delete:confirm:",
                "expense:delete:confirm:not-a-uuid",
                "expense:delete:confirm:00000000-0000-0000-0000-00000000000x",
                "expense:delete:cancel:",
                "expense:delete:cancel:not-a-uuid",
                "expense:delete:cancel:00000000-0000-0000-0000-00000000000x",
            )

        @JvmStatic
        fun unsupportedCallbacks(): Stream<String?> =
            Stream.of(
                null,
                "",
                "unknown",
                "cancel:extra",
            )
    }
}
