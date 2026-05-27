package me.kmozze.expensetracker.handler.statehandler

import me.kmozze.expensetracker.exception.BusinessErrorCode
import me.kmozze.expensetracker.model.domain.BotAction
import me.kmozze.expensetracker.model.domain.BotMessage
import me.kmozze.expensetracker.model.domain.ExpenseDateSelection
import me.kmozze.expensetracker.model.domain.HandlerResponse
import me.kmozze.expensetracker.model.domain.HandlerResult
import me.kmozze.expensetracker.model.domain.ResponseDelivery
import me.kmozze.expensetracker.model.domain.UserCommand
import me.kmozze.expensetracker.model.domain.UserInput
import me.kmozze.expensetracker.model.domain.UserState
import me.kmozze.expensetracker.model.domain.messageIdOrNull
import me.kmozze.expensetracker.service.ExpenseService
import org.springframework.stereotype.Component
import java.time.Clock
import java.time.LocalDate
import java.util.UUID
import kotlin.reflect.KClass

@Component
class AwaitingExpenseDateSelectionHandler(
    private val expenseService: ExpenseService,
    private val clock: Clock,
) : StateHandler {
    override val supportedStateClass: KClass<out UserState> = UserState.AwaitingExpenseDateSelection::class

    override fun handle(
        input: UserInput,
        currentState: UserState,
    ): HandlerResult {
        require(currentState is UserState.AwaitingExpenseDateSelection) {
            "AwaitingExpenseDateSelectionHandler requires AwaitingExpenseDateSelection state"
        }

        return when (val command = input.command) {
            UserCommand.Cancel -> cancelExpenseCreation(input, currentState)
            is UserCommand.SelectExpenseDate -> handleExpenseDateSelection(input, currentState, command.selection)
            UserCommand.InvalidExpenseDateSelection ->
                expenseDateSelectionError(
                    input = input,
                    currentState = currentState,
                    errorCode = BusinessErrorCode.INVALID_EXPENSE_DATE_SELECTION,
                )
            UserCommand.Unsupported,
            UserCommand.Start,
            UserCommand.AddExpense,
            UserCommand.ViewExpenses,
            UserCommand.Categories,
            UserCommand.Statistics,
            is UserCommand.SelectCategory,
            UserCommand.InvalidCategorySelection,
            is UserCommand.RequestExpenseDeletion,
            is UserCommand.ConfirmExpenseDeletion,
            is UserCommand.CancelExpenseDeletion,
            UserCommand.InvalidExpenseDeletion,
            is UserCommand.PlainText,
            -> repeatExpenseDateSelection(input, currentState)
        }
    }

    private fun handleExpenseDateSelection(
        input: UserInput,
        currentState: UserState.AwaitingExpenseDateSelection,
        selection: ExpenseDateSelection,
    ): HandlerResult =
        when (selection) {
            ExpenseDateSelection.TODAY -> saveExpenseWithDate(input, currentState, LocalDate.now(clock))
            ExpenseDateSelection.YESTERDAY -> saveExpenseWithDate(input, currentState, LocalDate.now(clock).minusDays(1))
            ExpenseDateSelection.MANUAL -> requestManualDateInput(input, currentState)
        }

    private fun saveExpenseWithDate(
        input: UserInput,
        currentState: UserState.AwaitingExpenseDateSelection,
        expenseDate: LocalDate,
    ): HandlerResult {
        val expenseDraft = currentState.expenseDraft.copy(expenseDate = expenseDate)
        val expense = expenseService.saveExpense(input.userId, expenseDraft)
        val delivery = responseDeliveryForExpenseCard(input.callbackMessageId)

        return expenseSavedResult(
            message =
                savedExpenseMessage(
                    expense = expense,
                    categoryName = expenseDraft.requireCategoryName(),
                ),
            expenseId = expense.id,
            delivery = delivery,
        )
    }

    private fun requestManualDateInput(
        input: UserInput,
        currentState: UserState.AwaitingExpenseDateSelection,
    ): HandlerResult {
        val delivery = responseDeliveryForExpenseCard(input.callbackMessageId)

        return HandlerResult(
            response =
                HandlerResponse(
                    message = currentState.toEnterExpenseDateManuallyMessage(),
                    actions = listOf(BotAction.ShowCancel),
                    delivery = delivery,
                ),
            nextState =
                UserState.AwaitingExpenseManualDateInput(
                    expenseDraft = currentState.expenseDraft,
                    cardMessageId = delivery.messageIdOrNull(),
                ),
        )
    }

    private fun repeatExpenseDateSelection(
        input: UserInput,
        currentState: UserState.AwaitingExpenseDateSelection,
    ): HandlerResult {
        val delivery = responseDeliveryForExpenseCard(input.callbackMessageId ?: currentState.cardMessageId)

        return HandlerResult(
            response =
                HandlerResponse(
                    message = currentState.toSelectExpenseDateMessage(),
                    actions = listOf(BotAction.ShowExpenseDateSelection),
                    delivery = delivery,
                ),
            nextState = currentState,
        )
    }

    private fun expenseDateSelectionError(
        input: UserInput,
        currentState: UserState.AwaitingExpenseDateSelection,
        errorCode: BusinessErrorCode,
    ): HandlerResult {
        val delivery = responseDeliveryForExpenseCard(input.callbackMessageId)

        return HandlerResult(
            response =
                HandlerResponse(
                    message = BotMessage.Error(errorCode),
                    actions = listOf(BotAction.ShowExpenseDateSelection),
                    delivery = delivery,
                ),
            nextState = currentState,
        )
    }

    private fun cancelExpenseCreation(
        input: UserInput,
        currentState: UserState.AwaitingExpenseDateSelection,
    ): HandlerResult {
        val delivery = responseDeliveryForExpenseCard(input.callbackMessageId ?: currentState.cardMessageId)

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

    private fun UserState.AwaitingExpenseDateSelection.toSelectExpenseDateMessage(): BotMessage.SelectExpenseDate =
        BotMessage.SelectExpenseDate(
            amount = expenseDraft.amount,
            categoryName = expenseDraft.requireCategoryName(),
            description = expenseDraft.description,
        )

    private fun UserState.AwaitingExpenseDateSelection.toEnterExpenseDateManuallyMessage(): BotMessage.EnterExpenseDateManually =
        BotMessage.EnterExpenseDateManually(
            amount = expenseDraft.amount,
            categoryName = expenseDraft.requireCategoryName(),
            description = expenseDraft.description,
        )

    private fun expenseSavedResult(
        message: BotMessage.ExpenseSaved,
        expenseId: UUID,
        delivery: ResponseDelivery,
    ): HandlerResult =
        HandlerResult(
            response =
                HandlerResponse(
                    message = message,
                    actions = actionsForSavedExpenseCard(expenseId),
                    delivery = delivery,
                ),
            nextState = UserState.Idle,
        )
}
