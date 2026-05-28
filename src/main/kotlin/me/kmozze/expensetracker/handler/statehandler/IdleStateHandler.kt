package me.kmozze.expensetracker.handler.statehandler

import me.kmozze.expensetracker.model.domain.BotAction
import me.kmozze.expensetracker.model.domain.BotText
import me.kmozze.expensetracker.model.domain.HandlerResponse
import me.kmozze.expensetracker.model.domain.HandlerResult
import me.kmozze.expensetracker.model.domain.OutgoingMessage
import me.kmozze.expensetracker.model.domain.UserCommand
import me.kmozze.expensetracker.model.domain.UserInput
import me.kmozze.expensetracker.model.domain.UserState
import org.springframework.stereotype.Component
import kotlin.reflect.KClass

@Component
class IdleStateHandler : StateHandler {
    override val supportedStateClass: KClass<out UserState> = UserState.Idle::class

    override fun handle(
        input: UserInput,
        currentState: UserState,
    ): HandlerResult {
        require(currentState is UserState.Idle) {
            "IdleStateHandler requires Idle state"
        }

        return when (input.command) {
            UserCommand.AddExpense ->
                HandlerResult(
                    response =
                        HandlerResponse(
                            outgoingMessages =
                                listOf(
                                    OutgoingMessage(
                                        text = BotText.AddExpenseInstructions,
                                        actions = emptyList(),
                                    ),
                                ),
                        ),
                    nextState = UserState.AwaitingExpenseInput,
                )

            UserCommand.ViewExpenses,
            UserCommand.Categories,
            UserCommand.Statistics,
            is UserCommand.RequestExpenseEdit,
            is UserCommand.RequestExpenseDeletion,
            ->
                HandlerResult(
                    response =
                        HandlerResponse(
                            outgoingMessages =
                                listOf(
                                    OutgoingMessage(
                                        text = BotText.FeatureInProgress,
                                        actions = emptyList(),
                                    ),
                                ),
                        ),
                    nextState = UserState.Idle,
                )

            UserCommand.Cancel,
            UserCommand.InvalidExpenseAction,
            ->
                HandlerResult(
                    response =
                        HandlerResponse(
                            outgoingMessages =
                                listOf(
                                    OutgoingMessage(
                                        text = BotText.SelectionExpired,
                                        actions = listOf(BotAction.ShowMainMenu),
                                    ),
                                ),
                        ),
                    nextState = UserState.Idle,
                )

            else ->
                HandlerResult(
                    response =
                        HandlerResponse(
                            outgoingMessages =
                                listOf(
                                    OutgoingMessage(
                                        text = BotText.UnknownCommand,
                                        actions = listOf(BotAction.ShowMainMenu),
                                    ),
                                ),
                        ),
                    nextState = UserState.Idle,
                )
        }
    }
}
