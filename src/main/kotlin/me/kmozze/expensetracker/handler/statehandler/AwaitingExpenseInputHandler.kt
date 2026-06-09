package me.kmozze.expensetracker.handler.statehandler

import me.kmozze.expensetracker.exception.BusinessException
import me.kmozze.expensetracker.handler.handlerResponse
import me.kmozze.expensetracker.handler.outgoingMessage
import me.kmozze.expensetracker.model.domain.BotAction
import me.kmozze.expensetracker.model.domain.BotText
import me.kmozze.expensetracker.model.domain.HandlerResponse
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
    ): HandlerResponse {
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

        val expenseDraft =
            try {
                expenseService.parseExpense(text)
            } catch (e: BusinessException) {
                return handlerResponse(
                    message =
                        outgoingMessage(
                            text = BotText.Error(e.errorCode),
                            actions = emptyList(),
                        ),
                    nextState = UserState.AwaitingExpenseInput,
                )
            }

        val categories = categoryService.getCategories(input.userId)
        if (categories.isEmpty()) {
            return noCategories()
        }

        return handlerResponse(
            messages =
                listOf(
                    outgoingMessage(
                        text = expenseDraft.toExpenseView(),
                        actions = emptyList(),
                    ),
                    outgoingMessage(
                        text =
                            BotText.SelectCategory,
                        actions = listOf(BotAction.ShowCategorySelection(categories.map { it.name })),
                    ),
                ),
            nextState = UserState.AwaitingCategorySelection(expenseDraft),
        )
    }

    private fun repeatExpenseInstructions(): HandlerResponse =
        handlerResponse(
            message =
                outgoingMessage(
                    text = BotText.AddExpenseInstructions,
                    actions = emptyList(),
                ),
            nextState = UserState.AwaitingExpenseInput,
        )

    private fun featureInProgress(): HandlerResponse =
        handlerResponse(
            message =
                outgoingMessage(
                    text = BotText.FeatureInProgress,
                    actions = emptyList(),
                ),
            nextState = UserState.Idle,
        )

    private fun noCategories(): HandlerResponse =
        handlerResponse(
            message =
                outgoingMessage(
                    text = BotText.NoCategories,
                    actions = listOf(BotAction.ShowMainMenu),
                ),
            nextState = UserState.Idle,
        )
}
