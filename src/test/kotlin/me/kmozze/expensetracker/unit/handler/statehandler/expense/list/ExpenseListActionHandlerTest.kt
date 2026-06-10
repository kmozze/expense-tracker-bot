package me.kmozze.expensetracker.unit.handler.statehandler.expense.list

import io.mockk.every
import io.mockk.junit5.MockKExtension
import io.mockk.mockk
import io.mockk.verify
import me.kmozze.expensetracker.handler.statehandler.expense.list.ExpenseListActionHandler
import me.kmozze.expensetracker.model.domain.bot.BotAction
import me.kmozze.expensetracker.model.domain.bot.BotText
import me.kmozze.expensetracker.model.domain.bot.CallbackAnswer
import me.kmozze.expensetracker.model.domain.bot.HandlerResponse
import me.kmozze.expensetracker.model.domain.bot.ResponseDelivery
import me.kmozze.expensetracker.model.domain.bot.UserCommand
import me.kmozze.expensetracker.model.domain.bot.UserState
import me.kmozze.expensetracker.model.domain.expense.ExpenseListCategoryOption
import me.kmozze.expensetracker.model.domain.expense.ExpenseListFilter
import me.kmozze.expensetracker.model.domain.expense.ExpenseListItem
import me.kmozze.expensetracker.model.domain.expense.ExpenseListPage
import me.kmozze.expensetracker.model.domain.expense.ExpenseListPeriod
import me.kmozze.expensetracker.model.domain.expense.Money
import me.kmozze.expensetracker.model.entity.Category
import me.kmozze.expensetracker.model.entity.Expense
import me.kmozze.expensetracker.service.CategoryService
import me.kmozze.expensetracker.service.ExpenseService
import me.kmozze.expensetracker.support.makeUserInput
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import java.math.BigDecimal
import java.time.LocalDate
import java.util.UUID

@ExtendWith(MockKExtension::class)
class ExpenseListActionHandlerTest {
    private val expenseService: ExpenseService = mockk()
    private val categoryService: CategoryService = mockk()
    private lateinit var handler: ExpenseListActionHandler

    @BeforeEach
    fun setUp() {
        handler =
            ExpenseListActionHandler(
                expenseService = expenseService,
                categoryService = categoryService,
            )
    }

    @Test
    fun `open settings edits callback message with default month and all categories`() {
        val input = makeInput(UserCommand.ViewExpenses, callbackMessageId = MESSAGE_ID)

        val result = handler.openSettings(input)

        assertSingleMessage(
            result = result,
            text = BotText.ExpenseListSettings(DEFAULT_FILTER, categoryName = null),
            actions = listOf(BotAction.ShowExpenseListSettings(DEFAULT_FILTER)),
            delivery = ResponseDelivery.EditMessage(MESSAGE_ID),
            nextState = UserState.Idle,
        )
    }

    @Test
    fun `request category selection loads category options and edits settings message`() {
        every { categoryService.getCategories(USER_ID) } returns listOf(CATEGORY)

        val result =
            handler.requestCategorySelection(
                input = makeInput(UserCommand.RequestExpenseListCategorySelection(DEFAULT_FILTER), callbackMessageId = MESSAGE_ID),
                filter = DEFAULT_FILTER,
            )

        assertSingleMessage(
            result = result,
            text = BotText.ExpenseListCategorySelection,
            actions =
                listOf(
                    BotAction.ShowExpenseListCategorySelection(
                        filter = DEFAULT_FILTER,
                        categories = listOf(ExpenseListCategoryOption(CATEGORY_ID, CATEGORY.name)),
                    ),
                ),
            delivery = ResponseDelivery.EditMessage(MESSAGE_ID),
            nextState = UserState.Idle,
        )
        verify(exactly = 1) { categoryService.getCategories(USER_ID) }
    }

    @Test
    fun `show list from settings sends new snapshot message`() {
        every { expenseService.getExpenseListPageForUser(USER_ID, DEFAULT_FILTER, page = 0, pageSize = 10) } returns LIST_PAGE

        val result =
            handler.showExpenseList(
                input = makeInput(UserCommand.ShowExpenseList(DEFAULT_FILTER, page = 0), callbackMessageId = MESSAGE_ID),
                filter = DEFAULT_FILTER,
                page = 0,
                shouldEditCurrentMessage = false,
            )

        assertSingleMessage(
            result = result,
            text = BotText.ExpenseListView(LIST_PAGE),
            actions = listOf(BotAction.ShowExpenseListPage(LIST_PAGE)),
            delivery = ResponseDelivery.SendNewMessage,
            nextState = UserState.Idle,
        )
    }

    @Test
    fun `pagination edits current list message after live requery`() {
        val secondPage = LIST_PAGE.copy(page = 1)
        every { expenseService.getExpenseListPageForUser(USER_ID, DEFAULT_FILTER, page = 1, pageSize = 10) } returns secondPage

        val result =
            handler.showExpenseList(
                input =
                    makeInput(
                        UserCommand.ShowExpenseList(DEFAULT_FILTER, page = 1, shouldEditCurrentMessage = true),
                        callbackMessageId = MESSAGE_ID,
                    ),
                filter = DEFAULT_FILTER,
                page = 1,
                shouldEditCurrentMessage = true,
            )

        assertSingleMessage(
            result = result,
            text = BotText.ExpenseListView(secondPage),
            actions = listOf(BotAction.ShowExpenseListPage(secondPage)),
            delivery = ResponseDelivery.EditMessage(MESSAGE_ID),
            nextState = UserState.Idle,
        )
    }

    @Test
    fun `open expense from list sends new actual expense card`() {
        every { expenseService.findExpenseForUser(USER_ID, EXPENSE_ID) } returns EXPENSE
        every { categoryService.findCategoryForUser(CATEGORY_ID, USER_ID) } returns CATEGORY

        val result =
            handler.openExpenseFromList(
                input = makeInput(UserCommand.OpenExpenseFromList(EXPENSE_ID), callbackMessageId = MESSAGE_ID),
                expenseId = EXPENSE_ID,
            )

        assertSingleMessage(
            result = result,
            text =
                BotText.ExpenseView(
                    amount = EXPENSE.amount,
                    categoryName = CATEGORY.name,
                    expenseDate = EXPENSE.expenseDate,
                    description = EXPENSE.description,
                ),
            actions = listOf(BotAction.ShowExpenseCardActions(EXPENSE_ID)),
            delivery = ResponseDelivery.SendNewMessage,
            nextState = UserState.Idle,
        )
        assertThat(result.callbackAnswer).isNull()
    }

    @Test
    fun `open missing expense from list returns alert without outgoing messages or state change`() {
        every { expenseService.findExpenseForUser(USER_ID, EXPENSE_ID) } returns null

        val result =
            handler.openExpenseFromList(
                input = makeInput(UserCommand.OpenExpenseFromList(EXPENSE_ID), callbackMessageId = MESSAGE_ID),
                expenseId = EXPENSE_ID,
            )

        assertAlertOnly(result, CallbackAnswer(text = BotText.ExpenseUnavailable, showAlert = true))
    }

    @Test
    fun `invalid list action returns stale selection alert only`() {
        val result = handler.invalidListAction()

        assertAlertOnly(result, CallbackAnswer(text = BotText.SelectionExpired, showAlert = true))
    }

    private fun assertSingleMessage(
        result: HandlerResponse,
        text: BotText,
        actions: List<BotAction>,
        delivery: ResponseDelivery,
        nextState: UserState,
    ) {
        assertThat(result.outgoingMessages.single().text).isEqualTo(text)
        assertThat(result.outgoingMessages.single().actions).containsExactlyElementsOf(actions)
        assertThat(result.outgoingMessages.single().delivery).isEqualTo(delivery)
        assertThat(result.nextState).isEqualTo(nextState)
    }

    private fun assertAlertOnly(
        result: HandlerResponse,
        callbackAnswer: CallbackAnswer,
    ) {
        assertThat(result.outgoingMessages).isEmpty()
        assertThat(result.callbackAnswer).isEqualTo(callbackAnswer)
        assertThat(result.nextState).isNull()
    }

    private fun makeInput(
        command: UserCommand,
        callbackMessageId: Int? = null,
    ) = makeUserInput(
        userId = USER_ID,
        chatId = CHAT_ID,
        callbackMessageId = callbackMessageId,
        command = command,
    )

    private companion object {
        const val USER_ID = 123L
        const val CHAT_ID = 456L
        const val MESSAGE_ID = 789
        val EXPENSE_ID: UUID = UUID.fromString("00000000-0000-0000-0000-000000000001")
        val CATEGORY_ID: UUID = UUID.fromString("00000000-0000-0000-0000-000000000002")
        val DEFAULT_FILTER: ExpenseListFilter = ExpenseListFilter(period = ExpenseListPeriod.Month)
        val CATEGORY: Category =
            Category(
                id = CATEGORY_ID,
                name = "Транспорт",
                userId = USER_ID,
            )
        val EXPENSE_AMOUNT: Money = Money.of(BigDecimal("500.00"))
        val EXPENSE_DATE: LocalDate = LocalDate.parse("2026-05-24")
        val EXPENSE: Expense =
            Expense(
                id = EXPENSE_ID,
                categoryId = CATEGORY_ID,
                amount = EXPENSE_AMOUNT,
                userId = USER_ID,
                expenseDate = EXPENSE_DATE,
                description = "такси",
            )
        val LIST_PAGE: ExpenseListPage =
            ExpenseListPage(
                filter = DEFAULT_FILTER,
                items =
                    listOf(
                        ExpenseListItem(
                            expenseId = EXPENSE_ID,
                            expenseDate = EXPENSE_DATE,
                            categoryName = CATEGORY.name,
                            amount = EXPENSE_AMOUNT,
                        ),
                    ),
                page = 0,
                pageSize = 10,
                totalCount = 1,
            )
    }
}
