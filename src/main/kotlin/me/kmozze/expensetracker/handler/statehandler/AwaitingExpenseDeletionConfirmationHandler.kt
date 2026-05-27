package me.kmozze.expensetracker.handler.statehandler

import me.kmozze.expensetracker.model.domain.BotAction
import me.kmozze.expensetracker.model.domain.BotMessage
import me.kmozze.expensetracker.model.domain.HandlerResponse
import me.kmozze.expensetracker.model.domain.HandlerResult
import me.kmozze.expensetracker.model.domain.ResponseDelivery
import me.kmozze.expensetracker.model.domain.UserCommand
import me.kmozze.expensetracker.model.domain.UserInput
import me.kmozze.expensetracker.model.domain.UserState
import me.kmozze.expensetracker.service.CategoryService
import me.kmozze.expensetracker.service.ExpenseService
import org.springframework.stereotype.Component
import java.util.UUID
import kotlin.reflect.KClass

@Component
class AwaitingExpenseDeletionConfirmationHandler(
    private val expenseService: ExpenseService,
    private val categoryService: CategoryService,
) : StateHandler {
    override val supportedStateClass: KClass<out UserState> = UserState.AwaitingExpenseDeletionConfirmation::class

    override fun handle(
        input: UserInput,
        currentState: UserState,
    ): HandlerResult {
        require(currentState is UserState.AwaitingExpenseDeletionConfirmation) {
            "AwaitingExpenseDeletionConfirmationHandler requires AwaitingExpenseDeletionConfirmation state"
        }

        return when (val command = input.command) {
            is UserCommand.ConfirmExpenseDeletion -> confirmExpenseDeletion(input, currentState, command)
            is UserCommand.CancelExpenseDeletion -> cancelExpenseDeletion(input, currentState, command)
            is UserCommand.RequestExpenseDeletion -> repeatOrExpire(input, currentState, command.expenseId)
            UserCommand.InvalidExpenseDeletion -> staleExpenseDeletionCallback(input, currentState)
            UserCommand.Unsupported,
            UserCommand.Start,
            UserCommand.AddExpense,
            UserCommand.ViewExpenses,
            UserCommand.Categories,
            UserCommand.Statistics,
            UserCommand.Cancel,
            is UserCommand.SelectCategory,
            UserCommand.InvalidCategorySelection,
            is UserCommand.SelectExpenseDate,
            UserCommand.InvalidExpenseDateSelection,
            is UserCommand.PlainText,
            -> repeatExpenseDeletionConfirmation(input, currentState)
        }
    }

    private fun confirmExpenseDeletion(
        input: UserInput,
        currentState: UserState.AwaitingExpenseDeletionConfirmation,
        command: UserCommand.ConfirmExpenseDeletion,
    ): HandlerResult {
        if (command.expenseId != currentState.expenseId) {
            return staleExpenseDeletionCallback(input, currentState)
        }

        val delivery = responseDeliveryForExpenseCard(input.callbackMessageId ?: currentState.cardMessageId)
        val deleted = expenseService.deleteExpenseForUser(input.userId, currentState.expenseId)
        val message =
            if (deleted) {
                BotMessage.ExpenseDeleted
            } else {
                BotMessage.ExpenseUnavailable
            }

        return HandlerResult(
            response =
                HandlerResponse(
                    message = message,
                    actions = actionsForCompletedExpenseCard(delivery),
                    delivery = delivery,
                ),
            nextState = UserState.Idle,
        )
    }

    private fun cancelExpenseDeletion(
        input: UserInput,
        currentState: UserState.AwaitingExpenseDeletionConfirmation,
        command: UserCommand.CancelExpenseDeletion,
    ): HandlerResult {
        if (command.expenseId != currentState.expenseId) {
            return staleExpenseDeletionCallback(input, currentState)
        }

        val delivery = responseDeliveryForExpenseCard(input.callbackMessageId ?: currentState.cardMessageId)
        val message =
            savedExpenseMessageForUser(
                input = input,
                expenseId = currentState.expenseId,
                showDeletionConfirmation = false,
            ) ?: return expenseUnavailable(delivery)

        return HandlerResult(
            response =
                HandlerResponse(
                    message = message,
                    actions = actionsForSavedExpenseCard(currentState.expenseId),
                    delivery = delivery,
                ),
            nextState = UserState.Idle,
        )
    }

    private fun repeatOrExpire(
        input: UserInput,
        currentState: UserState.AwaitingExpenseDeletionConfirmation,
        expenseId: UUID,
    ): HandlerResult =
        if (expenseId == currentState.expenseId) {
            repeatExpenseDeletionConfirmation(input, currentState)
        } else {
            staleExpenseDeletionCallback(input, currentState)
        }

    private fun repeatExpenseDeletionConfirmation(
        input: UserInput,
        currentState: UserState.AwaitingExpenseDeletionConfirmation,
    ): HandlerResult {
        val delivery = responseDeliveryForExpenseCard(input.callbackMessageId ?: currentState.cardMessageId)
        val message =
            savedExpenseMessageForUser(
                input = input,
                expenseId = currentState.expenseId,
                showDeletionConfirmation = true,
            ) ?: return expenseUnavailable(delivery)

        return HandlerResult(
            response =
                HandlerResponse(
                    message = message,
                    actions = listOf(BotAction.ShowExpenseDeletionConfirmation(currentState.expenseId)),
                    delivery = delivery,
                ),
            nextState = currentState,
        )
    }

    private fun staleExpenseDeletionCallback(
        input: UserInput,
        currentState: UserState.AwaitingExpenseDeletionConfirmation,
    ): HandlerResult {
        val delivery = responseDeliveryForExpenseCard(input.callbackMessageId)

        return HandlerResult(
            response =
                HandlerResponse(
                    message = BotMessage.SelectionExpired,
                    actions = actionsForCompletedExpenseCard(delivery),
                    delivery = delivery,
                ),
            nextState = currentState,
        )
    }

    private fun savedExpenseMessageForUser(
        input: UserInput,
        expenseId: UUID,
        showDeletionConfirmation: Boolean,
    ): BotMessage.ExpenseSaved? {
        val expense =
            expenseService.findExpenseForUser(input.userId, expenseId)
                ?: return null
        val category =
            categoryService.findCategoryForUser(expense.categoryId, input.userId)
                ?: return null

        return savedExpenseMessage(
            expense = expense,
            categoryName = category.name,
            showDeletionConfirmation = showDeletionConfirmation,
        )
    }

    private fun expenseUnavailable(delivery: ResponseDelivery): HandlerResult =
        HandlerResult(
            response =
                HandlerResponse(
                    message = BotMessage.ExpenseUnavailable,
                    actions = actionsForCompletedExpenseCard(delivery),
                    delivery = delivery,
                ),
            nextState = UserState.Idle,
        )
}
