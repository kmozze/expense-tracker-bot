package me.kmozze.expensetracker.handler.statehandler

import me.kmozze.expensetracker.exception.BusinessErrorCode
import me.kmozze.expensetracker.model.domain.BotAction
import me.kmozze.expensetracker.model.domain.BotText
import me.kmozze.expensetracker.model.domain.HandlerResponse
import me.kmozze.expensetracker.model.domain.OutgoingMessage
import me.kmozze.expensetracker.model.domain.UserCommand
import me.kmozze.expensetracker.model.domain.UserInput
import me.kmozze.expensetracker.model.domain.UserState
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

        val expenseDraft = currentState.expenseDraft.copy(categoryId = category.id)

        return HandlerResponse(
            outgoingMessages =
                listOf(
                    OutgoingMessage(
                        text = expenseDraft.toExpenseView(categoryName = category.name),
                        actions = emptyList(),
                    ),
                    OutgoingMessage(
                        text = BotText.SelectExpenseDate,
                        actions = listOf(BotAction.ShowExpenseDateSelection),
                    ),
                ),
            nextState =
                UserState.AwaitingExpenseDateSelection(
                    expenseDraft = expenseDraft,
                    categoryName = category.name,
                ),
        )
    }

    private fun cancelExpenseCreation(): HandlerResponse =
        HandlerResponse(
            outgoingMessages =
                listOf(
                    OutgoingMessage(
                        text = BotText.ExpenseCanceled,
                        actions = listOf(BotAction.RemoveReplyKeyboard),
                    ),
                ),
            nextState = UserState.Idle,
        )

    private fun repeatCategorySelection(
        input: UserInput,
        currentState: UserState.AwaitingCategorySelection,
    ): HandlerResponse {
        val categories = categoryService.getCategories(input.userId)

        return HandlerResponse(
            outgoingMessages =
                listOf(
                    OutgoingMessage(
                        text = currentState.expenseDraft.toExpenseView(),
                        actions = emptyList(),
                    ),
                    OutgoingMessage(
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
        HandlerResponse(
            outgoingMessages =
                listOf(
                    OutgoingMessage(
                        text = BotText.Error(errorCode),
                        actions = listOf(BotAction.ShowCategorySelection(categories.map { it.name })),
                    ),
                ),
            nextState = currentState,
        )
}
