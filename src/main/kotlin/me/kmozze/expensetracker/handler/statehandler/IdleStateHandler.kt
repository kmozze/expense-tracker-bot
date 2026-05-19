package me.kmozze.expensetracker.handler.statehandler

import me.kmozze.expensetracker.adapter.ui.Buttons
import me.kmozze.expensetracker.model.domain.Action
import me.kmozze.expensetracker.model.domain.HandlerResponse
import me.kmozze.expensetracker.model.domain.HandlerResult
import me.kmozze.expensetracker.model.domain.Message
import me.kmozze.expensetracker.model.domain.UserInput
import me.kmozze.expensetracker.model.domain.UserState
import org.springframework.stereotype.Component
import kotlin.reflect.KClass

@Component
class IdleStateHandler : StateHandler {
    override val supportedStateClass: KClass<out UserState> = UserState.Idle::class

    override fun handle(input: UserInput): HandlerResult =
        when (input.text) {
            Buttons.ADD_EXPENSE ->
                HandlerResult(
                    response =
                        HandlerResponse(
                            message = Message.AddExpenseInstructions,
                            actions = listOf(Action.ShowMainMenu),
                        ),
                    nextState = UserState.AwaitingExpenseInput,
                )

            Buttons.VIEW_EXPENSES,
            Buttons.CATEGORIES,
            Buttons.STATISTICS,
            ->
                HandlerResult(
                    response =
                        HandlerResponse(
                            message = Message.FeatureInProgress,
                            actions = listOf(Action.ShowMainMenu),
                        ),
                    nextState = UserState.Idle,
                )

            else ->
                HandlerResult(
                    response =
                        HandlerResponse(
                            message = Message.UnknownCommand,
                            actions = listOf(Action.ShowMainMenu),
                        ),
                    nextState = UserState.Idle,
                )
        }
}
