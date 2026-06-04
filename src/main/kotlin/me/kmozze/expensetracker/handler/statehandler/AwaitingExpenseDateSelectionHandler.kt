package me.kmozze.expensetracker.handler.statehandler

import me.kmozze.expensetracker.exception.BusinessErrorCode
import me.kmozze.expensetracker.model.domain.BotAction
import me.kmozze.expensetracker.model.domain.BotText
import me.kmozze.expensetracker.model.domain.ExpenseDateChoice
import me.kmozze.expensetracker.model.domain.ExpenseDraft
import me.kmozze.expensetracker.model.domain.HandlerResponse
import me.kmozze.expensetracker.model.domain.OutgoingMessage
import me.kmozze.expensetracker.model.domain.UserCommand
import me.kmozze.expensetracker.model.domain.UserInput
import me.kmozze.expensetracker.model.domain.UserState
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
            categoryName = currentState.categoryName,
            expenseDate = expense.expenseDate,
        )
    }

    private fun requestManualDateInput(currentState: UserState.AwaitingExpenseDateSelection): HandlerResponse =
        HandlerResponse(
            outgoingMessages =
                listOf(
                    OutgoingMessage(
                        text = currentState.expenseDraft.toExpenseView(categoryName = currentState.categoryName),
                        actions = emptyList(),
                    ),
                    OutgoingMessage(
                        text = BotText.EnterExpenseDateManually,
                        actions = listOf(BotAction.ShowCancel),
                    ),
                ),
            nextState =
                UserState.AwaitingExpenseManualDateInput(
                    expenseDraft = currentState.expenseDraft,
                    categoryName = currentState.categoryName,
                ),
        )

    private fun repeatExpenseDateSelection(currentState: UserState.AwaitingExpenseDateSelection): HandlerResponse =
        HandlerResponse(
            outgoingMessages =
                listOf(
                    OutgoingMessage(
                        text = currentState.expenseDraft.toExpenseView(categoryName = currentState.categoryName),
                        actions = emptyList(),
                    ),
                    OutgoingMessage(
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
        HandlerResponse(
            outgoingMessages =
                listOf(
                    OutgoingMessage(
                        text = BotText.Error(errorCode),
                        actions = listOf(BotAction.ShowExpenseDateSelection),
                    ),
                ),
            nextState = currentState,
        )

    private fun cancelExpenseCreation(): HandlerResponse =
        HandlerResponse(
            outgoingMessages =
                listOf(
                    OutgoingMessage(
                        text = BotText.ExpenseCanceled,
                        actions = listOf(BotAction.RemoveReplyKeyboard),
                    ),
                ),
            nextState = UserState.Idle,
        )

    private fun expenseSavedResult(
        expenseDraft: ExpenseDraft,
        expenseId: UUID,
        categoryName: String,
        expenseDate: LocalDate,
    ): HandlerResponse =
        HandlerResponse(
            outgoingMessages =
                listOf(
                    OutgoingMessage(
                        text = BotText.ExpenseSaved,
                        actions = listOf(BotAction.RemoveReplyKeyboard),
                    ),
                    OutgoingMessage(
                        text = expenseDraft.toExpenseView(categoryName = categoryName, expenseDate = expenseDate),
                        actions = listOf(BotAction.ShowExpenseCardActions(expenseId)),
                    ),
                ),
            nextState = UserState.Idle,
        )
}
