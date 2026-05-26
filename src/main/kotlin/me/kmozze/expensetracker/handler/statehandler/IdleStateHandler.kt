package me.kmozze.expensetracker.handler.statehandler

import me.kmozze.expensetracker.model.domain.BotAction
import me.kmozze.expensetracker.model.domain.BotMessage
import me.kmozze.expensetracker.model.domain.HandlerResponse
import me.kmozze.expensetracker.model.domain.HandlerResult
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
                            message = BotMessage.AddExpenseInstructions,
                            actions = listOf(BotAction.ShowMainMenu),
                        ),
                    nextState = UserState.AwaitingExpenseInput,
                )

            UserCommand.ViewExpenses,
            UserCommand.Categories,
            UserCommand.Statistics,
            ->
                HandlerResult(
                    response =
                        HandlerResponse(
                            message = BotMessage.FeatureInProgress,
                            actions = listOf(BotAction.ShowMainMenu),
                        ),
                    nextState = UserState.Idle,
                )

            UserCommand.Cancel,
            is UserCommand.SelectCategory,
            UserCommand.InvalidCategorySelection,
            is UserCommand.SelectExpenseDate,
            UserCommand.InvalidExpenseDateSelection,
            ->
                HandlerResult(
                    response =
                        HandlerResponse(
                            message = BotMessage.SelectionExpired,
                            actions = listOf(BotAction.ShowMainMenu),
                        ),
                    nextState = UserState.Idle,
                )

            else ->
                HandlerResult(
                    response =
                        HandlerResponse(
                            message = BotMessage.UnknownCommand,
                            actions = listOf(BotAction.ShowMainMenu),
                        ),
                    nextState = UserState.Idle,
                )
        }
    }
}
