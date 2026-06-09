package me.kmozze.expensetracker.unit.handler.statehandler

import io.mockk.every
import io.mockk.junit5.MockKExtension
import io.mockk.mockk
import io.mockk.verify
import me.kmozze.expensetracker.handler.statehandler.ExpenseCardActionHandler
import me.kmozze.expensetracker.model.domain.BotAction
import me.kmozze.expensetracker.model.domain.BotText
import me.kmozze.expensetracker.model.domain.HandlerResponse
import me.kmozze.expensetracker.model.domain.Money
import me.kmozze.expensetracker.model.domain.ResponseDelivery
import me.kmozze.expensetracker.model.domain.UserCommand
import me.kmozze.expensetracker.model.domain.UserInput
import me.kmozze.expensetracker.model.domain.UserState
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
class ExpenseCardActionHandlerTest {
    private val expenseService: ExpenseService = mockk()
    private val categoryService: CategoryService = mockk()
    private lateinit var handler: ExpenseCardActionHandler

    @BeforeEach
    fun setUp() {
        handler =
            ExpenseCardActionHandler(
                expenseService = expenseService,
                categoryService = categoryService,
            )
    }

    @Test
    fun `request expense deletion edits card into deletion confirmation`() {
        every { expenseService.findExpenseForUser(USER_ID, EXPENSE_ID) } returns EXPENSE
        every { categoryService.findCategoryForUser(CATEGORY_ID, USER_ID) } returns CATEGORY

        val result =
            handler.requestExpenseDeletion(
                input = makeInput(UserCommand.RequestExpenseDeletion(EXPENSE_ID), callbackMessageId = MESSAGE_ID),
                expenseId = EXPENSE_ID,
            )

        assertSingleMessage(
            result = result,
            text = EXPENSE_VIEW,
            actions = listOf(BotAction.ShowExpenseDeletionConfirmation(EXPENSE_ID)),
            delivery = ResponseDelivery.EditMessage(MESSAGE_ID),
            nextState = UserState.Idle,
        )
        verify(exactly = 1) { expenseService.findExpenseForUser(USER_ID, EXPENSE_ID) }
        verify(exactly = 1) { categoryService.findCategoryForUser(CATEGORY_ID, USER_ID) }
    }

    @Test
    fun `request expense deletion sends a new message without callback message id`() {
        every { expenseService.findExpenseForUser(USER_ID, EXPENSE_ID) } returns EXPENSE
        every { categoryService.findCategoryForUser(CATEGORY_ID, USER_ID) } returns CATEGORY

        val result =
            handler.requestExpenseDeletion(
                input = makeInput(UserCommand.RequestExpenseDeletion(EXPENSE_ID)),
                expenseId = EXPENSE_ID,
            )

        assertSingleMessage(
            result = result,
            text = EXPENSE_VIEW,
            actions = listOf(BotAction.ShowExpenseDeletionConfirmation(EXPENSE_ID)),
            delivery = ResponseDelivery.SendNewMessage,
            nextState = UserState.Idle,
        )
    }

    @Test
    fun `request expense deletion returns unavailable when expense is missing`() {
        every { expenseService.findExpenseForUser(USER_ID, EXPENSE_ID) } returns null

        val result =
            handler.requestExpenseDeletion(
                input = makeInput(UserCommand.RequestExpenseDeletion(EXPENSE_ID), callbackMessageId = MESSAGE_ID),
                expenseId = EXPENSE_ID,
            )

        assertSingleMessage(
            result = result,
            text = BotText.ExpenseUnavailable,
            actions = listOf(BotAction.ClearInlineKeyboard),
            delivery = ResponseDelivery.EditMessage(MESSAGE_ID),
            nextState = UserState.Idle,
        )
        verify(exactly = 1) { expenseService.findExpenseForUser(USER_ID, EXPENSE_ID) }
    }

    @Test
    fun `request expense deletion returns unavailable when category is missing`() {
        every { expenseService.findExpenseForUser(USER_ID, EXPENSE_ID) } returns EXPENSE
        every { categoryService.findCategoryForUser(CATEGORY_ID, USER_ID) } returns null

        val result =
            handler.requestExpenseDeletion(
                input = makeInput(UserCommand.RequestExpenseDeletion(EXPENSE_ID), callbackMessageId = MESSAGE_ID),
                expenseId = EXPENSE_ID,
            )

        assertSingleMessage(
            result = result,
            text = BotText.ExpenseUnavailable,
            actions = listOf(BotAction.ClearInlineKeyboard),
            delivery = ResponseDelivery.EditMessage(MESSAGE_ID),
            nextState = UserState.Idle,
        )
        verify(exactly = 1) { expenseService.findExpenseForUser(USER_ID, EXPENSE_ID) }
        verify(exactly = 1) { categoryService.findCategoryForUser(CATEGORY_ID, USER_ID) }
    }

    @Test
    fun `request expense edit opens field selection`() {
        every { expenseService.findExpenseForUser(USER_ID, EXPENSE_ID) } returns EXPENSE
        every { categoryService.findCategoryForUser(CATEGORY_ID, USER_ID) } returns CATEGORY

        val result =
            handler.requestExpenseEdit(
                input = makeInput(UserCommand.RequestExpenseEdit(EXPENSE_ID), callbackMessageId = MESSAGE_ID),
                expenseId = EXPENSE_ID,
            )

        assertThat(result.outgoingMessages).hasSize(2)
        assertThat(result.outgoingMessages[0].text).isEqualTo(EXPENSE_VIEW)
        assertThat(result.outgoingMessages[0].actions).containsExactly(BotAction.ClearInlineKeyboard)
        assertThat(result.outgoingMessages[0].delivery).isEqualTo(ResponseDelivery.EditMessage(MESSAGE_ID))
        assertThat(result.outgoingMessages[1].text).isEqualTo(BotText.EditExpenseFieldSelection)
        assertThat(result.outgoingMessages[1].actions).containsExactly(BotAction.ShowExpenseEditFieldSelection)
        assertThat(result.outgoingMessages[1].delivery).isEqualTo(ResponseDelivery.SendNewMessage)
        assertThat(result.nextState)
            .isEqualTo(UserState.AwaitingExpenseEditFieldSelection(expenseId = EXPENSE_ID))
        verify(exactly = 1) { expenseService.findExpenseForUser(USER_ID, EXPENSE_ID) }
        verify(exactly = 1) { categoryService.findCategoryForUser(CATEGORY_ID, USER_ID) }
    }

    @Test
    fun `request expense edit sends a new card message without callback message id`() {
        every { expenseService.findExpenseForUser(USER_ID, EXPENSE_ID) } returns EXPENSE
        every { categoryService.findCategoryForUser(CATEGORY_ID, USER_ID) } returns CATEGORY

        val result =
            handler.requestExpenseEdit(
                input = makeInput(UserCommand.RequestExpenseEdit(EXPENSE_ID)),
                expenseId = EXPENSE_ID,
            )

        assertThat(result.outgoingMessages[0].delivery).isEqualTo(ResponseDelivery.SendNewMessage)
        assertThat(result.nextState)
            .isEqualTo(UserState.AwaitingExpenseEditFieldSelection(expenseId = EXPENSE_ID))
    }

    @Test
    fun `request expense edit returns unavailable when expense is missing`() {
        every { expenseService.findExpenseForUser(USER_ID, EXPENSE_ID) } returns null

        val result =
            handler.requestExpenseEdit(
                input = makeInput(UserCommand.RequestExpenseEdit(EXPENSE_ID), callbackMessageId = MESSAGE_ID),
                expenseId = EXPENSE_ID,
            )

        assertSingleMessage(
            result = result,
            text = BotText.ExpenseUnavailable,
            actions = listOf(BotAction.ClearInlineKeyboard),
            delivery = ResponseDelivery.EditMessage(MESSAGE_ID),
            nextState = UserState.Idle,
        )
        verify(exactly = 1) { expenseService.findExpenseForUser(USER_ID, EXPENSE_ID) }
    }

    @Test
    fun `request expense edit returns unavailable when category is missing`() {
        every { expenseService.findExpenseForUser(USER_ID, EXPENSE_ID) } returns EXPENSE
        every { categoryService.findCategoryForUser(CATEGORY_ID, USER_ID) } returns null

        val result =
            handler.requestExpenseEdit(
                input = makeInput(UserCommand.RequestExpenseEdit(EXPENSE_ID), callbackMessageId = MESSAGE_ID),
                expenseId = EXPENSE_ID,
            )

        assertSingleMessage(
            result = result,
            text = BotText.ExpenseUnavailable,
            actions = listOf(BotAction.ClearInlineKeyboard),
            delivery = ResponseDelivery.EditMessage(MESSAGE_ID),
            nextState = UserState.Idle,
        )
        verify(exactly = 1) { expenseService.findExpenseForUser(USER_ID, EXPENSE_ID) }
        verify(exactly = 1) { categoryService.findCategoryForUser(CATEGORY_ID, USER_ID) }
    }

    @Test
    fun `cancel expense deletion restores card actions`() {
        every { expenseService.findExpenseForUser(USER_ID, EXPENSE_ID) } returns EXPENSE
        every { categoryService.findCategoryForUser(CATEGORY_ID, USER_ID) } returns CATEGORY

        val result =
            handler.cancelExpenseDeletion(
                input = makeInput(UserCommand.CancelExpenseDeletion(EXPENSE_ID), callbackMessageId = MESSAGE_ID),
                expenseId = EXPENSE_ID,
            )

        assertSingleMessage(
            result = result,
            text = EXPENSE_VIEW,
            actions = listOf(BotAction.ShowExpenseCardActions(EXPENSE_ID)),
            delivery = ResponseDelivery.EditMessage(MESSAGE_ID),
            nextState = UserState.Idle,
        )
        verify(exactly = 1) { expenseService.findExpenseForUser(USER_ID, EXPENSE_ID) }
        verify(exactly = 1) { categoryService.findCategoryForUser(CATEGORY_ID, USER_ID) }
    }

    @Test
    fun `cancel expense deletion returns unavailable when expense is missing`() {
        every { expenseService.findExpenseForUser(USER_ID, EXPENSE_ID) } returns null

        val result =
            handler.cancelExpenseDeletion(
                input = makeInput(UserCommand.CancelExpenseDeletion(EXPENSE_ID), callbackMessageId = MESSAGE_ID),
                expenseId = EXPENSE_ID,
            )

        assertSingleMessage(
            result = result,
            text = BotText.ExpenseUnavailable,
            actions = listOf(BotAction.ClearInlineKeyboard),
            delivery = ResponseDelivery.EditMessage(MESSAGE_ID),
            nextState = UserState.Idle,
        )
        verify(exactly = 1) { expenseService.findExpenseForUser(USER_ID, EXPENSE_ID) }
    }

    @Test
    fun `cancel expense deletion returns unavailable when category is missing`() {
        every { expenseService.findExpenseForUser(USER_ID, EXPENSE_ID) } returns EXPENSE
        every { categoryService.findCategoryForUser(CATEGORY_ID, USER_ID) } returns null

        val result =
            handler.cancelExpenseDeletion(
                input = makeInput(UserCommand.CancelExpenseDeletion(EXPENSE_ID), callbackMessageId = MESSAGE_ID),
                expenseId = EXPENSE_ID,
            )

        assertSingleMessage(
            result = result,
            text = BotText.ExpenseUnavailable,
            actions = listOf(BotAction.ClearInlineKeyboard),
            delivery = ResponseDelivery.EditMessage(MESSAGE_ID),
            nextState = UserState.Idle,
        )
        verify(exactly = 1) { expenseService.findExpenseForUser(USER_ID, EXPENSE_ID) }
        verify(exactly = 1) { categoryService.findCategoryForUser(CATEGORY_ID, USER_ID) }
    }

    @Test
    fun `confirm expense deletion deletes expense and clears inline keyboard`() {
        every { expenseService.deleteExpenseForUser(USER_ID, EXPENSE_ID) } returns true

        val result =
            handler.confirmExpenseDeletion(
                input = makeInput(UserCommand.ConfirmExpenseDeletion(EXPENSE_ID), callbackMessageId = MESSAGE_ID),
                expenseId = EXPENSE_ID,
            )

        assertSingleMessage(
            result = result,
            text = BotText.ExpenseDeleted,
            actions = listOf(BotAction.ClearInlineKeyboard),
            delivery = ResponseDelivery.EditMessage(MESSAGE_ID),
            nextState = UserState.Idle,
        )
        verify(exactly = 1) { expenseService.deleteExpenseForUser(USER_ID, EXPENSE_ID) }
    }

    @Test
    fun `confirm expense deletion returns unavailable when expense was not deleted`() {
        every { expenseService.deleteExpenseForUser(USER_ID, EXPENSE_ID) } returns false

        val result =
            handler.confirmExpenseDeletion(
                input = makeInput(UserCommand.ConfirmExpenseDeletion(EXPENSE_ID), callbackMessageId = MESSAGE_ID),
                expenseId = EXPENSE_ID,
            )

        assertSingleMessage(
            result = result,
            text = BotText.ExpenseUnavailable,
            actions = listOf(BotAction.ClearInlineKeyboard),
            delivery = ResponseDelivery.EditMessage(MESSAGE_ID),
            nextState = UserState.Idle,
        )
        verify(exactly = 1) { expenseService.deleteExpenseForUser(USER_ID, EXPENSE_ID) }
    }

    @Test
    fun `confirm expense deletion sends a new message without callback message id`() {
        every { expenseService.deleteExpenseForUser(USER_ID, EXPENSE_ID) } returns true

        val result =
            handler.confirmExpenseDeletion(
                input = makeInput(UserCommand.ConfirmExpenseDeletion(EXPENSE_ID)),
                expenseId = EXPENSE_ID,
            )

        assertSingleMessage(
            result = result,
            text = BotText.ExpenseDeleted,
            actions = listOf(BotAction.ClearInlineKeyboard),
            delivery = ResponseDelivery.SendNewMessage,
            nextState = UserState.Idle,
        )
    }

    private fun assertSingleMessage(
        result: HandlerResponse,
        text: BotText,
        actions: List<BotAction>,
        delivery: ResponseDelivery,
        nextState: UserState,
    ) {
        assertThat(
            result.outgoingMessages
                .single()
                .text,
        ).isEqualTo(text)
        assertThat(
            result.outgoingMessages
                .single()
                .actions,
        ).containsExactlyElementsOf(actions)
        assertThat(
            result.outgoingMessages
                .single()
                .delivery,
        ).isEqualTo(delivery)
        assertThat(result.nextState).isEqualTo(nextState)
    }

    private fun makeInput(
        command: UserCommand,
        callbackMessageId: Int? = null,
    ): UserInput =
        makeUserInput(
            userId = USER_ID,
            chatId = CHAT_ID,
            callbackMessageId = callbackMessageId,
            command = command,
        )

    private companion object {
        const val USER_ID = 123L
        const val CHAT_ID = 456L
        const val MESSAGE_ID = 789
        const val EXPENSE_DESCRIPTION = "такси"
        val EXPENSE_ID: UUID = UUID.fromString("00000000-0000-0000-0000-000000000001")
        val CATEGORY_ID: UUID = UUID.fromString("00000000-0000-0000-0000-000000000002")
        val EXPENSE_AMOUNT: Money = Money.of(BigDecimal("500.00"))
        val EXPENSE_DATE: LocalDate = LocalDate.parse("2026-05-24")
        val CATEGORY: Category =
            Category(
                id = CATEGORY_ID,
                name = "Транспорт",
                userId = USER_ID,
            )
        val EXPENSE: Expense =
            Expense(
                id = EXPENSE_ID,
                categoryId = CATEGORY_ID,
                amount = EXPENSE_AMOUNT,
                userId = USER_ID,
                expenseDate = EXPENSE_DATE,
                description = EXPENSE_DESCRIPTION,
            )
        val EXPENSE_VIEW: BotText.ExpenseView =
            BotText.ExpenseView(
                amount = EXPENSE_AMOUNT,
                categoryName = CATEGORY.name,
                expenseDate = EXPENSE_DATE,
                description = EXPENSE_DESCRIPTION,
            )
    }
}
