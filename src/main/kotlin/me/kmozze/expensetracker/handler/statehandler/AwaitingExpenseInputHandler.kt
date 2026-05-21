package me.kmozze.expensetracker.handler.statehandler

import me.kmozze.expensetracker.exception.BusinessException
import me.kmozze.expensetracker.model.domain.BotAction
import me.kmozze.expensetracker.model.domain.BotMessage
import me.kmozze.expensetracker.model.domain.HandlerResponse
import me.kmozze.expensetracker.model.domain.HandlerResult
import me.kmozze.expensetracker.model.domain.UserCommand
import me.kmozze.expensetracker.model.domain.UserInput
import me.kmozze.expensetracker.model.domain.UserState
import me.kmozze.expensetracker.service.CategoryService
import me.kmozze.expensetracker.service.ExpenseService
import org.springframework.stereotype.Component
import kotlin.reflect.KClass

@Component
class AwaitingExpenseInputHandler(
    private val expenseService: ExpenseService,
    private val categoryService: CategoryService,
) : StateHandler {
    override val supportedStateClass: KClass<out UserState> = UserState.AwaitingExpenseInput::class

    override fun handle(
        input: UserInput,
        currentState: UserState,
    ): HandlerResult {
        require(currentState is UserState.AwaitingExpenseInput) {
            "AwaitingExpenseInputHandler requires AwaitingExpenseInput state"
        }

        val text =
            when (val command = input.command) {
                UserCommand.AddExpense -> return repeatExpenseInstructions()
                UserCommand.ViewExpenses,
                UserCommand.Categories,
                UserCommand.Statistics,
                -> return featureInProgress()
                is UserCommand.PlainText -> command.value
                else -> return repeatExpenseInstructions()
            }

        val parsedExpense =
            try {
                expenseService.parseExpense(text)
            } catch (e: BusinessException) {
                return HandlerResult(
                    response =
                        HandlerResponse(
                            message = BotMessage.Error(e.errorCode),
                            actions = listOf(BotAction.ShowMainMenu),
                        ),
                    nextState = UserState.AwaitingExpenseInput,
                )
            }

        val categories = categoryService.getCategories(input.userId)
        if (categories.isEmpty()) {
            return noCategories()
        }

        return HandlerResult(
            response =
                HandlerResponse(
                    message =
                        BotMessage.SelectCategory(
                            amount = parsedExpense.amount,
                            description = parsedExpense.description,
                        ),
                    actions = listOf(BotAction.ShowCategorySelection(categories)),
                ),
            nextState = UserState.AwaitingCategorySelection(parsedExpense),
        )
    }

    private fun repeatExpenseInstructions(): HandlerResult =
        HandlerResult(
            response =
                HandlerResponse(
                    message = BotMessage.AddExpenseInstructions,
                    actions = listOf(BotAction.ShowMainMenu),
                ),
            nextState = UserState.AwaitingExpenseInput,
        )

    private fun featureInProgress(): HandlerResult =
        HandlerResult(
            response =
                HandlerResponse(
                    message = BotMessage.FeatureInProgress,
                    actions = listOf(BotAction.ShowMainMenu),
                ),
            nextState = UserState.Idle,
        )

    private fun noCategories(): HandlerResult =
        HandlerResult(
            response =
                HandlerResponse(
                    message = BotMessage.NoCategories,
                    actions = listOf(BotAction.ShowMainMenu),
                ),
            nextState = UserState.Idle,
        )
}
