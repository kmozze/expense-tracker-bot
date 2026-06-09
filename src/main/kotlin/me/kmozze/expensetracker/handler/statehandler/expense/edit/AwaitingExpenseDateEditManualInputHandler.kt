package me.kmozze.expensetracker.handler.statehandler.expense.edit

import me.kmozze.expensetracker.exception.BusinessException
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
import me.kmozze.expensetracker.service.CategoryService
import me.kmozze.expensetracker.service.ExpenseService
import org.springframework.stereotype.Component
import kotlin.reflect.KClass

@Component
class AwaitingExpenseDateEditManualInputHandler(
    private val expenseService: ExpenseService,
    private val categoryService: CategoryService,
) : StateHandler {
    override val supportedStateClass: KClass<out UserState> = UserState.AwaitingExpenseDateEditManualInput::class

    override fun handle(
        input: UserInput,
        currentState: UserState,
    ): HandlerResponse {
        require(currentState is UserState.AwaitingExpenseDateEditManualInput) {
            "AwaitingExpenseDateEditManualInputHandler requires AwaitingExpenseDateEditManualInput state"
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

            is UserCommand.PlainText -> updateDate(currentState, command.value)
            else -> repeatManualDateInput(currentState)
        }
    }

    private fun updateDate(
        currentState: UserState.AwaitingExpenseDateEditManualInput,
        dateText: String,
    ): HandlerResponse {
        val expenseDate =
            try {
                expenseService.parseExpenseDate(dateText)
            } catch (e: BusinessException) {
                return handlerResponse(
                    message =
                        outgoingMessage(
                            text = BotText.Error(e.errorCode),
                            actions = listOf(BotAction.ShowCancel),
                        ),
                    nextState = currentState,
                )
            }

        return buildDraftExpenseEditFieldSelectionResult(
            editSession =
                currentState.editSession.withExpenseDraft(
                    currentState.editSession.expenseDraft.copy(expenseDate = expenseDate),
                ),
        )
    }

    private fun repeatManualDateInput(currentState: UserState.AwaitingExpenseDateEditManualInput): HandlerResponse =
        handlerResponse(
            messages =
                listOf(
                    outgoingMessage(
                        text = currentState.editSession.expenseDraft.toExpenseView(),
                        actions = emptyList(),
                    ),
                    outgoingMessage(
                        text = BotText.EnterExpenseDateManually,
                        actions = listOf(BotAction.ShowCancel),
                    ),
                ),
            nextState = currentState,
        )
}
