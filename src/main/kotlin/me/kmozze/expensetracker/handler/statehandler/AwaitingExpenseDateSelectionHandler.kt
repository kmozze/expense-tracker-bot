package me.kmozze.expensetracker.handler.statehandler

import me.kmozze.expensetracker.exception.BusinessErrorCode
import me.kmozze.expensetracker.model.domain.BotAction
import me.kmozze.expensetracker.model.domain.BotMessage
import me.kmozze.expensetracker.model.domain.ExpenseDateSelection
import me.kmozze.expensetracker.model.domain.ExpenseDraft
import me.kmozze.expensetracker.model.domain.HandlerResponse
import me.kmozze.expensetracker.model.domain.HandlerResult
import me.kmozze.expensetracker.model.domain.UserCommand
import me.kmozze.expensetracker.model.domain.UserInput
import me.kmozze.expensetracker.model.domain.UserState
import me.kmozze.expensetracker.service.ExpenseService
import org.springframework.stereotype.Component
import java.time.Clock
import java.time.LocalDate
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
            is UserCommand.SelectExpenseDate -> handleExpenseDateSelection(input, currentState, command.selection)
            UserCommand.InvalidExpenseDateSelection ->
                expenseDateSelectionError(
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
            is UserCommand.PlainText,
            -> repeatExpenseDateSelection(currentState)
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
            ExpenseDateSelection.MANUAL -> requestManualDateInput(currentState)
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
            categoryName = currentState.categoryName,
            expenseDate = expense.expenseDate,
        )
    }

    private fun requestManualDateInput(currentState: UserState.AwaitingExpenseDateSelection): HandlerResult =
        HandlerResult(
            response =
                HandlerResponse(
                    message = currentState.toEnterExpenseDateManuallyMessage(),
                    actions = listOf(BotAction.ShowCancel),
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
                    message = currentState.toSelectExpenseDateMessage(),
                    actions = listOf(BotAction.ShowExpenseDateSelection),
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
                    message = BotMessage.Error(errorCode),
                    actions = listOf(BotAction.ShowExpenseDateSelection),
                ),
            nextState = currentState,
        )

    private fun cancelExpenseCreation(): HandlerResult =
        HandlerResult(
            response =
                HandlerResponse(
                    message = BotMessage.ExpenseCanceled,
                    actions = listOf(BotAction.ShowMainMenu),
                ),
            nextState = UserState.Idle,
        )

    private fun UserState.AwaitingExpenseDateSelection.toSelectExpenseDateMessage(): BotMessage.SelectExpenseDate =
        BotMessage.SelectExpenseDate(
            amount = expenseDraft.amount,
            categoryName = categoryName,
            description = expenseDraft.description,
        )

    private fun UserState.AwaitingExpenseDateSelection.toEnterExpenseDateManuallyMessage(): BotMessage.EnterExpenseDateManually =
        BotMessage.EnterExpenseDateManually(
            amount = expenseDraft.amount,
            categoryName = categoryName,
            description = expenseDraft.description,
        )

    private fun expenseSavedResult(
        expenseDraft: ExpenseDraft,
        categoryName: String,
        expenseDate: LocalDate,
    ): HandlerResult =
        HandlerResult(
            response =
                HandlerResponse(
                    message =
                        BotMessage.ExpenseSaved(
                            amount = expenseDraft.amount,
                            categoryName = categoryName,
                            expenseDate = expenseDate,
                            description = expenseDraft.description,
                        ),
                    actions = listOf(BotAction.ShowMainMenu),
                ),
            nextState = UserState.Idle,
        )
}
