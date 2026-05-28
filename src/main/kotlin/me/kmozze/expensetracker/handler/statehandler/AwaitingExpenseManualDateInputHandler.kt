package me.kmozze.expensetracker.handler.statehandler

import me.kmozze.expensetracker.exception.BusinessException
import me.kmozze.expensetracker.exception.ErrorCode
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
    ): HandlerResult {
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
            is UserCommand.RequestExpenseEdit,
            is UserCommand.RequestExpenseDeletion,
            UserCommand.InvalidExpenseAction,
            -> repeatManualDateInput(currentState)
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
                return manualDateInputError(currentState, e.errorCode)
            }

        return saveExpenseWithDate(input, currentState, expenseDate)
    }

    private fun saveExpenseWithDate(
        input: UserInput,
        currentState: UserState.AwaitingExpenseManualDateInput,
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

    private fun repeatManualDateInput(currentState: UserState.AwaitingExpenseManualDateInput): HandlerResult =
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
            nextState = currentState,
        )

    private fun manualDateInputError(
        currentState: UserState.AwaitingExpenseManualDateInput,
        errorCode: ErrorCode,
    ): HandlerResult =
        HandlerResult(
            response =
                HandlerResponse(
                    outgoingMessages =
                        listOf(
                            OutgoingMessage(
                                text = BotText.Error(errorCode),
                                actions = listOf(BotAction.ShowCancel),
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

    private fun UserState.AwaitingExpenseManualDateInput.toEnterExpenseDateManuallyMessage(): BotText.EnterExpenseDateManually =
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
