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
import me.kmozze.expensetracker.service.ExpenseService
import org.springframework.stereotype.Component
import kotlin.reflect.KClass

@Component
class AwaitingExpenseCategoryEditHandler(
    private val expenseService: ExpenseService,
    private val categoryService: CategoryService,
) : StateHandler {
    override val supportedStateClass: KClass<out UserState> = UserState.AwaitingExpenseCategoryEdit::class

    override fun handle(
        input: UserInput,
        currentState: UserState,
    ): HandlerResponse {
        require(currentState is UserState.AwaitingExpenseCategoryEdit) {
            "AwaitingExpenseCategoryEditHandler requires AwaitingExpenseCategoryEdit state"
        }

        return when (val command = input.command) {
            UserCommand.Cancel ->
                cancelExpenseEdit(
                    expenseService = expenseService,
                    categoryService = categoryService,
                    userId = input.userId,
                    expenseId = currentState.expenseId,
                )

            is UserCommand.PlainText -> selectCategory(input, currentState, command.value)
            else -> repeatCategorySelection(input, currentState)
        }
    }

    private fun selectCategory(
        input: UserInput,
        currentState: UserState.AwaitingExpenseCategoryEdit,
        categoryName: String,
    ): HandlerResponse {
        val categories = categoryService.getCategories(input.userId)
        val category =
            categories.firstOrNull { it.name == categoryName }
                ?: return categorySelectionError(
                    currentState = currentState,
                    categories = categories,
                )

        val updatedExpense = expenseService.updateExpenseCategoryForUser(input.userId, currentState.expenseId, category.id)
        if (updatedExpense == null) {
            return expenseEditUnavailableResult()
        }

        return buildUpdatedExpenseResult(
            expenseService = expenseService,
            categoryService = categoryService,
            userId = input.userId,
            expenseId = updatedExpense.id,
            nextState = UserState.Idle,
            prefixText = BotText.ExpenseSaved,
        )
    }

    private fun repeatCategorySelection(
        input: UserInput,
        currentState: UserState.AwaitingExpenseCategoryEdit,
    ): HandlerResponse {
        val categories = categoryService.getCategories(input.userId)

        if (categories.isEmpty()) {
            return HandlerResponse(
                outgoingMessages =
                    listOf(
                        OutgoingMessage(
                            text = BotText.NoCategories,
                            actions = listOf(BotAction.ShowMainMenu),
                        ),
                    ),
                nextState = UserState.Idle,
            )
        }

        val expense =
            expenseService.findExpenseForUser(
                userId = input.userId,
                expenseId = currentState.expenseId,
            ) ?: return expenseEditUnavailableResult()

        val category =
            categoryService.findCategoryForUser(
                categoryId = expense.categoryId,
                userId = input.userId,
            ) ?: return expenseEditUnavailableResult()

        return HandlerResponse(
            outgoingMessages =
                listOf(
                    OutgoingMessage(
                        text = expense.toExpenseView(category),
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
        currentState: UserState.AwaitingExpenseCategoryEdit,
        categories: List<Category>,
    ): HandlerResponse =
        if (categories.isEmpty()) {
            HandlerResponse(
                outgoingMessages =
                    listOf(
                        OutgoingMessage(
                            text = BotText.NoCategories,
                            actions = listOf(BotAction.ShowMainMenu),
                        ),
                    ),
                nextState = UserState.Idle,
            )
        } else {
            HandlerResponse(
                outgoingMessages =
                    listOf(
                        OutgoingMessage(
                            text = BotText.Error(BusinessErrorCode.CATEGORY_NOT_FOUND),
                            actions = listOf(BotAction.ShowCategorySelection(categories.map { it.name })),
                        ),
                    ),
                nextState = currentState,
            )
        }
}
