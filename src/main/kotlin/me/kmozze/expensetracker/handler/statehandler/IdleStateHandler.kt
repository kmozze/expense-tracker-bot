package me.kmozze.expensetracker.handler.statehandler

import me.kmozze.expensetracker.model.domain.BotAction
import me.kmozze.expensetracker.model.domain.BotMessage
import me.kmozze.expensetracker.model.domain.HandlerResponse
import me.kmozze.expensetracker.model.domain.HandlerResult
import me.kmozze.expensetracker.model.domain.UserCommand
import me.kmozze.expensetracker.model.domain.UserInput
import me.kmozze.expensetracker.model.domain.UserState
import me.kmozze.expensetracker.model.domain.messageIdOrNull
import me.kmozze.expensetracker.service.CategoryService
import me.kmozze.expensetracker.service.ExpenseService
import org.springframework.stereotype.Component
import java.util.UUID
import kotlin.reflect.KClass

@Component
class IdleStateHandler(
    private val expenseService: ExpenseService,
    private val categoryService: CategoryService,
) : StateHandler {
    override val supportedStateClass: KClass<out UserState> = UserState.Idle::class

    override fun handle(
        input: UserInput,
        currentState: UserState,
    ): HandlerResult {
        require(currentState is UserState.Idle) {
            "IdleStateHandler requires Idle state"
        }

        return when (val command = input.command) {
            UserCommand.AddExpense ->
                HandlerResult(
                    response =
                        HandlerResponse(
                            message = BotMessage.AddExpenseInstructions,
                            actions = listOf(BotAction.ShowMainMenu),
                        ),
                    nextState = UserState.AwaitingExpenseInput,
                )

            UserCommand.ViewExpenses,
            UserCommand.Categories,
            UserCommand.Statistics,
            ->
                HandlerResult(
                    response =
                        HandlerResponse(
                            message = BotMessage.FeatureInProgress,
                            actions = listOf(BotAction.ShowMainMenu),
                        ),
                    nextState = UserState.Idle,
                )

            is UserCommand.RequestExpenseDeletion ->
                requestExpenseDeletion(input, command.expenseId)

            UserCommand.Cancel,
            is UserCommand.SelectCategory,
            UserCommand.InvalidCategorySelection,
            is UserCommand.SelectExpenseDate,
            UserCommand.InvalidExpenseDateSelection,
            is UserCommand.ConfirmExpenseDeletion,
            is UserCommand.CancelExpenseDeletion,
            UserCommand.InvalidExpenseDeletion,
            ->
                HandlerResult(
                    response =
                        HandlerResponse(
                            message = BotMessage.SelectionExpired,
                            actions = listOf(BotAction.ShowMainMenu),
                        ),
                    nextState = UserState.Idle,
                )

            else ->
                HandlerResult(
                    response =
                        HandlerResponse(
                            message = BotMessage.UnknownCommand,
                            actions = listOf(BotAction.ShowMainMenu),
                        ),
                    nextState = UserState.Idle,
                )
        }
    }

    private fun requestExpenseDeletion(
        input: UserInput,
        expenseId: UUID,
    ): HandlerResult {
        val expense =
            expenseService.findExpenseForUser(input.userId, expenseId)
                ?: return expenseUnavailable(input)
        val category =
            categoryService.findCategoryForUser(expense.categoryId, input.userId)
                ?: return expenseUnavailable(input)
        val delivery = responseDeliveryForExpenseCard(input.callbackMessageId)

        return HandlerResult(
            response =
                HandlerResponse(
                    message =
                        savedExpenseMessage(
                            expense = expense,
                            categoryName = category.name,
                            showDeletionConfirmation = true,
                        ),
                    actions = listOf(BotAction.ShowExpenseDeletionConfirmation(expenseId)),
                    delivery = delivery,
                ),
            nextState =
                UserState.AwaitingExpenseDeletionConfirmation(
                    expenseId = expenseId,
                    cardMessageId = delivery.messageIdOrNull(),
                ),
        )
    }

    private fun expenseUnavailable(input: UserInput): HandlerResult {
        val delivery = responseDeliveryForExpenseCard(input.callbackMessageId)

        return HandlerResult(
            response =
                HandlerResponse(
                    message = BotMessage.ExpenseUnavailable,
                    actions = actionsForCompletedExpenseCard(delivery),
                    delivery = delivery,
                ),
            nextState = UserState.Idle,
        )
    }
}
