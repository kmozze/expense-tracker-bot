package me.kmozze.expensetracker.handler.statehandler

import me.kmozze.expensetracker.handler.handlerResponse
import me.kmozze.expensetracker.handler.outgoingMessage
import me.kmozze.expensetracker.model.domain.BotAction
import me.kmozze.expensetracker.model.domain.BotText
import me.kmozze.expensetracker.model.domain.ExpenseDraft
import me.kmozze.expensetracker.model.domain.HandlerResponse
import me.kmozze.expensetracker.model.domain.ResponseDelivery
import me.kmozze.expensetracker.model.domain.UserInput
import me.kmozze.expensetracker.model.domain.UserState
import me.kmozze.expensetracker.model.entity.Category
import me.kmozze.expensetracker.model.entity.Expense
import me.kmozze.expensetracker.service.CategoryService
import me.kmozze.expensetracker.service.ExpenseService
import org.springframework.stereotype.Component
import java.util.UUID

@Component
class ExpenseCardActionHandler(
    private val expenseService: ExpenseService,
    private val categoryService: CategoryService,
) {
    fun requestExpenseEdit(
        input: UserInput,
        expenseId: UUID,
    ): HandlerResponse {
        val expense =
            expenseService.findExpenseForUser(
                userId = input.userId,
                expenseId = expenseId,
            ) ?: return expenseUnavailableResult(input)

        val category =
            categoryService.findCategoryForUser(
                categoryId = expense.categoryId,
                userId = input.userId,
            ) ?: return expenseUnavailableResult(input)

        val expenseDraft =
            ExpenseDraft(
                amount = expense.amount,
                description = expense.description,
                categoryId = expense.categoryId,
                expenseDate = expense.expenseDate,
            )

        return handlerResponse(
            messages =
                listOf(
                    outgoingMessage(
                        text =
                            expense.toExpenseView(category),
                        actions = listOf(BotAction.ClearInlineKeyboard),
                        delivery = input.callbackMessageDelivery(),
                    ),
                    outgoingMessage(
                        text = BotText.EditExpenseFieldSelection,
                        actions = listOf(BotAction.ShowExpenseEditFieldSelection),
                    ),
                ),
            nextState =
                UserState.AwaitingExpenseEditFieldSelection(
                    expenseId = expense.id,
                    expenseDraft = expenseDraft,
                    categoryName = category.name,
                ),
        )
    }

    fun requestExpenseDeletion(
        input: UserInput,
        expenseId: UUID,
    ): HandlerResponse =
        expenseCardResult(
            input = input,
            expenseId = expenseId,
            textFactory = { expense, category ->
                expense.toExpenseView(category)
            },
            actionsFactory = { listOf(BotAction.ShowExpenseDeletionConfirmation(expenseId)) },
        )

    fun cancelExpenseDeletion(
        input: UserInput,
        expenseId: UUID,
    ): HandlerResponse =
        expenseCardResult(
            input = input,
            expenseId = expenseId,
            textFactory = { expense, category ->
                expense.toExpenseView(category)
            },
            actionsFactory = { listOf(BotAction.ShowExpenseCardActions(expenseId)) },
        )

    fun confirmExpenseDeletion(
        input: UserInput,
        expenseId: UUID,
    ): HandlerResponse {
        val deleted =
            expenseService.deleteExpenseForUser(
                userId = input.userId,
                expenseId = expenseId,
            )

        return editExpenseCardResult(
            input = input,
            text =
                if (deleted) {
                    BotText.ExpenseDeleted
                } else {
                    BotText.ExpenseUnavailable
                },
            actions = listOf(BotAction.ClearInlineKeyboard),
        )
    }

    private fun expenseCardResult(
        input: UserInput,
        expenseId: UUID,
        textFactory: (Expense, Category) -> BotText,
        actionsFactory: () -> List<BotAction>,
    ): HandlerResponse {
        val expense =
            expenseService.findExpenseForUser(
                userId = input.userId,
                expenseId = expenseId,
            ) ?: return expenseUnavailableResult(input)

        val category =
            categoryService.findCategoryForUser(
                categoryId = expense.categoryId,
                userId = input.userId,
            ) ?: return expenseUnavailableResult(input)

        return editExpenseCardResult(
            input = input,
            text = textFactory(expense, category),
            actions = actionsFactory(),
        )
    }

    private fun expenseUnavailableResult(input: UserInput): HandlerResponse =
        editExpenseCardResult(
            input = input,
            text = BotText.ExpenseUnavailable,
            actions = listOf(BotAction.ClearInlineKeyboard),
        )

    private fun editExpenseCardResult(
        input: UserInput,
        text: BotText,
        actions: List<BotAction>,
    ): HandlerResponse =
        handlerResponse(
            message =
                outgoingMessage(
                    text = text,
                    actions = actions,
                    delivery = input.callbackMessageDelivery(),
                ),
            nextState = UserState.Idle,
        )

    private fun UserInput.callbackMessageDelivery(): ResponseDelivery =
        callbackMessageId
            ?.let { ResponseDelivery.EditMessage(it) }
            ?: ResponseDelivery.SendNewMessage
}
