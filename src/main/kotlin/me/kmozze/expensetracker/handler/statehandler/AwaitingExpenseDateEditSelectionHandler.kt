package me.kmozze.expensetracker.handler.statehandler

import me.kmozze.expensetracker.exception.BusinessErrorCode
import me.kmozze.expensetracker.model.domain.BotAction
import me.kmozze.expensetracker.model.domain.BotText
import me.kmozze.expensetracker.model.domain.ExpenseDateChoice
import me.kmozze.expensetracker.model.domain.HandlerResponse
import me.kmozze.expensetracker.model.domain.HandlerResult
import me.kmozze.expensetracker.model.domain.OutgoingMessage
import me.kmozze.expensetracker.model.domain.UserCommand
import me.kmozze.expensetracker.model.domain.UserInput
import me.kmozze.expensetracker.model.domain.UserState
import me.kmozze.expensetracker.service.CategoryService
import me.kmozze.expensetracker.service.ExpenseService
import org.springframework.stereotype.Component
import java.time.Clock
import java.time.LocalDate
import kotlin.reflect.KClass

@Component
class AwaitingExpenseDateEditSelectionHandler(
    private val expenseService: ExpenseService,
    private val categoryService: CategoryService,
    private val clock: Clock,
) : StateHandler {
    override val supportedStateClass: KClass<out UserState> = UserState.AwaitingExpenseDateEditSelection::class

    override fun handle(
        input: UserInput,
        currentState: UserState,
    ): HandlerResult {
        require(currentState is UserState.AwaitingExpenseDateEditSelection) {
            "AwaitingExpenseDateEditSelectionHandler requires AwaitingExpenseDateEditSelection state"
        }

        return when (val command = input.command) {
            UserCommand.Cancel ->
                cancelExpenseEdit(
                    expenseService = expenseService,
                    categoryService = categoryService,
                    userId = input.userId,
                    expenseId = currentState.expenseId,
                )

            is UserCommand.SelectExpenseDate -> handleExpenseDateSelection(input, currentState, command.choice)
            is UserCommand.PlainText -> expenseDateSelectionError(currentState)
            else -> repeatDateSelection(input, currentState)
        }
    }

    private fun handleExpenseDateSelection(
        input: UserInput,
        currentState: UserState.AwaitingExpenseDateEditSelection,
        choice: ExpenseDateChoice,
    ): HandlerResult =
        when (choice) {
            ExpenseDateChoice.Today,
            ExpenseDateChoice.Yesterday,
            ->
                saveExpenseWithDate(
                    input = input,
                    currentState = currentState,
                    expenseDate = requireNotNull(choice.toDate(clock)),
                )

            ExpenseDateChoice.ManualInput -> requestManualDateInput(input, currentState)
        }

    private fun expenseDateSelectionError(currentState: UserState.AwaitingExpenseDateEditSelection): HandlerResult =
        HandlerResult(
            response =
                HandlerResponse(
                    outgoingMessages =
                        listOf(
                            OutgoingMessage(
                                text = BotText.Error(BusinessErrorCode.INVALID_EXPENSE_DATE_SELECTION),
                                actions = listOf(BotAction.ShowExpenseDateSelection),
                            ),
                        ),
                ),
            nextState = currentState,
        )

    private fun saveExpenseWithDate(
        input: UserInput,
        currentState: UserState.AwaitingExpenseDateEditSelection,
        expenseDate: LocalDate,
    ): HandlerResult {
        val updatedExpense = expenseService.updateExpenseDateForUser(input.userId, currentState.expenseId, expenseDate)
        if (updatedExpense == null) {
            return expenseEditUnavailableResult()
        }

        return buildUpdatedExpenseResult(
            expenseService = expenseService,
            categoryService = categoryService,
            userId = input.userId,
            expenseId = updatedExpense.id,
            nextState = UserState.Idle,
            prefixText = BotText.Done,
        )
    }

    private fun requestManualDateInput(
        input: UserInput,
        currentState: UserState.AwaitingExpenseDateEditSelection,
    ): HandlerResult {
        val expense =
            expenseService.findExpenseForUser(
                userId = input.userId,
                expenseId = currentState.expenseId,
            ) ?: return expenseEditUnavailableResult()

        val category =
            categoryService.findCategoryForUser(
                categoryId = expense.categoryId,
                userId = input.userId,
            ) ?: return expenseEditUnavailableResult()

        return HandlerResult(
            response =
                HandlerResponse(
                    outgoingMessages =
                        listOf(
                            OutgoingMessage(
                                text =
                                    BotText.EnterExpenseDateManually(
                                        amount = expense.amount,
                                        categoryName = category.name,
                                        description = expense.description,
                                    ),
                                actions = listOf(BotAction.ShowCancel),
                            ),
                        ),
                ),
            nextState = UserState.AwaitingExpenseDateEditManualInput(currentState.expenseId),
        )
    }

    private fun repeatDateSelection(
        input: UserInput,
        currentState: UserState.AwaitingExpenseDateEditSelection,
    ): HandlerResult {
        val expense =
            expenseService.findExpenseForUser(
                userId = input.userId,
                expenseId = currentState.expenseId,
            ) ?: return expenseEditUnavailableResult()

        val category =
            categoryService.findCategoryForUser(
                categoryId = expense.categoryId,
                userId = input.userId,
            ) ?: return expenseEditUnavailableResult()

        return HandlerResult(
            response =
                HandlerResponse(
                    outgoingMessages =
                        listOf(
                            OutgoingMessage(
                                text =
                                    BotText.SelectExpenseDate(
                                        amount = expense.amount,
                                        categoryName = category.name,
                                        description = expense.description,
                                    ),
                                actions = listOf(BotAction.ShowExpenseDateSelection),
                            ),
                        ),
                ),
            nextState = currentState,
        )
    }
}
