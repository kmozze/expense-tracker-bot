package me.kmozze.expensetracker.handler.statehandler

import me.kmozze.expensetracker.exception.BusinessErrorCode
import me.kmozze.expensetracker.model.domain.BotAction
import me.kmozze.expensetracker.model.domain.BotText
import me.kmozze.expensetracker.model.domain.HandlerResponse
import me.kmozze.expensetracker.model.domain.HandlerResult
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
    ): HandlerResult {
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
    ): HandlerResult {
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
            prefixText = BotText.Done,
        )
    }

    private fun repeatCategorySelection(
        input: UserInput,
        currentState: UserState.AwaitingExpenseCategoryEdit,
    ): HandlerResult {
        val categories = categoryService.getCategories(input.userId)

        if (categories.isEmpty()) {
            return HandlerResult(
                response =
                    HandlerResponse(
                        outgoingMessages =
                            listOf(
                                OutgoingMessage(
                                    text = BotText.NoCategories,
                                    actions = listOf(BotAction.ShowMainMenu),
                                ),
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

        return HandlerResult(
            response =
                HandlerResponse(
                    outgoingMessages =
                        listOf(
                            OutgoingMessage(
                                text =
                                    BotText.SelectCategory(
                                        amount = expense.amount,
                                        description = expense.description,
                                    ),
                                actions = listOf(BotAction.ShowCategorySelection(categories)),
                            ),
                        ),
                ),
            nextState = currentState,
        )
    }

    private fun categorySelectionError(
        currentState: UserState.AwaitingExpenseCategoryEdit,
        categories: List<Category>,
    ): HandlerResult =
        if (categories.isEmpty()) {
            HandlerResult(
                response =
                    HandlerResponse(
                        outgoingMessages =
                            listOf(
                                OutgoingMessage(
                                    text = BotText.NoCategories,
                                    actions = listOf(BotAction.ShowMainMenu),
                                ),
                            ),
                    ),
                nextState = UserState.Idle,
            )
        } else {
            HandlerResult(
                response =
                    HandlerResponse(
                        outgoingMessages =
                            listOf(
                                OutgoingMessage(
                                    text = BotText.Error(BusinessErrorCode.CATEGORY_NOT_FOUND),
                                    actions = listOf(BotAction.ShowCategorySelection(categories)),
                                ),
                            ),
                    ),
                nextState = currentState,
            )
        }
}
