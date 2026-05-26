package me.kmozze.expensetracker.unit.handler.statehandler

import me.kmozze.expensetracker.handler.statehandler.IdleStateHandler
import me.kmozze.expensetracker.model.domain.BotAction
import me.kmozze.expensetracker.model.domain.BotMessage
import me.kmozze.expensetracker.model.domain.ExpenseDateSelection
import me.kmozze.expensetracker.model.domain.UserCommand
import me.kmozze.expensetracker.model.domain.UserState
import me.kmozze.expensetracker.support.makeUserInput
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import java.util.UUID
import java.util.stream.Stream

class IdleStateHandlerTest {
    private val handler = IdleStateHandler()

    @Test
    fun `add expense command starts expense input`() {
        val result =
            handler.handle(
                input = makeInput(UserCommand.AddExpense),
                currentState = UserState.Idle,
            )

        assertThat(result.response.message).isEqualTo(BotMessage.AddExpenseInstructions)
        assertThat(result.response.actions).containsExactly(BotAction.ShowMainMenu)
        assertThat(result.nextState).isEqualTo(UserState.AwaitingExpenseInput)
    }

    @ParameterizedTest(name = "command: {0}")
    @MethodSource("featureInProgressCommands")
    fun `menu commands return feature in progress`(command: UserCommand) {
        val result =
            handler.handle(
                input = makeInput(command),
                currentState = UserState.Idle,
            )

        assertThat(result.response.message).isEqualTo(BotMessage.FeatureInProgress)
        assertThat(result.response.actions).containsExactly(BotAction.ShowMainMenu)
        assertThat(result.nextState).isEqualTo(UserState.Idle)
    }

    @ParameterizedTest(name = "command: {0}")
    @MethodSource("unsupportedCommands")
    fun `unsupported commands keep idle state`(command: UserCommand) {
        val result =
            handler.handle(
                input = makeInput(command),
                currentState = UserState.Idle,
            )

        assertThat(result.response.message).isEqualTo(BotMessage.UnknownCommand)
        assertThat(result.response.actions).containsExactly(BotAction.ShowMainMenu)
        assertThat(result.nextState).isEqualTo(UserState.Idle)
    }

    @ParameterizedTest(name = "command: {0}")
    @MethodSource("staleCallbackCommands")
    fun `stale callback commands keep idle state with expired selection message`(command: UserCommand) {
        val result =
            handler.handle(
                input = makeInput(command),
                currentState = UserState.Idle,
            )

        assertThat(result.response.message).isEqualTo(BotMessage.SelectionExpired)
        assertThat(result.response.actions).containsExactly(BotAction.ShowMainMenu)
        assertThat(result.nextState).isEqualTo(UserState.Idle)
    }

    private fun makeInput(command: UserCommand) =
        makeUserInput(
            userId = USER_ID,
            chatId = CHAT_ID,
            command = command,
        )

    private companion object {
        const val USER_ID = 123L
        const val CHAT_ID = 456L

        @JvmStatic
        fun featureInProgressCommands(): Stream<UserCommand> =
            Stream.of(
                UserCommand.ViewExpenses,
                UserCommand.Categories,
                UserCommand.Statistics,
            )

        @JvmStatic
        fun unsupportedCommands(): Stream<UserCommand> =
            Stream.of(
                UserCommand.Unsupported,
                UserCommand.PlainText("500 такси"),
            )

        @JvmStatic
        fun staleCallbackCommands(): Stream<UserCommand> =
            Stream.of(
                UserCommand.Cancel,
                UserCommand.SelectCategory(UUID.fromString("00000000-0000-0000-0000-000000000001")),
                UserCommand.SelectExpenseDate(ExpenseDateSelection.TODAY),
                UserCommand.InvalidCategorySelection,
                UserCommand.InvalidExpenseDateSelection,
            )
    }
}
