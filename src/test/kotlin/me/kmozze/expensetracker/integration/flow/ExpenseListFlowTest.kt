package me.kmozze.expensetracker.integration.flow

import me.kmozze.expensetracker.adapter.callback.CallbackData
import me.kmozze.expensetracker.handler.DialogueRouter
import me.kmozze.expensetracker.model.domain.bot.BotAction
import me.kmozze.expensetracker.model.domain.bot.BotText
import me.kmozze.expensetracker.model.domain.bot.ResponseDelivery
import me.kmozze.expensetracker.model.domain.bot.UserState
import me.kmozze.expensetracker.model.domain.expense.ExpenseListFilter
import me.kmozze.expensetracker.model.domain.expense.ExpenseListPage
import me.kmozze.expensetracker.model.domain.expense.ExpenseListPeriod
import me.kmozze.expensetracker.model.domain.expense.Money
import me.kmozze.expensetracker.model.entity.Category
import me.kmozze.expensetracker.model.entity.Expense
import me.kmozze.expensetracker.repository.ICategoryRepository
import me.kmozze.expensetracker.repository.IExpenseRepository
import me.kmozze.expensetracker.support.processUserInput
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.math.BigDecimal
import java.time.Clock
import java.time.LocalDate

class ExpenseListFlowTest : AbstractFlowIntegrationTest() {
    @Autowired
    private lateinit var dialogueRouter: DialogueRouter

    @Autowired
    private lateinit var categoryRepository: ICategoryRepository

    @Autowired
    private lateinit var expenseRepository: IExpenseRepository

    @Autowired
    private lateinit var clock: Clock

    @Test
    fun `view expenses opens settings sends snapshot list and opens full expense card`() {
        val userId = 2020L
        val chatId = 3020L
        val category =
            categoryRepository.create(
                Category(
                    name = "Транспорт",
                    userId = userId,
                ),
            )
        val expense =
            expenseRepository.create(
                Expense(
                    amount = EXPENSE_AMOUNT,
                    categoryId = category.id,
                    userId = userId,
                    expenseDate = LocalDate.now(clock),
                    description = EXPENSE_DESCRIPTION,
                ),
            )
        val defaultFilter = ExpenseListFilter(period = ExpenseListPeriod.Month)

        val settings =
            dialogueRouter.processUserInput(
                userId = userId,
                chatId = chatId,
                callbackData = CallbackData.menuViewExpenses(),
                callbackMessageId = MENU_MESSAGE_ID,
            )

        settings.assertSingleMessage(
            text = BotText.ExpenseListSettings(defaultFilter, categoryName = null),
            actions = listOf(BotAction.ShowExpenseListSettings(defaultFilter)),
            delivery = ResponseDelivery.SendNewMessage,
        )
        settings.assertNextState(UserState.Idle)

        val list =
            dialogueRouter.processUserInput(
                userId = userId,
                chatId = chatId,
                callbackData = CallbackData.showExpenseList(defaultFilter),
                callbackMessageId = SETTINGS_MESSAGE_ID,
            )
        val pageText = list.outgoingMessages.single().text as BotText.ExpenseListView
        val page = pageText.page

        assertThat(page.items.map { it.expenseId }).containsExactly(expense.id)
        assertThat(page.items.single().categoryName).isEqualTo(category.name)
        list.assertSingleMessage(
            text = BotText.ExpenseListView(page),
            actions = listOf(BotAction.ShowExpenseListPage(page)),
            delivery = ResponseDelivery.SendNewMessage,
        )
        assertThat(page)
            .extracting(ExpenseListPage::page, ExpenseListPage::pageSize, ExpenseListPage::totalCount)
            .containsExactly(0, 10, 1)

        val card =
            dialogueRouter.processUserInput(
                userId = userId,
                chatId = chatId,
                callbackData = CallbackData.openExpenseFromList(expense.id),
                callbackMessageId = LIST_MESSAGE_ID,
            )

        card.assertSingleMessage(
            text =
                BotText.ExpenseView(
                    amount = EXPENSE_AMOUNT,
                    categoryName = category.name,
                    expenseDate = expense.expenseDate,
                    description = EXPENSE_DESCRIPTION,
                ),
            actions = listOf(BotAction.ShowExpenseCardActions(expense.id)),
            delivery = ResponseDelivery.SendNewMessage,
        )
        card.assertNextState(UserState.Idle)
    }

    private companion object {
        const val MENU_MESSAGE_ID = 901
        const val SETTINGS_MESSAGE_ID = 902
        const val LIST_MESSAGE_ID = 903
        const val EXPENSE_DESCRIPTION = "такси"
        val EXPENSE_AMOUNT: Money = Money.of(BigDecimal("500.00"))
    }
}
