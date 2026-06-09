package me.kmozze.expensetracker.handler.statehandler.expense.add

import me.kmozze.expensetracker.exception.BusinessErrorCode
import me.kmozze.expensetracker.handler.handlerResponse
import me.kmozze.expensetracker.handler.outgoingMessage
import me.kmozze.expensetracker.handler.statehandler.common.StateHandler
import me.kmozze.expensetracker.handler.statehandler.expense.toExpenseView
import me.kmozze.expensetracker.model.domain.bot.BotAction
import me.kmozze.expensetracker.model.domain.bot.BotText
import me.kmozze.expensetracker.model.domain.bot.HandlerResponse
import me.kmozze.expensetracker.model.domain.bot.UserCommand
import me.kmozze.expensetracker.model.domain.bot.UserInput
import me.kmozze.expensetracker.model.domain.bot.UserState
import me.kmozze.expensetracker.model.domain.expense.ExpenseDraftCategory
import me.kmozze.expensetracker.model.entity.Category
import me.kmozze.expensetracker.service.CategoryService
import org.springframework.stereotype.Component
import kotlin.reflect.KClass

@Component
class AwaitingCategorySelectionHandler(
    private val categoryService: CategoryService,
) : StateHandler {
    override val supportedStateClass: KClass<out UserState> = UserState.AwaitingCategorySelection::class

    override fun handle(
        input: UserInput,
        currentState: UserState,
    ): HandlerResponse {
        require(currentState is UserState.AwaitingCategorySelection) {
            "AwaitingCategorySelectionHandler requires AwaitingCategorySelection state"
        }

        return when (val command = input.command) {
            UserCommand.Cancel -> cancelExpenseCreation()
            is UserCommand.PlainText ->
                selectCategory(
                    input = input,
                    currentState = currentState,
                    categoryName = command.value,
                )
            UserCommand.Unsupported,
            UserCommand.Start,
            UserCommand.Menu,
            UserCommand.AddExpense,
            UserCommand.ViewExpenses,
            UserCommand.Categories,
            UserCommand.Statistics,
            UserCommand.FinishExpenseEdit,
            is UserCommand.RequestExpenseEdit,
            is UserCommand.RequestExpenseDeletion,
            is UserCommand.ConfirmExpenseDeletion,
            is UserCommand.CancelExpenseDeletion,
            is UserCommand.SelectExpenseDate,
            is UserCommand.SelectExpenseEditField,
            UserCommand.InvalidExpenseAction,
            -> repeatCategorySelection(input, currentState)
        }
    }

    private fun selectCategory(
        input: UserInput,
        currentState: UserState.AwaitingCategorySelection,
        categoryName: String,
    ): HandlerResponse {
        val categories = categoryService.getCategories(input.userId)
        val category =
            categories.firstOrNull { it.name == categoryName }
                ?: return categorySelectionError(
                    currentState = currentState,
                    categories = categories,
                    errorCode = BusinessErrorCode.CATEGORY_NOT_FOUND,
                )

        val expenseDraft =
            currentState.expenseDraft.copy(
                category = ExpenseDraftCategory(categoryId = category.id, name = category.name),
            )

        return handlerResponse(
            messages =
                listOf(
                    outgoingMessage(
                        text = expenseDraft.toExpenseView(),
                        actions = emptyList(),
                    ),
                    outgoingMessage(
                        text = BotText.SelectExpenseDate,
                        actions = listOf(BotAction.ShowExpenseDateSelection),
                    ),
                ),
            nextState =
                UserState.AwaitingExpenseDateSelection(
                    expenseDraft = expenseDraft,
                ),
        )
    }

    private fun cancelExpenseCreation(): HandlerResponse =
        handlerResponse(
            message =
                outgoingMessage(
                    text = BotText.ExpenseCanceled,
                    actions = listOf(BotAction.RemoveReplyKeyboard),
                ),
            nextState = UserState.Idle,
        )

    private fun repeatCategorySelection(
        input: UserInput,
        currentState: UserState.AwaitingCategorySelection,
    ): HandlerResponse {
        val categories = categoryService.getCategories(input.userId)

        return handlerResponse(
            messages =
                listOf(
                    outgoingMessage(
                        text = currentState.expenseDraft.toExpenseView(),
                        actions = emptyList(),
                    ),
                    outgoingMessage(
                        text = BotText.SelectCategory,
                        actions = listOf(BotAction.ShowCategorySelection(categories.map { it.name })),
                    ),
                ),
            nextState = currentState,
        )
    }

    private fun categorySelectionError(
        currentState: UserState.AwaitingCategorySelection,
        categories: List<Category>,
        errorCode: BusinessErrorCode,
    ): HandlerResponse =
        handlerResponse(
            message =
                outgoingMessage(
                    text = BotText.Error(errorCode),
                    actions = listOf(BotAction.ShowCategorySelection(categories.map { it.name })),
                ),
            nextState = currentState,
        )
}
