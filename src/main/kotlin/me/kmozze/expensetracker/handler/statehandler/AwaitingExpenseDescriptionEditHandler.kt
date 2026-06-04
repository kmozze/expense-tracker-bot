package me.kmozze.expensetracker.handler.statehandler

import me.kmozze.expensetracker.model.domain.BotAction
import me.kmozze.expensetracker.model.domain.BotText
import me.kmozze.expensetracker.model.domain.HandlerResponse
import me.kmozze.expensetracker.model.domain.HandlerResult
import me.kmozze.expensetracker.model.domain.OutgoingMessage
import me.kmozze.expensetracker.model.domain.UserCommand
import me.kmozze.expensetracker.model.domain.UserInput
import me.kmozze.expensetracker.model.domain.UserState
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
    ): HandlerResult {
        require(currentState is UserState.AwaitingExpenseDescriptionEdit) {
            "AwaitingExpenseDescriptionEditHandler requires AwaitingExpenseDescriptionEdit state"
        }

        return when (val command = input.command) {
            UserCommand.Cancel ->
                cancelExpenseEdit(
                    expenseService = expenseService,
                    categoryService = categoryService,
                    userId = input.userId,
                    expenseId = currentState.expenseId,
                )

            is UserCommand.PlainText -> saveDescription(input, currentState, command.value)
            else -> repeatDescriptionInput(currentState)
        }
    }

    private fun saveDescription(
        input: UserInput,
        currentState: UserState.AwaitingExpenseDescriptionEdit,
        descriptionText: String,
    ): HandlerResult {
        val normalizedDescription = descriptionText.trim()
        if (normalizedDescription.isEmpty()) {
            return repeatDescriptionInput(currentState)
        }

        val updatedExpense =
            expenseService.updateExpenseDescriptionForUser(
                userId = input.userId,
                expenseId = currentState.expenseId,
                description = normalizedDescription,
            )
        if (updatedExpense == null) {
            return expenseEditUnavailableResult()
        }

        return buildUpdatedExpenseResult(
            expenseService = expenseService,
            categoryService = categoryService,
            userId = input.userId,
            expenseId = updatedExpense.id,
            nextState = UserState.Idle,
            prefixText = BotText.ExpenseSaved,
        )
    }

    private fun repeatDescriptionInput(currentState: UserState.AwaitingExpenseDescriptionEdit): HandlerResult =
        HandlerResult(
            response =
                HandlerResponse(
                    outgoingMessages =
                        listOf(
                            OutgoingMessage(
                                text = BotText.EnterExpenseDescription,
                                actions = listOf(BotAction.ShowCancel),
                            ),
                        ),
                ),
            nextState = currentState,
        )
}
