package me.kmozze.expensetracker.handler.statehandler

import me.kmozze.expensetracker.exception.BusinessErrorCode
import me.kmozze.expensetracker.model.domain.BotAction
import me.kmozze.expensetracker.model.domain.BotMessage
import me.kmozze.expensetracker.model.domain.HandlerResponse
import me.kmozze.expensetracker.model.domain.HandlerResult
import me.kmozze.expensetracker.model.domain.UserCommand
import me.kmozze.expensetracker.model.domain.UserInput
import me.kmozze.expensetracker.model.domain.UserState
import me.kmozze.expensetracker.model.domain.messageIdOrNull
import me.kmozze.expensetracker.service.CategoryService
import org.springframework.stereotype.Component
import java.util.UUID
import kotlin.reflect.KClass

@Component
class AwaitingCategorySelectionHandler(
    private val categoryService: CategoryService,
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
            UserCommand.Cancel -> cancelExpenseCreation(input)
            is UserCommand.SelectCategory ->
                selectCategory(
                    input = input,
                    currentState = currentState,
                    categoryId = command.categoryId,
                )
            UserCommand.InvalidCategorySelection ->
                categorySelectionError(
                    input = input,
                    currentState = currentState,
                    errorCode = BusinessErrorCode.INVALID_CATEGORY_SELECTION,
                )
            UserCommand.Unsupported,
            UserCommand.Start,
            UserCommand.AddExpense,
            UserCommand.ViewExpenses,
            UserCommand.Categories,
            UserCommand.Statistics,
            is UserCommand.SelectExpenseDate,
            UserCommand.InvalidExpenseDateSelection,
            is UserCommand.RequestExpenseDeletion,
            is UserCommand.ConfirmExpenseDeletion,
            is UserCommand.CancelExpenseDeletion,
            UserCommand.InvalidExpenseDeletion,
            is UserCommand.PlainText,
            -> repeatCategorySelection(input, currentState)
        }
    }

    private fun selectCategory(
        input: UserInput,
        currentState: UserState.AwaitingCategorySelection,
        categoryId: UUID,
    ): HandlerResult {
        val category =
            categoryService.findCategoryForUser(categoryId, input.userId)
                ?: return categorySelectionError(
                    input = input,
                    currentState = currentState,
                    errorCode = BusinessErrorCode.CATEGORY_NOT_FOUND,
                )

        val expenseDraft =
            currentState.expenseDraft.copy(
                categoryId = category.id,
                categoryName = category.name,
            )
        val delivery = responseDeliveryForExpenseCard(input.callbackMessageId)

        return HandlerResult(
            response =
                HandlerResponse(
                    message =
                        BotMessage.SelectExpenseDate(
                            amount = expenseDraft.amount,
                            categoryName = category.name,
                            description = expenseDraft.description,
                        ),
                    actions = listOf(BotAction.ShowExpenseDateSelection),
                    delivery = delivery,
                ),
            nextState =
                UserState.AwaitingExpenseDateSelection(
                    expenseDraft = expenseDraft,
                    cardMessageId = delivery.messageIdOrNull(),
                ),
        )
    }

    private fun cancelExpenseCreation(input: UserInput): HandlerResult {
        val delivery = responseDeliveryForExpenseCard(input.callbackMessageId)

        return HandlerResult(
            response =
                HandlerResponse(
                    message = BotMessage.ExpenseCanceled,
                    actions = actionsForCompletedExpenseCard(delivery),
                    delivery = delivery,
                ),
            nextState = UserState.Idle,
        )
    }

    private fun repeatCategorySelection(
        input: UserInput,
        currentState: UserState.AwaitingCategorySelection,
    ): HandlerResult {
        val categories = categoryService.getCategories(input.userId)
        val delivery = responseDeliveryForExpenseCard(input.callbackMessageId)

        return HandlerResult(
            response =
                HandlerResponse(
                    message =
                        BotMessage.SelectCategory(
                            amount = currentState.expenseDraft.amount,
                            description = currentState.expenseDraft.description,
                        ),
                    actions = listOf(BotAction.ShowCategorySelection(categories)),
                    delivery = delivery,
                ),
            nextState = currentState,
        )
    }

    private fun categorySelectionError(
        input: UserInput,
        currentState: UserState.AwaitingCategorySelection,
        errorCode: BusinessErrorCode,
    ): HandlerResult {
        val categories = categoryService.getCategories(input.userId)
        val delivery = responseDeliveryForExpenseCard(input.callbackMessageId)

        return HandlerResult(
            response =
                HandlerResponse(
                    message = BotMessage.Error(errorCode),
                    actions = listOf(BotAction.ShowCategorySelection(categories)),
                    delivery = delivery,
                ),
            nextState = currentState,
        )
    }
}
