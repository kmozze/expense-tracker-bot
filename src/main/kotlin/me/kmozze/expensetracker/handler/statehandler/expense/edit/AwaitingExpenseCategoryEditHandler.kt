package me.kmozze.expensetracker.handler.statehandler.expense.edit

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
                    expenseId = currentState.editSession.expenseId,
                )

            UserCommand.FinishExpenseEdit ->
                finishExpenseEdit(
                    expenseService = expenseService,
                    categoryService = categoryService,
                    userId = input.userId,
                    editSession = currentState.editSession,
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

        return buildDraftExpenseEditFieldSelectionResult(
            editSession =
                currentState.editSession.withExpenseDraft(
                    currentState.editSession.expenseDraft.copy(
                        category = ExpenseDraftCategory(categoryId = category.id, name = category.name),
                    ),
                ),
        )
    }

    private fun repeatCategorySelection(
        input: UserInput,
        currentState: UserState.AwaitingExpenseCategoryEdit,
    ): HandlerResponse {
        val categories = categoryService.getCategories(input.userId)

        if (categories.isEmpty()) {
            return handlerResponse(
                message =
                    outgoingMessage(
                        text = BotText.NoCategories,
                        actions = listOf(BotAction.ShowMainMenu),
                    ),
                nextState = UserState.Idle,
            )
        }

        return handlerResponse(
            messages =
                listOf(
                    outgoingMessage(
                        text = currentState.editSession.expenseDraft.toExpenseView(),
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
        currentState: UserState.AwaitingExpenseCategoryEdit,
        categories: List<Category>,
    ): HandlerResponse =
        if (categories.isEmpty()) {
            handlerResponse(
                message =
                    outgoingMessage(
                        text = BotText.NoCategories,
                        actions = listOf(BotAction.ShowMainMenu),
                    ),
                nextState = UserState.Idle,
            )
        } else {
            handlerResponse(
                message =
                    outgoingMessage(
                        text = BotText.Error(BusinessErrorCode.CATEGORY_NOT_FOUND),
                        actions = listOf(BotAction.ShowCategorySelection(categories.map { it.name })),
                    ),
                nextState = currentState,
            )
        }
}
