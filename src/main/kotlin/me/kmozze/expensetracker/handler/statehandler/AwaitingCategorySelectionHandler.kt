package me.kmozze.expensetracker.handler.statehandler

import me.kmozze.expensetracker.exception.BusinessErrorCode
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

        val callbackData = input.callbackData

        if (callbackData == CANCEL_CALLBACK) {
            return HandlerResult(
                response =
                    HandlerResponse(
                        message = Message.ExpenseCanceled,
                        actions = listOf(Action.ShowMainMenu),
                    ),
                nextState = UserState.Idle,
            )
        }

        if (callbackData == null || !callbackData.startsWith(SELECT_CATEGORY_PREFIX)) {
            val categories = categoryService.getCategories(input.userId)
            return HandlerResult(
                response =
                    HandlerResponse(
                        message =
                            Message.SelectCategory(
                                amount = currentState.parsedExpense.amount,
                                description = currentState.parsedExpense.description,
                            ),
                        actions = listOf(Action.ShowCategorySelection(categories)),
                    ),
                nextState = currentState,
            )
        }

        val categoryId =
            callbackData
                .removePrefix(SELECT_CATEGORY_PREFIX)
                .let { runCatching { UUID.fromString(it) }.getOrNull() }
                ?: return invalidCategorySelection(input.userId, currentState)

        val category =
            try {
                categoryService.getCategoryForUser(categoryId, input.userId)
            } catch (e: BusinessException) {
                if (e.errorCode == BusinessErrorCode.CATEGORY_NOT_FOUND) {
                    return categorySelectionError(
                        userId = input.userId,
                        currentState = currentState,
                        errorCode = BusinessErrorCode.CATEGORY_NOT_FOUND,
                    )
                }

                throw e
            }

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
                        Message.ExpenseSaved(
                            amount = expense.amount,
                            categoryName = category.name,
                            description = expense.description.orEmpty(),
                        ),
                    actions = listOf(Action.ShowMainMenu),
                ),
            nextState = UserState.Idle,
        )
    }

    private fun invalidCategorySelection(
        userId: Long,
        currentState: UserState.AwaitingCategorySelection,
    ): HandlerResult =
        categorySelectionError(
            userId = userId,
            currentState = currentState,
            errorCode = BusinessErrorCode.INVALID_CATEGORY_SELECTION,
        )

    private fun categorySelectionError(
        userId: Long,
        currentState: UserState.AwaitingCategorySelection,
        errorCode: BusinessErrorCode,
    ): HandlerResult {
        val categories = categoryService.getCategories(userId)

        return HandlerResult(
            response =
                HandlerResponse(
                    message = Message.Error(errorCode),
                    actions = listOf(Action.ShowCategorySelection(categories)),
                ),
            nextState = currentState,
        )
    }

    private companion object {
        const val SELECT_CATEGORY_PREFIX = "select_category:"
        const val CANCEL_CALLBACK = "cancel"
    }
}
