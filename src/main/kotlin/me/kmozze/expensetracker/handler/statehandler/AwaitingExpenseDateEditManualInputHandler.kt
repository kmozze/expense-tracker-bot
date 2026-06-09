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

            UserCommand.FinishExpenseEdit ->
                finishExpenseEdit(
                    expenseService = expenseService,
                    categoryService = categoryService,
                    userId = input.userId,
                    expenseId = currentState.expenseId,
                    expenseDraft = currentState.expenseDraft,
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
            expenseId = currentState.expenseId,
            expenseDraft = currentState.expenseDraft.copy(expenseDate = expenseDate),
            categoryName = currentState.categoryName,
        )
    }

    private fun repeatManualDateInput(currentState: UserState.AwaitingExpenseDateEditManualInput): HandlerResponse =
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
            nextState = currentState,
        )
}
