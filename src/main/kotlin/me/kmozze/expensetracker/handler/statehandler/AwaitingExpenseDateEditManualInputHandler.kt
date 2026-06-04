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
                    expenseId = currentState.expenseId,
                )

            is UserCommand.PlainText -> updateDate(input, currentState, command.value)
            else -> repeatManualDateInput(input, currentState)
        }
    }

    private fun updateDate(
        input: UserInput,
        currentState: UserState.AwaitingExpenseDateEditManualInput,
        dateText: String,
    ): HandlerResponse {
        val expenseDate =
            try {
                expenseService.parseExpenseDate(dateText)
            } catch (e: BusinessException) {
                return HandlerResponse(
                    outgoingMessages =
                        listOf(
                            OutgoingMessage(
                                text = BotText.Error(e.errorCode),
                                actions = listOf(BotAction.ShowCancel),
                            ),
                        ),
                    nextState = currentState,
                )
            }

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
            prefixText = BotText.ExpenseSaved,
        )
    }

    private fun repeatManualDateInput(
        input: UserInput,
        currentState: UserState.AwaitingExpenseDateEditManualInput,
    ): HandlerResponse {
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

        return HandlerResponse(
            outgoingMessages =
                listOf(
                    OutgoingMessage(
                        text = expense.toExpenseView(category),
                        actions = emptyList(),
                    ),
                    OutgoingMessage(
                        text = BotText.EnterExpenseDateManually,
                        actions = listOf(BotAction.ShowCancel),
                    ),
                ),
            nextState = currentState,
        )
    }
}
