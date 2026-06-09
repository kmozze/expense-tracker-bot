package me.kmozze.expensetracker.handler.statehandler.expense.add

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
import me.kmozze.expensetracker.model.domain.expense.ExpenseDateChoice
import me.kmozze.expensetracker.model.domain.expense.ExpenseDraft
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
    ): HandlerResponse {
        require(currentState is UserState.AwaitingExpenseDateSelection) {
            "AwaitingExpenseDateSelectionHandler requires AwaitingExpenseDateSelection state"
        }

        return when (val command = input.command) {
            UserCommand.Cancel -> cancelExpenseCreation()
            is UserCommand.SelectExpenseDate -> handleExpenseDateSelection(input, currentState, command.choice)
            is UserCommand.PlainText ->
                expenseDateSelectionError(
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
            UserCommand.FinishExpenseEdit,
            is UserCommand.RequestExpenseEdit,
            is UserCommand.RequestExpenseDeletion,
            is UserCommand.ConfirmExpenseDeletion,
            is UserCommand.CancelExpenseDeletion,
            is UserCommand.SelectExpenseEditField,
            UserCommand.InvalidExpenseAction,
            -> repeatExpenseDateSelection(currentState)
        }
    }

    private fun handleExpenseDateSelection(
        input: UserInput,
        currentState: UserState.AwaitingExpenseDateSelection,
        choice: ExpenseDateChoice,
    ): HandlerResponse =
        when (choice) {
            ExpenseDateChoice.Today,
            ExpenseDateChoice.Yesterday,
            -> saveExpenseWithDate(input, currentState, requireNotNull(choice.toDate(clock)))

            ExpenseDateChoice.ManualInput -> requestManualDateInput(currentState)
        }

    private fun saveExpenseWithDate(
        input: UserInput,
        currentState: UserState.AwaitingExpenseDateSelection,
        expenseDate: LocalDate,
    ): HandlerResponse {
        val expenseDraft = currentState.expenseDraft.copy(expenseDate = expenseDate)
        val expense = expenseService.saveExpense(input.userId, expenseDraft)

        return expenseSavedResult(
            expenseDraft = expenseDraft,
            expenseId = expense.id,
        )
    }

    private fun requestManualDateInput(currentState: UserState.AwaitingExpenseDateSelection): HandlerResponse =
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
            nextState =
                UserState.AwaitingExpenseManualDateInput(
                    expenseDraft = currentState.expenseDraft,
                ),
        )

    private fun repeatExpenseDateSelection(currentState: UserState.AwaitingExpenseDateSelection): HandlerResponse =
        handlerResponse(
            messages =
                listOf(
                    outgoingMessage(
                        text = currentState.expenseDraft.toExpenseView(),
                        actions = emptyList(),
                    ),
                    outgoingMessage(
                        text = BotText.SelectExpenseDate,
                        actions = listOf(BotAction.ShowExpenseDateSelection),
                    ),
                ),
            nextState = currentState,
        )

    private fun expenseDateSelectionError(
        currentState: UserState.AwaitingExpenseDateSelection,
        errorCode: BusinessErrorCode,
    ): HandlerResponse =
        handlerResponse(
            message =
                outgoingMessage(
                    text = BotText.Error(errorCode),
                    actions = listOf(BotAction.ShowExpenseDateSelection),
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
