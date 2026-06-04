package me.kmozze.expensetracker.handler.statehandler

import me.kmozze.expensetracker.model.domain.BotAction
import me.kmozze.expensetracker.model.domain.BotText
import me.kmozze.expensetracker.model.domain.HandlerResponse
import me.kmozze.expensetracker.model.domain.HandlerResult
import me.kmozze.expensetracker.model.domain.OutgoingMessage
import me.kmozze.expensetracker.model.domain.ResponseDelivery
import me.kmozze.expensetracker.model.domain.UserCommand
import me.kmozze.expensetracker.model.domain.UserInput
import me.kmozze.expensetracker.model.domain.UserState
import me.kmozze.expensetracker.model.entity.Category
import me.kmozze.expensetracker.model.entity.Expense
import me.kmozze.expensetracker.service.CategoryService
import me.kmozze.expensetracker.service.ExpenseService
import org.springframework.stereotype.Component
import java.util.UUID
import kotlin.reflect.KClass

@Component
class IdleStateHandler(
    private val expenseService: ExpenseService,
    private val categoryService: CategoryService,
) : StateHandler {
    override val supportedStateClass: KClass<out UserState> = UserState.Idle::class

    override fun handle(
        input: UserInput,
        currentState: UserState,
    ): HandlerResult {
        require(currentState is UserState.Idle) {
            "IdleStateHandler requires Idle state"
        }

        return when (val command = input.command) {
            UserCommand.AddExpense ->
                HandlerResult(
                    response =
                        HandlerResponse(
                            outgoingMessages =
                                listOf(
                                    OutgoingMessage(
                                        text = BotText.AddExpenseInstructions,
                                        actions = emptyList(),
                                    ),
                                ),
                        ),
                    nextState = UserState.AwaitingExpenseInput,
                )

            UserCommand.ViewExpenses,
            UserCommand.Categories,
            UserCommand.Statistics,
            ->
                HandlerResult(
                    response =
                        HandlerResponse(
                            outgoingMessages =
                                listOf(
                                    OutgoingMessage(
                                        text = BotText.FeatureInProgress,
                                        actions = emptyList(),
                                    ),
                                ),
                        ),
                    nextState = UserState.Idle,
                )

            is UserCommand.RequestExpenseEdit ->
                requestExpenseEdit(input, command.expenseId)

            is UserCommand.RequestExpenseDeletion ->
                requestExpenseDeletion(input, command.expenseId)

            is UserCommand.ConfirmExpenseDeletion ->
                confirmExpenseDeletion(input, command.expenseId)

            is UserCommand.CancelExpenseDeletion ->
                cancelExpenseDeletion(input, command.expenseId)

            UserCommand.Cancel,
            UserCommand.InvalidExpenseAction,
            ->
                HandlerResult(
                    response =
                        HandlerResponse(
                            outgoingMessages =
                                listOf(
                                    OutgoingMessage(
                                        text = BotText.SelectionExpired,
                                        actions = listOf(BotAction.ShowMainMenu),
                                    ),
                                ),
                        ),
                    nextState = UserState.Idle,
                )

            else ->
                HandlerResult(
                    response =
                        HandlerResponse(
                            outgoingMessages =
                                listOf(
                                    OutgoingMessage(
                                        text = BotText.UnknownCommand,
                                        actions = listOf(BotAction.ShowMainMenu),
                                    ),
                                ),
                        ),
                    nextState = UserState.Idle,
                )
        }
    }

    private fun requestExpenseEdit(
        input: UserInput,
        expenseId: UUID,
    ): HandlerResult {
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

        return HandlerResult(
            response =
                HandlerResponse(
                    outgoingMessages =
                        listOf(
                            OutgoingMessage(
                                text =
                                    expense.toExpenseView(category),
                                actions = listOf(BotAction.ClearInlineKeyboard),
                                delivery = input.callbackMessageDelivery(),
                            ),
                            OutgoingMessage(
                                text = BotText.EditExpenseFieldSelection,
                                actions = listOf(BotAction.ShowExpenseEditFieldSelection),
                            ),
                        ),
                ),
            nextState = UserState.AwaitingExpenseEditFieldSelection(expense.id),
        )
    }

    private fun requestExpenseDeletion(
        input: UserInput,
        expenseId: UUID,
    ): HandlerResult =
        expenseCardResult(
            input = input,
            expenseId = expenseId,
            textFactory = { expense, category ->
                expense.toExpenseView(category)
            },
            actionsFactory = { listOf(BotAction.ShowExpenseDeletionConfirmation(expenseId)) },
        )

    private fun cancelExpenseDeletion(
        input: UserInput,
        expenseId: UUID,
    ): HandlerResult =
        expenseCardResult(
            input = input,
            expenseId = expenseId,
            textFactory = { expense, category ->
                expense.toExpenseView(category)
            },
            actionsFactory = { listOf(BotAction.ShowExpenseCardActions(expenseId)) },
        )

    private fun confirmExpenseDeletion(
        input: UserInput,
        expenseId: UUID,
    ): HandlerResult {
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
    ): HandlerResult {
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

    private fun expenseUnavailableResult(input: UserInput): HandlerResult =
        editExpenseCardResult(
            input = input,
            text = BotText.ExpenseUnavailable,
            actions = listOf(BotAction.ClearInlineKeyboard),
        )

    private fun editExpenseCardResult(
        input: UserInput,
        text: BotText,
        actions: List<BotAction>,
    ): HandlerResult =
        HandlerResult(
            response =
                HandlerResponse(
                    outgoingMessages =
                        listOf(
                            OutgoingMessage(
                                text = text,
                                actions = actions,
                                delivery = input.callbackMessageDelivery(),
                            ),
                        ),
                ),
            nextState = UserState.Idle,
        )

    private fun UserInput.callbackMessageDelivery(): ResponseDelivery =
        callbackMessageId
            ?.let { ResponseDelivery.EditMessage(it) }
            ?: ResponseDelivery.SendNewMessage
}
