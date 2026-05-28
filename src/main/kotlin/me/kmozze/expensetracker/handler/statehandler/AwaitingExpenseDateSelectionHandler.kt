package me.kmozze.expensetracker.handler.statehandler

import me.kmozze.expensetracker.adapter.ui.Buttons
import me.kmozze.expensetracker.exception.BusinessErrorCode
import me.kmozze.expensetracker.model.domain.BotAction
import me.kmozze.expensetracker.model.domain.BotText
import me.kmozze.expensetracker.model.domain.ExpenseDraft
import me.kmozze.expensetracker.model.domain.HandlerResponse
import me.kmozze.expensetracker.model.domain.HandlerResult
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
    ): HandlerResult {
        require(currentState is UserState.AwaitingExpenseDateSelection) {
            "AwaitingExpenseDateSelectionHandler requires AwaitingExpenseDateSelection state"
        }

        return when (val command = input.command) {
            UserCommand.Cancel -> cancelExpenseCreation()
            is UserCommand.PlainText -> handleExpenseDateSelection(input, currentState, command.value)
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
            UserCommand.InvalidExpenseAction,
            -> repeatExpenseDateSelection(currentState)
        }
    }

    private fun handleExpenseDateSelection(
        input: UserInput,
        currentState: UserState.AwaitingExpenseDateSelection,
        text: String,
    ): HandlerResult =
        when (text) {
            Buttons.TODAY -> saveExpenseWithDate(input, currentState, LocalDate.now(clock))
            Buttons.YESTERDAY -> saveExpenseWithDate(input, currentState, LocalDate.now(clock).minusDays(1))
            Buttons.ENTER_DATE_MANUALLY -> requestManualDateInput(currentState)
            else ->
                expenseDateSelectionError(
                    currentState = currentState,
                    errorCode = BusinessErrorCode.INVALID_EXPENSE_DATE_SELECTION,
                )
        }

    private fun saveExpenseWithDate(
        input: UserInput,
        currentState: UserState.AwaitingExpenseDateSelection,
        expenseDate: LocalDate,
    ): HandlerResult {
        val expenseDraft = currentState.expenseDraft.copy(expenseDate = expenseDate)
        val expense = expenseService.saveExpense(input.userId, expenseDraft)

        return expenseSavedResult(
            expenseDraft = expenseDraft,
            expenseId = expense.id,
            categoryName = currentState.categoryName,
            expenseDate = expense.expenseDate,
        )
    }

    private fun requestManualDateInput(currentState: UserState.AwaitingExpenseDateSelection): HandlerResult =
        HandlerResult(
            response =
                HandlerResponse(
                    outgoingMessages =
                        listOf(
                            OutgoingMessage(
                                text = currentState.toEnterExpenseDateManuallyMessage(),
                                actions = listOf(BotAction.ShowCancel),
                            ),
                        ),
                ),
            nextState =
                UserState.AwaitingExpenseManualDateInput(
                    expenseDraft = currentState.expenseDraft,
                    categoryName = currentState.categoryName,
                ),
        )

    private fun repeatExpenseDateSelection(currentState: UserState.AwaitingExpenseDateSelection): HandlerResult =
        HandlerResult(
            response =
                HandlerResponse(
                    outgoingMessages =
                        listOf(
                            OutgoingMessage(
                                text = currentState.toSelectExpenseDateMessage(),
                                actions = listOf(BotAction.ShowExpenseDateSelection),
                            ),
                        ),
                ),
            nextState = currentState,
        )

    private fun expenseDateSelectionError(
        currentState: UserState.AwaitingExpenseDateSelection,
        errorCode: BusinessErrorCode,
    ): HandlerResult =
        HandlerResult(
            response =
                HandlerResponse(
                    outgoingMessages =
                        listOf(
                            OutgoingMessage(
                                text = BotText.Error(errorCode),
                                actions = listOf(BotAction.ShowExpenseDateSelection),
                            ),
                        ),
                ),
            nextState = currentState,
        )

    private fun cancelExpenseCreation(): HandlerResult =
        HandlerResult(
            response =
                HandlerResponse(
                    outgoingMessages =
                        listOf(
                            OutgoingMessage(
                                text = BotText.ExpenseCanceled,
                                actions = listOf(BotAction.RemoveReplyKeyboard),
                            ),
                        ),
                ),
            nextState = UserState.Idle,
        )

    private fun UserState.AwaitingExpenseDateSelection.toSelectExpenseDateMessage(): BotText.SelectExpenseDate =
        BotText.SelectExpenseDate(
            amount = expenseDraft.amount,
            categoryName = categoryName,
            description = expenseDraft.description,
        )

    private fun UserState.AwaitingExpenseDateSelection.toEnterExpenseDateManuallyMessage(): BotText.EnterExpenseDateManually =
        BotText.EnterExpenseDateManually(
            amount = expenseDraft.amount,
            categoryName = categoryName,
            description = expenseDraft.description,
        )

    private fun expenseSavedResult(
        expenseDraft: ExpenseDraft,
        expenseId: UUID,
        categoryName: String,
        expenseDate: LocalDate,
    ): HandlerResult =
        HandlerResult(
            response =
                HandlerResponse(
                    outgoingMessages =
                        listOf(
                            OutgoingMessage(
                                text = BotText.Done,
                                actions = listOf(BotAction.RemoveReplyKeyboard),
                            ),
                            OutgoingMessage(
                                text =
                                    BotText.ExpenseSaved(
                                        amount = expenseDraft.amount,
                                        categoryName = categoryName,
                                        expenseDate = expenseDate,
                                        description = expenseDraft.description,
                                    ),
                                actions = listOf(BotAction.ShowExpenseCardActions(expenseId)),
                            ),
                        ),
                ),
            nextState = UserState.Idle,
        )
}
