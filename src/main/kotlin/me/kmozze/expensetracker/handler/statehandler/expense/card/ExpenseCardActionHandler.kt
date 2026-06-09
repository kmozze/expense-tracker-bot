package me.kmozze.expensetracker.handler.statehandler.expense.card

import me.kmozze.expensetracker.handler.handlerResponse
import me.kmozze.expensetracker.handler.outgoingMessage
import me.kmozze.expensetracker.handler.statehandler.expense.toExpenseDraft
import me.kmozze.expensetracker.handler.statehandler.expense.toExpenseView
import me.kmozze.expensetracker.model.domain.bot.BotAction
import me.kmozze.expensetracker.model.domain.bot.BotText
import me.kmozze.expensetracker.model.domain.bot.HandlerResponse
import me.kmozze.expensetracker.model.domain.bot.ResponseDelivery
import me.kmozze.expensetracker.model.domain.bot.UserInput
import me.kmozze.expensetracker.model.domain.bot.UserState
import me.kmozze.expensetracker.model.domain.expense.ExpenseEditSession
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

        val expenseDraft = expense.toExpenseDraft(category.name)
        val editSession =
            ExpenseEditSession(
                expenseId = expense.id,
                expenseDraft = expenseDraft,
            )

        return handlerResponse(
            messages =
                listOf(
                    outgoingMessage(
                        text =
                            expenseDraft.toExpenseView(),
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
                    editSession = editSession,
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
                expense.toExpenseView(category.name)
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
                expense.toExpenseView(category.name)
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
