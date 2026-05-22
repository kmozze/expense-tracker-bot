package me.kmozze.expensetracker.unit.adapter.input

import me.kmozze.expensetracker.adapter.callback.CallbackData
import me.kmozze.expensetracker.adapter.input.UserCommandParser
import me.kmozze.expensetracker.adapter.ui.Buttons
import me.kmozze.expensetracker.model.domain.UserCommand
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import java.util.UUID
import java.util.stream.Stream

class UserCommandParserTest {
    @ParameterizedTest(name = "text: \"{0}\"")
    @MethodSource("textCommands")
    fun `parse text commands`(
        text: String,
        expectedCommand: UserCommand,
    ) {
        val command = UserCommandParser.parse(text = text, callbackData = null)

        assertThat(command).isEqualTo(expectedCommand)
    }

    @Test
    fun `parse plain text as plain text command`() {
        val command = UserCommandParser.parse(text = "500 такси", callbackData = null)

        assertThat(command).isEqualTo(UserCommand.PlainText("500 такси"))
    }

    @Test
    fun `return unsupported when text and callback data are missing`() {
        val command = UserCommandParser.parse(text = null, callbackData = null)

        assertThat(command).isEqualTo(UserCommand.Unsupported)
    }

    @Test
    fun `parse callback data before text`() {
        val command =
            UserCommandParser.parse(
                text = Buttons.ADD_EXPENSE,
                callbackData = CallbackData.cancel(),
            )

        assertThat(command).isEqualTo(UserCommand.Cancel)
    }

    private companion object {
        val CATEGORY_ID: UUID = UUID.fromString("00000000-0000-0000-0000-000000000001")
        val SELECT_CATEGORY_CALLBACK: String = CallbackData.selectCategory(CATEGORY_ID)

        @JvmStatic
        fun textCommands(): Stream<Arguments> =
            Stream.of(
                Arguments.arguments("/start", UserCommand.Start),
                Arguments.arguments("/START", UserCommand.Start),
                Arguments.arguments(Buttons.ADD_EXPENSE, UserCommand.AddExpense),
                Arguments.arguments(Buttons.VIEW_EXPENSES, UserCommand.ViewExpenses),
                Arguments.arguments(Buttons.CATEGORIES, UserCommand.Categories),
                Arguments.arguments(Buttons.STATISTICS, UserCommand.Statistics),
                Arguments.arguments(SELECT_CATEGORY_CALLBACK, UserCommand.PlainText(SELECT_CATEGORY_CALLBACK)),
            )
    }
}
