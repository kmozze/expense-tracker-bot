package me.kmozze.expensetracker.unit.handler.statehandler.idle

import io.mockk.every
import io.mockk.junit5.MockKExtension
import io.mockk.mockk
import io.mockk.verify
import me.kmozze.expensetracker.handler.statehandler.expense.card.ExpenseCardActionHandler
import me.kmozze.expensetracker.handler.statehandler.expense.list.ExpenseListActionHandler
import me.kmozze.expensetracker.handler.statehandler.idle.IdleStateHandler
import me.kmozze.expensetracker.model.domain.bot.BotAction
import me.kmozze.expensetracker.model.domain.bot.BotText
import me.kmozze.expensetracker.model.domain.bot.HandlerResponse
import me.kmozze.expensetracker.model.domain.bot.OutgoingMessage
import me.kmozze.expensetracker.model.domain.bot.ResponseDelivery
import me.kmozze.expensetracker.model.domain.bot.UserCommand
import me.kmozze.expensetracker.model.domain.bot.UserInput
import me.kmozze.expensetracker.model.domain.bot.UserState
import me.kmozze.expensetracker.model.domain.expense.ExpenseListFilter
import me.kmozze.expensetracker.model.domain.expense.ExpenseListPeriod
import me.kmozze.expensetracker.support.makeUserInput
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import java.util.UUID
import java.util.stream.Stream

@ExtendWith(MockKExtension::class)
class IdleStateHandlerTest {
    private val expenseCardActionHandler: ExpenseCardActionHandler = mockk()
    private val expenseListActionHandler: ExpenseListActionHandler = mockk()
    private lateinit var handler: IdleStateHandler

    @BeforeEach
    fun setUp() {
        handler =
            IdleStateHandler(
                expenseCardActionHandler = expenseCardActionHandler,
                expenseListActionHandler = expenseListActionHandler,
            )
    }

    @Test
    fun `add expense command starts expense input`() {
        val result =
            handler.handle(
                input = makeInput(UserCommand.AddExpense),
                currentState = UserState.Idle,
            )

        assertThat(
            result.outgoingMessages
                .single()
                .text,
        ).isEqualTo(BotText.AddExpenseInstructions)
        assertThat(
            result.outgoingMessages
                .single()
                .actions,
        ).isEmpty()
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

        assertThat(
            result.outgoingMessages
                .single()
                .text,
        ).isEqualTo(BotText.FeatureInProgress)
        assertThat(
            result.outgoingMessages
                .single()
                .actions,
        ).isEmpty()
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

        assertThat(
            result.outgoingMessages
                .single()
                .text,
        ).isEqualTo(BotText.UnknownCommand)
        assertThat(
            result.outgoingMessages
                .single()
                .actions,
        ).containsExactly(BotAction.ShowMainMenu)
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

        assertThat(
            result.outgoingMessages
                .single()
                .text,
        ).isEqualTo(BotText.SelectionExpired)
        assertThat(
            result.outgoingMessages
                .single()
                .actions,
        ).containsExactly(BotAction.ShowMainMenu)
        assertThat(result.nextState).isEqualTo(UserState.Idle)
    }

    @Test
    fun `view expenses opens expense list settings`() {
        val input = makeInput(UserCommand.ViewExpenses)
        every { expenseListActionHandler.openSettings(input) } returns DELEGATE_RESPONSE

        val result =
            handler.handle(
                input = input,
                currentState = UserState.Idle,
            )

        assertThat(result).isSameAs(DELEGATE_RESPONSE)
        verify(exactly = 1) { expenseListActionHandler.openSettings(input) }
    }

    @Test
    fun `show expense list is delegated to expense list action handler`() {
        val command = UserCommand.ShowExpenseList(LIST_FILTER, page = 2, shouldEditCurrentMessage = true)
        val input = makeInput(command)
        every {
            expenseListActionHandler.showExpenseList(
                input = input,
                filter = LIST_FILTER,
                page = 2,
                shouldEditCurrentMessage = true,
            )
        } returns DELEGATE_RESPONSE

        val result =
            handler.handle(
                input = input,
                currentState = UserState.Idle,
            )

        assertThat(result).isSameAs(DELEGATE_RESPONSE)
        verify(exactly = 1) {
            expenseListActionHandler.showExpenseList(
                input = input,
                filter = LIST_FILTER,
                page = 2,
                shouldEditCurrentMessage = true,
            )
        }
    }

    @Test
    fun `open expense from list is delegated to expense list action handler`() {
        val input = makeInput(UserCommand.OpenExpenseFromList(EXPENSE_ID))
        every { expenseListActionHandler.openExpenseFromList(input, EXPENSE_ID) } returns DELEGATE_RESPONSE

        val result =
            handler.handle(
                input = input,
                currentState = UserState.Idle,
            )

        assertThat(result).isSameAs(DELEGATE_RESPONSE)
        verify(exactly = 1) { expenseListActionHandler.openExpenseFromList(input, EXPENSE_ID) }
    }

    @Test
    fun `invalid expense list action is delegated to silent alert handler`() {
        val input = makeInput(UserCommand.InvalidExpenseListAction)
        every { expenseListActionHandler.invalidListAction() } returns DELEGATE_RESPONSE

        val result =
            handler.handle(
                input = input,
                currentState = UserState.Idle,
            )

        assertThat(result).isSameAs(DELEGATE_RESPONSE)
        verify(exactly = 1) { expenseListActionHandler.invalidListAction() }
    }

    @Test
    fun `request expense edit is delegated to expense card action handler`() {
        val input = makeInput(UserCommand.RequestExpenseEdit(EXPENSE_ID))
        every { expenseCardActionHandler.requestExpenseEdit(input, EXPENSE_ID) } returns DELEGATE_RESPONSE

        val result =
            handler.handle(
                input = input,
                currentState = UserState.Idle,
            )

        assertThat(result).isSameAs(DELEGATE_RESPONSE)
        verify(exactly = 1) { expenseCardActionHandler.requestExpenseEdit(input, EXPENSE_ID) }
    }

    @Test
    fun `request expense deletion is delegated to expense card action handler`() {
        val input = makeInput(UserCommand.RequestExpenseDeletion(EXPENSE_ID))
        every { expenseCardActionHandler.requestExpenseDeletion(input, EXPENSE_ID) } returns DELEGATE_RESPONSE

        val result =
            handler.handle(
                input = input,
                currentState = UserState.Idle,
            )

        assertThat(result).isSameAs(DELEGATE_RESPONSE)
        verify(exactly = 1) { expenseCardActionHandler.requestExpenseDeletion(input, EXPENSE_ID) }
    }

    @Test
    fun `confirm expense deletion is delegated to expense card action handler`() {
        val input = makeInput(UserCommand.ConfirmExpenseDeletion(EXPENSE_ID))
        every { expenseCardActionHandler.confirmExpenseDeletion(input, EXPENSE_ID) } returns DELEGATE_RESPONSE

        val result =
            handler.handle(
                input = input,
                currentState = UserState.Idle,
            )

        assertThat(result).isSameAs(DELEGATE_RESPONSE)
        verify(exactly = 1) { expenseCardActionHandler.confirmExpenseDeletion(input, EXPENSE_ID) }
    }

    @Test
    fun `cancel expense deletion is delegated to expense card action handler`() {
        val input = makeInput(UserCommand.CancelExpenseDeletion(EXPENSE_ID))
        every { expenseCardActionHandler.cancelExpenseDeletion(input, EXPENSE_ID) } returns DELEGATE_RESPONSE

        val result =
            handler.handle(
                input = input,
                currentState = UserState.Idle,
            )

        assertThat(result).isSameAs(DELEGATE_RESPONSE)
        verify(exactly = 1) { expenseCardActionHandler.cancelExpenseDeletion(input, EXPENSE_ID) }
    }

    private fun makeInput(command: UserCommand): UserInput =
        makeUserInput(
            userId = USER_ID,
            chatId = CHAT_ID,
            command = command,
        )

    private companion object {
        const val USER_ID = 123L
        const val CHAT_ID = 456L
        val EXPENSE_ID: UUID = UUID.fromString("00000000-0000-0000-0000-000000000001")
        val LIST_FILTER: ExpenseListFilter = ExpenseListFilter(period = ExpenseListPeriod.Month)
        val DELEGATE_RESPONSE: HandlerResponse =
            HandlerResponse(
                outgoingMessages =
                    listOf(
                        OutgoingMessage(
                            text = BotText.Done,
                            actions = emptyList(),
                            delivery = ResponseDelivery.SendNewMessage,
                        ),
                    ),
                nextState = UserState.Idle,
            )

        @JvmStatic
        fun featureInProgressCommands(): Stream<UserCommand> =
            Stream.of(
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
                UserCommand.InvalidExpenseAction,
            )
    }
}
