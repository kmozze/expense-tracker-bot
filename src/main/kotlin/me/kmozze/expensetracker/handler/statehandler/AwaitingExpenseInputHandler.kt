package me.kmozze.expensetracker.handler.statehandler

import me.kmozze.expensetracker.adapter.ui.Buttons
import me.kmozze.expensetracker.exception.BusinessException
import me.kmozze.expensetracker.model.domain.Action
import me.kmozze.expensetracker.model.domain.HandlerResponse
import me.kmozze.expensetracker.model.domain.HandlerResult
import me.kmozze.expensetracker.model.domain.Message
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

    override fun handle(input: UserInput): HandlerResult {
        if (input.text == Buttons.ADD_EXPENSE) {
            return HandlerResult(
                response =
                    HandlerResponse(
                        message = Message.AddExpenseInstructions,
                        actions = listOf(Action.ShowMainMenu),
                    ),
                nextState = UserState.AwaitingExpenseInput,
            )
        }

        if (input.text in setOf(Buttons.VIEW_EXPENSES, Buttons.CATEGORIES, Buttons.STATISTICS)) {
            return HandlerResult(
                response =
                    HandlerResponse(
                        message = Message.FeatureInProgress,
                        actions = listOf(Action.ShowMainMenu),
                    ),
                nextState = UserState.Idle,
            )
        }

        val text =
            input.text
                ?: return HandlerResult(
                    response =
                        HandlerResponse(
                            message = Message.AddExpenseInstructions,
                            actions = listOf(Action.ShowMainMenu),
                        ),
                    nextState = UserState.AwaitingExpenseInput,
                )

        val parsedExpense =
            try {
                expenseService.parseExpense(text)
            } catch (e: BusinessException) {
                return HandlerResult(
                    response =
                        HandlerResponse(
                            message = Message.Error(e.errorCode),
                            actions = listOf(Action.ShowMainMenu),
                        ),
                    nextState = UserState.AwaitingExpenseInput,
                )
            }

        var categories = categoryService.getCategories(input.userId)
        if (categories.isEmpty()) {
            categoryService.initDefaultCategories(input.userId)
            categories = categoryService.getCategories(input.userId)
        }

        return HandlerResult(
            response =
                HandlerResponse(
                    message =
                        Message.SelectCategory(
                            amount = parsedExpense.amount,
                            description = parsedExpense.description,
                        ),
                    actions = listOf(Action.ShowCategorySelection(categories)),
                ),
            nextState = UserState.AwaitingCategorySelection(parsedExpense),
        )
    }
}
