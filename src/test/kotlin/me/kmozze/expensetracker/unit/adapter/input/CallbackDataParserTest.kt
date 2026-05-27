package me.kmozze.expensetracker.unit.adapter.input

import me.kmozze.expensetracker.adapter.callback.CallbackData
import me.kmozze.expensetracker.adapter.input.CallbackDataParser
import me.kmozze.expensetracker.model.domain.ExpenseDateSelection
import me.kmozze.expensetracker.model.domain.UserCommand
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import java.util.UUID
import java.util.stream.Stream

class CallbackDataParserTest {
    @Test
    fun `parse cancel callback`() {
        val command = CallbackDataParser.parse(CallbackData.cancel())

        assertThat(command).isEqualTo(UserCommand.Cancel)
    }

    @Test
    fun `parse valid category selection callback`() {
        val command = CallbackDataParser.parse(CallbackData.selectCategory(CATEGORY_ID))

        assertThat(command).isEqualTo(UserCommand.SelectCategory(CATEGORY_ID))
    }

    @Test
    fun `parse today expense date callback`() {
        val command = CallbackDataParser.parse(CallbackData.selectExpenseDateToday())

        assertThat(command).isEqualTo(UserCommand.SelectExpenseDate(ExpenseDateSelection.TODAY))
    }

    @Test
    fun `parse yesterday expense date callback`() {
        val command = CallbackDataParser.parse(CallbackData.selectExpenseDateYesterday())

        assertThat(command).isEqualTo(UserCommand.SelectExpenseDate(ExpenseDateSelection.YESTERDAY))
    }

    @Test
    fun `parse manual expense date callback`() {
        val command = CallbackDataParser.parse(CallbackData.enterExpenseDateManually())

        assertThat(command).isEqualTo(UserCommand.SelectExpenseDate(ExpenseDateSelection.MANUAL))
    }

    @Test
    fun `parse expense deletion request callback`() {
        val command = CallbackDataParser.parse(CallbackData.requestExpenseDeletion(EXPENSE_ID))

        assertThat(command).isEqualTo(UserCommand.RequestExpenseDeletion(EXPENSE_ID))
    }

    @Test
    fun `parse expense deletion confirmation callback`() {
        val command = CallbackDataParser.parse(CallbackData.confirmExpenseDeletion(EXPENSE_ID))

        assertThat(command).isEqualTo(UserCommand.ConfirmExpenseDeletion(EXPENSE_ID))
    }

    @Test
    fun `parse expense deletion cancel callback`() {
        val command = CallbackDataParser.parse(CallbackData.cancelExpenseDeletion(EXPENSE_ID))

        assertThat(command).isEqualTo(UserCommand.CancelExpenseDeletion(EXPENSE_ID))
    }

    @ParameterizedTest(name = "callbackData: \"{0}\"")
    @MethodSource("invalidCategoryCallbacks")
    fun `parse invalid category selection callback`(callbackData: String) {
        val command = CallbackDataParser.parse(callbackData)

        assertThat(command).isEqualTo(UserCommand.InvalidCategorySelection)
    }

    @ParameterizedTest(name = "callbackData: \"{0}\"")
    @MethodSource("invalidExpenseDateCallbacks")
    fun `parse invalid expense date callback`(callbackData: String) {
        val command = CallbackDataParser.parse(callbackData)

        assertThat(command).isEqualTo(UserCommand.InvalidExpenseDateSelection)
    }

    @ParameterizedTest(name = "callbackData: \"{0}\"")
    @MethodSource("invalidExpenseDeletionCallbacks")
    fun `parse invalid expense deletion callback`(callbackData: String) {
        val command = CallbackDataParser.parse(callbackData)

        assertThat(command).isEqualTo(UserCommand.InvalidExpenseDeletion)
    }

    @ParameterizedTest(name = "callbackData: \"{0}\"")
    @MethodSource("unsupportedCallbacks")
    fun `parse unsupported callback`(callbackData: String?) {
        val command = CallbackDataParser.parse(callbackData)

        assertThat(command).isEqualTo(UserCommand.Unsupported)
    }

    private companion object {
        val CATEGORY_ID: UUID = UUID.fromString("00000000-0000-0000-0000-000000000001")
        val EXPENSE_ID: UUID = UUID.fromString("00000000-0000-0000-0000-000000000101")

        @JvmStatic
        fun invalidCategoryCallbacks(): Stream<String> =
            Stream.of(
                "select_category:",
                "select_category:not-a-uuid",
                "select_category:00000000-0000-0000-0000-00000000000x",
            )

        @JvmStatic
        fun invalidExpenseDateCallbacks(): Stream<String> =
            Stream.of(
                "select_expense_date:",
                "select_expense_date:not-a-date-choice",
            )

        @JvmStatic
        fun invalidExpenseDeletionCallbacks(): Stream<String> =
            Stream.of(
                "delete_expense:",
                "delete_expense:not-a-uuid",
                "confirm_delete_expense:",
                "confirm_delete_expense:not-a-uuid",
                "cancel_delete_expense:",
                "cancel_delete_expense:not-a-uuid",
            )

        @JvmStatic
        fun unsupportedCallbacks(): Stream<String?> =
            Stream.of(
                null,
                "",
                "unknown",
                "select-category:$CATEGORY_ID",
                "cancel:extra",
            )
    }
}
