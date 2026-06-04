package me.kmozze.expensetracker.handler.statehandler

import me.kmozze.expensetracker.handler.handlerResponse
import me.kmozze.expensetracker.handler.outgoingMessage
import me.kmozze.expensetracker.model.domain.BotAction
import me.kmozze.expensetracker.model.domain.BotText
import me.kmozze.expensetracker.model.domain.HandlerResponse
import me.kmozze.expensetracker.model.domain.UserCommand
import me.kmozze.expensetracker.model.domain.UserInput
import me.kmozze.expensetracker.model.domain.UserState
import org.springframework.stereotype.Component
import kotlin.reflect.KClass

@Component
class IdleStateHandler(
    private val expenseCardActionHandler: ExpenseCardActionHandler,
) : StateHandler {
    override val supportedStateClass: KClass<out UserState> = UserState.Idle::class

    override fun handle(
        input: UserInput,
        currentState: UserState,
    ): HandlerResponse {
        require(currentState is UserState.Idle) {
            "IdleStateHandler requires Idle state"
        }

        return when (val command = input.command) {
            UserCommand.AddExpense ->
                handlerResponse(
                    message =
                        outgoingMessage(
                            text = BotText.AddExpenseInstructions,
                            actions = emptyList(),
                        ),
                    nextState = UserState.AwaitingExpenseInput,
                )

            UserCommand.ViewExpenses,
            UserCommand.Categories,
            UserCommand.Statistics,
            ->
                handlerResponse(
                    message =
                        outgoingMessage(
                            text = BotText.FeatureInProgress,
                            actions = emptyList(),
                        ),
                    nextState = UserState.Idle,
                )

            is UserCommand.RequestExpenseEdit ->
                expenseCardActionHandler.requestExpenseEdit(input, command.expenseId)

            is UserCommand.RequestExpenseDeletion ->
                expenseCardActionHandler.requestExpenseDeletion(input, command.expenseId)

            is UserCommand.ConfirmExpenseDeletion ->
                expenseCardActionHandler.confirmExpenseDeletion(input, command.expenseId)

            is UserCommand.CancelExpenseDeletion ->
                expenseCardActionHandler.cancelExpenseDeletion(input, command.expenseId)

            UserCommand.Cancel,
            UserCommand.InvalidExpenseAction,
            ->
                handlerResponse(
                    message =
                        outgoingMessage(
                            text = BotText.SelectionExpired,
                            actions = listOf(BotAction.ShowMainMenu),
                        ),
                    nextState = UserState.Idle,
                )

            else ->
                handlerResponse(
                    message =
                        outgoingMessage(
                            text = BotText.UnknownCommand,
                            actions = listOf(BotAction.ShowMainMenu),
                        ),
                    nextState = UserState.Idle,
                )
        }
    }
}
