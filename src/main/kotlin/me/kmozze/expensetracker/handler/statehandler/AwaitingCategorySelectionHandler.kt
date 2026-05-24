package me.kmozze.expensetracker.handler.statehandler

import me.kmozze.expensetracker.exception.BusinessErrorCode
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
import java.util.UUID
import kotlin.reflect.KClass

@Component
class AwaitingCategorySelectionHandler(
    private val categoryService: CategoryService,
    private val expenseService: ExpenseService,
) : StateHandler {
    override val supportedStateClass: KClass<out UserState> = UserState.AwaitingCategorySelection::class

    override fun handle(
        input: UserInput,
        currentState: UserState,
    ): HandlerResult {
        require(currentState is UserState.AwaitingCategorySelection) {
            "AwaitingCategorySelectionHandler requires AwaitingCategorySelection state"
        }

        return when (val command = input.command) {
            UserCommand.Cancel -> cancelExpenseCreation()
            is UserCommand.SelectCategory ->
                saveExpense(
                    input = input,
                    currentState = currentState,
                    categoryId = command.categoryId,
                )
            UserCommand.InvalidCategorySelection ->
                categorySelectionError(
                    userId = input.userId,
                    currentState = currentState,
                    errorCode = BusinessErrorCode.INVALID_CATEGORY_SELECTION,
                )
            UserCommand.Unsupported,
            UserCommand.Start,
            UserCommand.AddExpense,
            UserCommand.ViewExpenses,
            UserCommand.Categories,
            UserCommand.Statistics,
            is UserCommand.PlainText,
            -> repeatCategorySelection(input.userId, currentState)
        }
    }

    private fun saveExpense(
        input: UserInput,
        currentState: UserState.AwaitingCategorySelection,
        categoryId: UUID,
    ): HandlerResult {
        val category =
            categoryService.findCategoryForUser(categoryId, input.userId)
                ?: return categorySelectionError(
                    userId = input.userId,
                    currentState = currentState,
                    errorCode = BusinessErrorCode.CATEGORY_NOT_FOUND,
                )

        val expense =
            expenseService.saveExpense(
                userId = input.userId,
                categoryId = category.id,
                parsedExpense = currentState.parsedExpense,
            )

        return HandlerResult(
            response =
                HandlerResponse(
                    message =
                        BotMessage.ExpenseSaved(
                            amount = expense.amount,
                            categoryName = category.name,
                            expenseDate = expense.expenseDate,
                            description = expense.description,
                        ),
                    actions = listOf(BotAction.ShowMainMenu),
                ),
            nextState = UserState.Idle,
        )
    }

    private fun cancelExpenseCreation(): HandlerResult =
        HandlerResult(
            response =
                HandlerResponse(
                    message = BotMessage.ExpenseCanceled,
                    actions = listOf(BotAction.ShowMainMenu),
                ),
            nextState = UserState.Idle,
        )

    private fun repeatCategorySelection(
        userId: Long,
        currentState: UserState.AwaitingCategorySelection,
    ): HandlerResult {
        val categories = categoryService.getCategories(userId)

        return HandlerResult(
            response =
                HandlerResponse(
                    message =
                        BotMessage.SelectCategory(
                            amount = currentState.parsedExpense.amount,
                            description = currentState.parsedExpense.description,
                        ),
                    actions = listOf(BotAction.ShowCategorySelection(categories)),
                ),
            nextState = currentState,
        )
    }

    private fun categorySelectionError(
        userId: Long,
        currentState: UserState.AwaitingCategorySelection,
        errorCode: BusinessErrorCode,
    ): HandlerResult {
        val categories = categoryService.getCategories(userId)

        return HandlerResult(
            response =
                HandlerResponse(
                    message = BotMessage.Error(errorCode),
                    actions = listOf(BotAction.ShowCategorySelection(categories)),
                ),
            nextState = currentState,
        )
    }
}
