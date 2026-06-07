package me.kmozze.expensetracker.handler.statehandler

import me.kmozze.expensetracker.exception.BusinessErrorCode
import me.kmozze.expensetracker.handler.handlerResponse
import me.kmozze.expensetracker.handler.outgoingMessage
import me.kmozze.expensetracker.model.domain.BotAction
import me.kmozze.expensetracker.model.domain.BotText
import me.kmozze.expensetracker.model.domain.ExpenseDateChoice
import me.kmozze.expensetracker.model.domain.HandlerResponse
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
    ): HandlerResponse {
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

            UserCommand.FinishExpenseEdit ->
                finishExpenseEdit(
                    expenseService = expenseService,
                    categoryService = categoryService,
                    userId = input.userId,
                    expenseId = currentState.expenseId,
                    expenseDraft = currentState.expenseDraft,
                )

            is UserCommand.SelectExpenseDate -> handleExpenseDateSelection(currentState, command.choice)
            is UserCommand.PlainText -> expenseDateSelectionError(currentState)
            else -> repeatDateSelection(currentState)
        }
    }

    private fun handleExpenseDateSelection(
        currentState: UserState.AwaitingExpenseDateEditSelection,
        choice: ExpenseDateChoice,
    ): HandlerResponse =
        when (choice) {
            ExpenseDateChoice.Today,
            ExpenseDateChoice.Yesterday,
            ->
                updateDraftWithDate(
                    currentState = currentState,
                    expenseDate = requireNotNull(choice.toDate(clock)),
                )

            ExpenseDateChoice.ManualInput -> requestManualDateInput(currentState)
        }

    private fun expenseDateSelectionError(currentState: UserState.AwaitingExpenseDateEditSelection): HandlerResponse =
        handlerResponse(
            message =
                outgoingMessage(
                    text = BotText.Error(BusinessErrorCode.INVALID_EXPENSE_DATE_SELECTION),
                    actions = listOf(BotAction.ShowExpenseDateSelection),
                ),
            nextState = currentState,
        )

    private fun updateDraftWithDate(
        currentState: UserState.AwaitingExpenseDateEditSelection,
        expenseDate: LocalDate,
    ): HandlerResponse =
        buildDraftExpenseEditFieldSelectionResult(
            expenseId = currentState.expenseId,
            expenseDraft = currentState.expenseDraft.copy(expenseDate = expenseDate),
            categoryName = currentState.categoryName,
        )

    private fun requestManualDateInput(currentState: UserState.AwaitingExpenseDateEditSelection): HandlerResponse =
        handlerResponse(
            messages =
                listOf(
                    outgoingMessage(
                        text = currentState.expenseDraft.toExpenseView(categoryName = currentState.categoryName),
                        actions = emptyList(),
                    ),
                    outgoingMessage(
                        text = BotText.EnterExpenseDateManually,
                        actions = listOf(BotAction.ShowCancel),
                    ),
                ),
            nextState =
                UserState.AwaitingExpenseDateEditManualInput(
                    expenseId = currentState.expenseId,
                    expenseDraft = currentState.expenseDraft,
                    categoryName = currentState.categoryName,
                ),
        )

    private fun repeatDateSelection(currentState: UserState.AwaitingExpenseDateEditSelection): HandlerResponse =
        handlerResponse(
            messages =
                listOf(
                    outgoingMessage(
                        text = currentState.expenseDraft.toExpenseView(categoryName = currentState.categoryName),
                        actions = emptyList(),
                    ),
                    outgoingMessage(
                        text = BotText.SelectExpenseDate,
                        actions = listOf(BotAction.ShowExpenseDateSelection),
                    ),
                ),
            nextState = currentState,
        )
}
