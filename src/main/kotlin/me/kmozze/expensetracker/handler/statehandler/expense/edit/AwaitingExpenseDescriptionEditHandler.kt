package me.kmozze.expensetracker.handler.statehandler.expense.edit

import me.kmozze.expensetracker.handler.handlerResponse
import me.kmozze.expensetracker.handler.outgoingMessage
import me.kmozze.expensetracker.handler.statehandler.common.StateHandler
import me.kmozze.expensetracker.model.domain.bot.BotAction
import me.kmozze.expensetracker.model.domain.bot.BotText
import me.kmozze.expensetracker.model.domain.bot.HandlerResponse
import me.kmozze.expensetracker.model.domain.bot.UserCommand
import me.kmozze.expensetracker.model.domain.bot.UserInput
import me.kmozze.expensetracker.model.domain.bot.UserState
import me.kmozze.expensetracker.service.CategoryService
import me.kmozze.expensetracker.service.ExpenseService
import org.springframework.stereotype.Component
import kotlin.reflect.KClass

@Component
class AwaitingExpenseDescriptionEditHandler(
    private val expenseService: ExpenseService,
    private val categoryService: CategoryService,
) : StateHandler {
    override val supportedStateClass: KClass<out UserState> = UserState.AwaitingExpenseDescriptionEdit::class

    override fun handle(
        input: UserInput,
        currentState: UserState,
    ): HandlerResponse {
        require(currentState is UserState.AwaitingExpenseDescriptionEdit) {
            "AwaitingExpenseDescriptionEditHandler requires AwaitingExpenseDescriptionEdit state"
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

            is UserCommand.PlainText -> saveDescription(currentState, command.value)
            else -> repeatDescriptionInput(currentState)
        }
    }

    private fun saveDescription(
        currentState: UserState.AwaitingExpenseDescriptionEdit,
        descriptionText: String,
    ): HandlerResponse {
        val normalizedDescription = descriptionText.trim()
        if (normalizedDescription.isEmpty()) {
            return repeatDescriptionInput(currentState)
        }

        return buildDraftExpenseEditFieldSelectionResult(
            editSession =
                currentState.editSession.withExpenseDraft(
                    currentState.editSession.expenseDraft.copy(description = normalizedDescription),
                ),
        )
    }

    private fun repeatDescriptionInput(currentState: UserState.AwaitingExpenseDescriptionEdit): HandlerResponse =
        handlerResponse(
            message =
                outgoingMessage(
                    text = BotText.EnterExpenseDescription,
                    actions = listOf(BotAction.ShowCancel),
                ),
            nextState = currentState,
        )
}
