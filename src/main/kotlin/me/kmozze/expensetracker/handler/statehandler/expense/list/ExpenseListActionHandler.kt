package me.kmozze.expensetracker.handler.statehandler.expense.list

import me.kmozze.expensetracker.handler.callbackAnswerResponse
import me.kmozze.expensetracker.handler.handlerResponse
import me.kmozze.expensetracker.handler.outgoingMessage
import me.kmozze.expensetracker.handler.statehandler.expense.toExpenseView
import me.kmozze.expensetracker.model.domain.bot.BotAction
import me.kmozze.expensetracker.model.domain.bot.BotText
import me.kmozze.expensetracker.model.domain.bot.CallbackAnswer
import me.kmozze.expensetracker.model.domain.bot.HandlerResponse
import me.kmozze.expensetracker.model.domain.bot.ResponseDelivery
import me.kmozze.expensetracker.model.domain.bot.UserInput
import me.kmozze.expensetracker.model.domain.bot.UserState
import me.kmozze.expensetracker.model.domain.expense.ExpenseListCategoryOption
import me.kmozze.expensetracker.model.domain.expense.ExpenseListFilter
import me.kmozze.expensetracker.model.domain.expense.ExpenseListPeriod
import me.kmozze.expensetracker.service.CategoryService
import me.kmozze.expensetracker.service.ExpenseService
import org.springframework.stereotype.Component
import java.util.UUID

@Component
class ExpenseListActionHandler(
    private val expenseService: ExpenseService,
    private val categoryService: CategoryService,
) {
    fun openSettings(input: UserInput): HandlerResponse =
        settingsResult(
            input = input,
            filter = ExpenseListFilter(period = ExpenseListPeriod.Month),
        )

    fun requestPeriodSelection(
        input: UserInput,
        filter: ExpenseListFilter,
    ): HandlerResponse {
        if (!isFilterAvailable(input, filter)) {
            return staleSelectionResult()
        }

        return handlerResponse(
            message =
                outgoingMessage(
                    text = BotText.ExpenseListPeriodSelection,
                    actions = listOf(BotAction.ShowExpenseListPeriodSelection(filter)),
                    delivery = input.callbackMessageDelivery(),
                ),
            nextState = UserState.Idle,
        )
    }

    fun requestCategorySelection(
        input: UserInput,
        filter: ExpenseListFilter,
    ): HandlerResponse {
        if (!isFilterAvailable(input, filter)) {
            return staleSelectionResult()
        }

        val categoryOptions =
            categoryService
                .getCategories(input.userId)
                .map { category ->
                    ExpenseListCategoryOption(
                        categoryId = category.id,
                        name = category.name,
                    )
                }

        return handlerResponse(
            message =
                outgoingMessage(
                    text = BotText.ExpenseListCategorySelection,
                    actions = listOf(BotAction.ShowExpenseListCategorySelection(filter, categoryOptions)),
                    delivery = input.callbackMessageDelivery(),
                ),
            nextState = UserState.Idle,
        )
    }

    fun selectPeriod(
        input: UserInput,
        filter: ExpenseListFilter,
    ): HandlerResponse =
        settingsResult(
            input = input,
            filter = filter,
        )

    fun selectCategory(
        input: UserInput,
        filter: ExpenseListFilter,
    ): HandlerResponse =
        settingsResult(
            input = input,
            filter = filter,
        )

    fun showExpenseList(
        input: UserInput,
        filter: ExpenseListFilter,
        page: Int,
        shouldEditCurrentMessage: Boolean,
    ): HandlerResponse {
        if (!isFilterAvailable(input, filter)) {
            return staleSelectionResult()
        }

        val expenseListPage =
            expenseService.getExpenseListPageForUser(
                userId = input.userId,
                filter = filter,
                page = page,
                pageSize = EXPENSE_LIST_PAGE_SIZE,
            )
        val delivery =
            if (shouldEditCurrentMessage) {
                input.callbackMessageDelivery()
            } else {
                ResponseDelivery.SendNewMessage
            }

        return handlerResponse(
            message =
                outgoingMessage(
                    text = BotText.ExpenseListView(expenseListPage),
                    actions = listOf(BotAction.ShowExpenseListPage(expenseListPage)),
                    delivery = delivery,
                ),
            nextState = UserState.Idle,
        )
    }

    fun openExpenseFromList(
        input: UserInput,
        expenseId: UUID,
    ): HandlerResponse {
        val expense =
            expenseService.findExpenseForUser(
                userId = input.userId,
                expenseId = expenseId,
            ) ?: return expenseUnavailableAlert()

        val category =
            categoryService.findCategoryForUser(
                categoryId = expense.categoryId,
                userId = input.userId,
            ) ?: return expenseUnavailableAlert()

        return handlerResponse(
            message =
                outgoingMessage(
                    text = expense.toExpenseView(category.name),
                    actions = listOf(BotAction.ShowExpenseCardActions(expenseId)),
                    delivery = ResponseDelivery.SendNewMessage,
                ),
            nextState = UserState.Idle,
        )
    }

    fun invalidListAction(): HandlerResponse = staleSelectionResult()

    private fun settingsResult(
        input: UserInput,
        filter: ExpenseListFilter,
    ): HandlerResponse {
        val categoryName =
            filter.categoryId?.let { categoryId ->
                categoryService
                    .findCategoryForUser(
                        categoryId = categoryId,
                        userId = input.userId,
                    )?.name ?: return staleSelectionResult()
            }

        return handlerResponse(
            message =
                outgoingMessage(
                    text = BotText.ExpenseListSettings(filter, categoryName),
                    actions = listOf(BotAction.ShowExpenseListSettings(filter)),
                    delivery = input.callbackMessageDelivery(),
                ),
            nextState = UserState.Idle,
        )
    }

    private fun isFilterAvailable(
        input: UserInput,
        filter: ExpenseListFilter,
    ): Boolean =
        filter.categoryId == null ||
            categoryService.findCategoryForUser(
                categoryId = filter.categoryId,
                userId = input.userId,
            ) != null

    private fun staleSelectionResult(): HandlerResponse =
        callbackAnswerResponse(
            callbackAnswer = CallbackAnswer(text = BotText.SelectionExpired, showAlert = true),
        )

    private fun expenseUnavailableAlert(): HandlerResponse =
        callbackAnswerResponse(
            callbackAnswer = CallbackAnswer(text = BotText.ExpenseUnavailable, showAlert = true),
        )

    private fun UserInput.callbackMessageDelivery(): ResponseDelivery =
        callbackMessageId
            ?.let { ResponseDelivery.EditMessage(it) }
            ?: ResponseDelivery.SendNewMessage

    private companion object {
        const val EXPENSE_LIST_PAGE_SIZE = 10
    }
}
