package me.kmozze.expensetracker.handler.statehandler

import me.kmozze.expensetracker.exception.BusinessException
import me.kmozze.expensetracker.handler.handlerResponse
import me.kmozze.expensetracker.handler.outgoingMessage
import me.kmozze.expensetracker.model.domain.BotAction
import me.kmozze.expensetracker.model.domain.BotText
import me.kmozze.expensetracker.model.domain.HandlerResponse
import me.kmozze.expensetracker.model.domain.UserCommand
import me.kmozze.expensetracker.model.domain.UserInput
import me.kmozze.expensetracker.model.domain.UserState
import me.kmozze.expensetracker.service.CategoryService
import me.kmozze.expensetracker.service.ExpenseService
import org.springframework.stereotype.Component
import kotlin.reflect.KClass

@Component
class AwaitingExpenseAmountEditHandler(
    private val expenseService: ExpenseService,
    private val categoryService: CategoryService,
) : StateHandler {
    override val supportedStateClass: KClass<out UserState> = UserState.AwaitingExpenseAmountEdit::class

    override fun handle(
        input: UserInput,
        currentState: UserState,
    ): HandlerResponse {
        require(currentState is UserState.AwaitingExpenseAmountEdit) {
            "AwaitingExpenseAmountEditHandler requires AwaitingExpenseAmountEdit state"
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

            is UserCommand.PlainText -> saveAmount(currentState, command.value)
            else -> repeatAmountInput(currentState)
        }
    }

    private fun saveAmount(
        currentState: UserState.AwaitingExpenseAmountEdit,
        amountText: String,
    ): HandlerResponse {
        val amount =
            try {
                expenseService.parseExpenseAmount(amountText)
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
                    currentState.editSession.expenseDraft.copy(amount = amount),
                ),
        )
    }

    private fun repeatAmountInput(currentState: UserState.AwaitingExpenseAmountEdit): HandlerResponse =
        handlerResponse(
            message =
                outgoingMessage(
                    text = BotText.EnterExpenseAmount,
                    actions = listOf(BotAction.ShowCancel),
                ),
            nextState = currentState,
        )
}
