package me.kmozze.expensetracker.handler.statehandler

import me.kmozze.expensetracker.handler.handlerResponse
import me.kmozze.expensetracker.handler.outgoingMessage
import me.kmozze.expensetracker.model.domain.BotAction
import me.kmozze.expensetracker.model.domain.BotText
import me.kmozze.expensetracker.model.domain.ExpenseEditField
import me.kmozze.expensetracker.model.domain.HandlerResponse
import me.kmozze.expensetracker.model.domain.UserCommand
import me.kmozze.expensetracker.model.domain.UserInput
import me.kmozze.expensetracker.model.domain.UserState
import me.kmozze.expensetracker.service.CategoryService
import me.kmozze.expensetracker.service.ExpenseService
import org.springframework.stereotype.Component
import kotlin.reflect.KClass

@Component
class AwaitingExpenseEditFieldSelectionHandler(
    private val expenseService: ExpenseService,
    private val categoryService: CategoryService,
) : StateHandler {
    override val supportedStateClass: KClass<out UserState> = UserState.AwaitingExpenseEditFieldSelection::class

    override fun handle(
        input: UserInput,
        currentState: UserState,
    ): HandlerResponse {
        require(currentState is UserState.AwaitingExpenseEditFieldSelection) {
            "AwaitingExpenseEditFieldSelectionHandler requires AwaitingExpenseEditFieldSelection state"
        }

        return when (val command = input.command) {
            UserCommand.Cancel ->
                cancelExpenseEdit(
                    expenseService = expenseService,
                    categoryService = categoryService,
                    userId = input.userId,
                    expenseId = currentState.editSession.expenseId,
                )

            UserCommand.FinishExpenseEdit ->
                finishExpenseEdit(
                    expenseService = expenseService,
                    categoryService = categoryService,
                    userId = input.userId,
                    editSession = currentState.editSession,
                )

            is UserCommand.SelectExpenseEditField -> selectEditField(input, currentState, command.field)
            else -> repeatEditFieldSelection(currentState)
        }
    }

    private fun selectEditField(
        input: UserInput,
        currentState: UserState.AwaitingExpenseEditFieldSelection,
        selectedField: ExpenseEditField,
    ): HandlerResponse =
        when (selectedField) {
            ExpenseEditField.Amount ->
                handlerResponse(
                    message =
                        outgoingMessage(
                            text = BotText.EnterExpenseAmount,
                            actions = listOf(BotAction.ShowCancel),
                        ),
                    nextState =
                        UserState.AwaitingExpenseAmountEdit(
                            editSession = currentState.editSession,
                        ),
                )

            ExpenseEditField.Category -> showCategorySelectionForEdit(input, currentState)

            ExpenseEditField.Date -> requestDateSelection(input, currentState)

            ExpenseEditField.Description ->
                handlerResponse(
                    message =
                        outgoingMessage(
                            text = BotText.EnterExpenseDescription,
                            actions = listOf(BotAction.ShowCancel),
                        ),
                    nextState =
                        UserState.AwaitingExpenseDescriptionEdit(
                            editSession = currentState.editSession,
                        ),
                )
        }

    private fun showCategorySelectionForEdit(
        input: UserInput,
        currentState: UserState.AwaitingExpenseEditFieldSelection,
    ): HandlerResponse {
        val categories = categoryService.getCategories(input.userId)

        if (categories.isEmpty()) {
            return handlerResponse(
                message =
                    outgoingMessage(
                        text = BotText.NoCategories,
                        actions = listOf(BotAction.ShowMainMenu),
                    ),
                nextState = UserState.Idle,
            )
        }

        return handlerResponse(
            messages =
                listOf(
                    outgoingMessage(
                        text = currentState.editSession.expenseDraft.toExpenseView(),
                        actions = emptyList(),
                    ),
                    outgoingMessage(
                        text = BotText.SelectCategory,
                        actions = listOf(BotAction.ShowCategorySelection(categories.map { it.name })),
                    ),
                ),
            nextState =
                UserState.AwaitingExpenseCategoryEdit(
                    editSession = currentState.editSession,
                ),
        )
    }

    private fun requestDateSelection(
        input: UserInput,
        currentState: UserState.AwaitingExpenseEditFieldSelection,
    ): HandlerResponse =
        handlerResponse(
            messages =
                listOf(
                    outgoingMessage(
                        text = currentState.editSession.expenseDraft.toExpenseView(),
                        actions = emptyList(),
                    ),
                    outgoingMessage(
                        text = BotText.SelectExpenseDate,
                        actions = listOf(BotAction.ShowExpenseDateSelection),
                    ),
                ),
            nextState =
                UserState.AwaitingExpenseDateEditSelection(
                    editSession = currentState.editSession,
                ),
        )

    private fun repeatEditFieldSelection(currentState: UserState.AwaitingExpenseEditFieldSelection): HandlerResponse =
        handlerResponse(
            message =
                outgoingMessage(
                    text = BotText.EditExpenseFieldSelection,
                    actions = listOf(BotAction.ShowExpenseEditFieldSelection),
                ),
            nextState = currentState,
        )
}
