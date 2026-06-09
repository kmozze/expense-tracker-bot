package me.kmozze.expensetracker.handler.statehandler

import me.kmozze.expensetracker.exception.BusinessException
import me.kmozze.expensetracker.exception.ErrorCode
import me.kmozze.expensetracker.handler.handlerResponse
import me.kmozze.expensetracker.handler.outgoingMessage
import me.kmozze.expensetracker.model.domain.BotAction
import me.kmozze.expensetracker.model.domain.BotText
import me.kmozze.expensetracker.model.domain.ExpenseDraft
import me.kmozze.expensetracker.model.domain.HandlerResponse
import me.kmozze.expensetracker.model.domain.UserCommand
import me.kmozze.expensetracker.model.domain.UserInput
import me.kmozze.expensetracker.model.domain.UserState
import me.kmozze.expensetracker.service.ExpenseService
import org.springframework.stereotype.Component
import java.time.LocalDate
import java.util.UUID
import kotlin.reflect.KClass

@Component
class AwaitingExpenseManualDateInputHandler(
    private val expenseService: ExpenseService,
) : StateHandler {
    override val supportedStateClass: KClass<out UserState> = UserState.AwaitingExpenseManualDateInput::class

    override fun handle(
        input: UserInput,
        currentState: UserState,
    ): HandlerResponse {
        require(currentState is UserState.AwaitingExpenseManualDateInput) {
            "AwaitingExpenseManualDateInputHandler requires AwaitingExpenseManualDateInput state"
        }

        return when (val command = input.command) {
            UserCommand.Cancel -> cancelExpenseCreation()
            is UserCommand.PlainText -> saveExpenseWithManualDate(input, currentState, command.value)
            UserCommand.Unsupported,
            UserCommand.Start,
            UserCommand.Menu,
            UserCommand.AddExpense,
            UserCommand.ViewExpenses,
            UserCommand.Categories,
            UserCommand.Statistics,
            UserCommand.FinishExpenseEdit,
            is UserCommand.RequestExpenseEdit,
            is UserCommand.RequestExpenseDeletion,
            is UserCommand.ConfirmExpenseDeletion,
            is UserCommand.CancelExpenseDeletion,
            is UserCommand.SelectExpenseDate,
            is UserCommand.SelectExpenseEditField,
            UserCommand.InvalidExpenseAction,
            -> repeatManualDateInput(currentState)
        }
    }

    private fun saveExpenseWithManualDate(
        input: UserInput,
        currentState: UserState.AwaitingExpenseManualDateInput,
        dateText: String,
    ): HandlerResponse {
        val expenseDate =
            try {
                expenseService.parseExpenseDate(dateText)
            } catch (e: BusinessException) {
                return manualDateInputError(currentState, e.errorCode)
            }

        return saveExpenseWithDate(input, currentState, expenseDate)
    }

    private fun saveExpenseWithDate(
        input: UserInput,
        currentState: UserState.AwaitingExpenseManualDateInput,
        expenseDate: LocalDate,
    ): HandlerResponse {
        val expenseDraft = currentState.expenseDraft.copy(expenseDate = expenseDate)
        val expense = expenseService.saveExpense(input.userId, expenseDraft)

        return expenseSavedResult(
            expenseDraft = expenseDraft,
            expenseId = expense.id,
        )
    }

    private fun repeatManualDateInput(currentState: UserState.AwaitingExpenseManualDateInput): HandlerResponse =
        handlerResponse(
            messages =
                listOf(
                    outgoingMessage(
                        text = currentState.expenseDraft.toExpenseView(),
                        actions = emptyList(),
                    ),
                    outgoingMessage(
                        text = BotText.EnterExpenseDateManually,
                        actions = listOf(BotAction.ShowCancel),
                    ),
                ),
            nextState = currentState,
        )

    private fun manualDateInputError(
        currentState: UserState.AwaitingExpenseManualDateInput,
        errorCode: ErrorCode,
    ): HandlerResponse =
        handlerResponse(
            message =
                outgoingMessage(
                    text = BotText.Error(errorCode),
                    actions = listOf(BotAction.ShowCancel),
                ),
            nextState = currentState,
        )

    private fun cancelExpenseCreation(): HandlerResponse =
        handlerResponse(
            message =
                outgoingMessage(
                    text = BotText.ExpenseCanceled,
                    actions = listOf(BotAction.RemoveReplyKeyboard),
                ),
            nextState = UserState.Idle,
        )

    private fun expenseSavedResult(
        expenseDraft: ExpenseDraft,
        expenseId: UUID,
    ): HandlerResponse =
        handlerResponse(
            messages =
                listOf(
                    outgoingMessage(
                        text = BotText.ExpenseSaved,
                        actions = listOf(BotAction.RemoveReplyKeyboard),
                    ),
                    outgoingMessage(
                        text = expenseDraft.toExpenseView(),
                        actions = listOf(BotAction.ShowExpenseCardActions(expenseId)),
                    ),
                ),
            nextState = UserState.Idle,
        )
}
