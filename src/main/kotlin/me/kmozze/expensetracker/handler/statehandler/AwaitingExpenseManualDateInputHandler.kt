package me.kmozze.expensetracker.handler.statehandler

import me.kmozze.expensetracker.exception.BusinessErrorCode
import me.kmozze.expensetracker.exception.BusinessException
import me.kmozze.expensetracker.exception.ErrorCode
import me.kmozze.expensetracker.model.domain.BotAction
import me.kmozze.expensetracker.model.domain.BotText
import me.kmozze.expensetracker.model.domain.ExpenseDateSelection
import me.kmozze.expensetracker.model.domain.ExpenseDraft
import me.kmozze.expensetracker.model.domain.HandlerResponse
import me.kmozze.expensetracker.model.domain.HandlerResult
import me.kmozze.expensetracker.model.domain.ResponseDelivery
import me.kmozze.expensetracker.model.domain.UserCommand
import me.kmozze.expensetracker.model.domain.UserInput
import me.kmozze.expensetracker.model.domain.UserState
import me.kmozze.expensetracker.service.ExpenseService
import org.springframework.stereotype.Component
import java.time.Clock
import java.time.LocalDate
import kotlin.reflect.KClass

@Component
class AwaitingExpenseManualDateInputHandler(
    private val expenseService: ExpenseService,
    private val clock: Clock,
) : StateHandler {
    override val supportedStateClass: KClass<out UserState> = UserState.AwaitingExpenseManualDateInput::class

    override fun handle(
        input: UserInput,
        currentState: UserState,
    ): HandlerResult {
        require(currentState is UserState.AwaitingExpenseManualDateInput) {
            "AwaitingExpenseManualDateInputHandler requires AwaitingExpenseManualDateInput state"
        }

        return when (val command = input.command) {
            UserCommand.Cancel -> cancelExpenseCreation(input, currentState)
            is UserCommand.PlainText -> saveExpenseWithManualDate(input, currentState, command.value)
            is UserCommand.SelectExpenseDate -> handleExpenseDateSelection(input, currentState, command.selection)
            UserCommand.InvalidExpenseDateSelection ->
                manualDateInputError(
                    input = input,
                    currentState = currentState,
                    errorCode = BusinessErrorCode.INVALID_EXPENSE_DATE_SELECTION,
                )
            UserCommand.Unsupported,
            UserCommand.Start,
            UserCommand.Menu,
            UserCommand.AddExpense,
            UserCommand.ViewExpenses,
            UserCommand.Categories,
            UserCommand.Statistics,
            is UserCommand.SelectCategory,
            UserCommand.InvalidCategorySelection,
            -> repeatManualDateInput(input, currentState)
        }
    }

    private fun saveExpenseWithManualDate(
        input: UserInput,
        currentState: UserState.AwaitingExpenseManualDateInput,
        dateText: String,
    ): HandlerResult {
        val expenseDate =
            try {
                expenseService.parseExpenseDate(dateText)
            } catch (e: BusinessException) {
                return manualDateInputError(input, currentState, e.errorCode)
            }

        return saveExpenseWithDate(input, currentState, expenseDate)
    }

    private fun handleExpenseDateSelection(
        input: UserInput,
        currentState: UserState.AwaitingExpenseManualDateInput,
        selection: ExpenseDateSelection,
    ): HandlerResult =
        when (selection) {
            ExpenseDateSelection.TODAY -> saveExpenseWithDate(input, currentState, LocalDate.now(clock))
            ExpenseDateSelection.YESTERDAY -> saveExpenseWithDate(input, currentState, LocalDate.now(clock).minusDays(1))
            ExpenseDateSelection.MANUAL -> repeatManualDateInput(input, currentState)
        }

    private fun saveExpenseWithDate(
        input: UserInput,
        currentState: UserState.AwaitingExpenseManualDateInput,
        expenseDate: LocalDate,
    ): HandlerResult {
        val expenseDraft = currentState.expenseDraft.copy(expenseDate = expenseDate)
        val expense = expenseService.saveExpense(input.userId, expenseDraft)
        val delivery = responseDeliveryForExpenseCard(input.callbackMessageId ?: currentState.cardMessageId)

        return expenseSavedResult(
            expenseDraft = expenseDraft,
            categoryName = currentState.categoryName,
            expenseDate = expense.expenseDate,
            delivery = delivery,
        )
    }

    private fun repeatManualDateInput(
        input: UserInput,
        currentState: UserState.AwaitingExpenseManualDateInput,
    ): HandlerResult {
        val delivery = responseDeliveryForExpenseCard(input.callbackMessageId ?: currentState.cardMessageId)

        return HandlerResult(
            response =
                HandlerResponse(
                    text = currentState.toEnterExpenseDateManuallyMessage(),
                    actions = listOf(BotAction.ShowCancel),
                    delivery = delivery,
                ),
            nextState = currentState,
        )
    }

    private fun manualDateInputError(
        input: UserInput,
        currentState: UserState.AwaitingExpenseManualDateInput,
        errorCode: ErrorCode,
    ): HandlerResult {
        val delivery = responseDeliveryForExpenseCard(input.callbackMessageId ?: currentState.cardMessageId)

        return HandlerResult(
            response =
                HandlerResponse(
                    text = BotText.Error(errorCode),
                    actions = listOf(BotAction.ShowCancel),
                    delivery = delivery,
                ),
            nextState = currentState,
        )
    }

    private fun cancelExpenseCreation(
        input: UserInput,
        currentState: UserState.AwaitingExpenseManualDateInput,
    ): HandlerResult {
        val delivery = responseDeliveryForExpenseCard(input.callbackMessageId ?: currentState.cardMessageId)

        return HandlerResult(
            response =
                HandlerResponse(
                    text = BotText.ExpenseCanceled,
                    actions = actionsForCompletedExpenseCard(delivery),
                    delivery = delivery,
                ),
            nextState = UserState.Idle,
        )
    }

    private fun UserState.AwaitingExpenseManualDateInput.toEnterExpenseDateManuallyMessage(): BotText.EnterExpenseDateManually =
        BotText.EnterExpenseDateManually(
            amount = expenseDraft.amount,
            categoryName = categoryName,
            description = expenseDraft.description,
        )

    private fun expenseSavedResult(
        expenseDraft: ExpenseDraft,
        categoryName: String,
        expenseDate: LocalDate,
        delivery: ResponseDelivery,
    ): HandlerResult =
        HandlerResult(
            response =
                HandlerResponse(
                    text =
                        BotText.ExpenseSaved(
                            amount = expenseDraft.amount,
                            categoryName = categoryName,
                            expenseDate = expenseDate,
                            description = expenseDraft.description,
                        ),
                    actions = actionsForCompletedExpenseCard(delivery),
                    delivery = delivery,
                ),
            nextState = UserState.Idle,
        )
}
