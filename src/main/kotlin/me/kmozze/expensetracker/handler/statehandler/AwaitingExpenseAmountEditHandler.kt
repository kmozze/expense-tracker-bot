package me.kmozze.expensetracker.handler.statehandler

import me.kmozze.expensetracker.exception.BusinessException
import me.kmozze.expensetracker.model.domain.BotAction
import me.kmozze.expensetracker.model.domain.BotText
import me.kmozze.expensetracker.model.domain.HandlerResponse
import me.kmozze.expensetracker.model.domain.OutgoingMessage
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
                    expenseId = currentState.expenseId,
                )

            is UserCommand.PlainText -> saveAmount(input, currentState, command.value)
            else -> repeatAmountInput(currentState)
        }
    }

    private fun saveAmount(
        input: UserInput,
        currentState: UserState.AwaitingExpenseAmountEdit,
        amountText: String,
    ): HandlerResponse {
        val amount =
            try {
                expenseService.parseExpenseAmount(amountText)
            } catch (e: BusinessException) {
                return HandlerResponse(
                    outgoingMessages =
                        listOf(
                            OutgoingMessage(
                                text = BotText.Error(e.errorCode),
                                actions = listOf(BotAction.ShowCancel),
                            ),
                        ),
                    nextState = UserState.AwaitingExpenseAmountEdit(currentState.expenseId),
                )
            }

        val updatedExpense = expenseService.updateExpenseAmountForUser(input.userId, currentState.expenseId, amount)
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

    private fun repeatAmountInput(currentState: UserState.AwaitingExpenseAmountEdit): HandlerResponse =
        HandlerResponse(
            outgoingMessages =
                listOf(
                    OutgoingMessage(
                        text = BotText.EnterExpenseAmount,
                        actions = listOf(BotAction.ShowCancel),
                    ),
                ),
            nextState = currentState,
        )
}
