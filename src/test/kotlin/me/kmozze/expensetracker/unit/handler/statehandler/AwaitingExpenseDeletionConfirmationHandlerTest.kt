package me.kmozze.expensetracker.unit.handler.statehandler

import io.mockk.confirmVerified
import io.mockk.every
import io.mockk.junit5.MockKExtension
import io.mockk.mockk
import io.mockk.verify
import me.kmozze.expensetracker.handler.statehandler.AwaitingExpenseDeletionConfirmationHandler
import me.kmozze.expensetracker.model.domain.BotAction
import me.kmozze.expensetracker.model.domain.BotMessage
import me.kmozze.expensetracker.model.domain.Money
import me.kmozze.expensetracker.model.domain.ResponseDelivery
import me.kmozze.expensetracker.model.domain.UserCommand
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
class AwaitingExpenseDeletionConfirmationHandlerTest {
    private val expenseService: ExpenseService = mockk()
    private val categoryService: CategoryService = mockk()
    private lateinit var handler: AwaitingExpenseDeletionConfirmationHandler

    @BeforeEach
    fun setUp() {
        handler =
            AwaitingExpenseDeletionConfirmationHandler(
                expenseService = expenseService,
                categoryService = categoryService,
            )
    }

    @Test
    fun `confirm deletion deletes expense and returns idle`() {
        every { expenseService.deleteExpenseForUser(USER_ID, EXPENSE_ID) } returns true

        val result = handle(UserCommand.ConfirmExpenseDeletion(EXPENSE_ID))

        assertThat(result.response.message).isEqualTo(BotMessage.ExpenseDeleted)
        assertThat(result.response.actions).containsExactly(BotAction.ClearInlineKeyboard)
        assertThat(result.response.delivery).isEqualTo(ResponseDelivery.EditMessage(CARD_MESSAGE_ID))
        assertThat(result.nextState).isEqualTo(UserState.Idle)
        verify(exactly = 1) { expenseService.deleteExpenseForUser(USER_ID, EXPENSE_ID) }
        confirmVerified(expenseService, categoryService)
    }

    @Test
    fun `confirm deletion returns unavailable when expense was already removed`() {
        every { expenseService.deleteExpenseForUser(USER_ID, EXPENSE_ID) } returns false

        val result = handle(UserCommand.ConfirmExpenseDeletion(EXPENSE_ID))

        assertThat(result.response.message).isEqualTo(BotMessage.ExpenseUnavailable)
        assertThat(result.response.actions).containsExactly(BotAction.ClearInlineKeyboard)
        assertThat(result.nextState).isEqualTo(UserState.Idle)
        verify(exactly = 1) { expenseService.deleteExpenseForUser(USER_ID, EXPENSE_ID) }
        confirmVerified(expenseService, categoryService)
    }

    @Test
    fun `cancel deletion returns expense card and idle state`() {
        every { expenseService.findExpenseForUser(USER_ID, EXPENSE_ID) } returns expense()
        every { categoryService.findCategoryForUser(CATEGORY_ID, USER_ID) } returns category()

        val result = handle(UserCommand.CancelExpenseDeletion(EXPENSE_ID))

        assertThat(result.response.message)
            .isEqualTo(
                BotMessage.ExpenseSaved(
                    amount = EXPENSE_AMOUNT,
                    categoryName = CATEGORY_NAME,
                    expenseDate = EXPENSE_DATE,
                    description = EXPENSE_DESCRIPTION,
                ),
            )
        assertThat(result.response.actions).containsExactly(BotAction.ShowExpenseCardActions(EXPENSE_ID))
        assertThat(result.response.delivery).isEqualTo(ResponseDelivery.EditMessage(CARD_MESSAGE_ID))
        assertThat(result.nextState).isEqualTo(UserState.Idle)
        verify(exactly = 1) { expenseService.findExpenseForUser(USER_ID, EXPENSE_ID) }
        verify(exactly = 1) { categoryService.findCategoryForUser(CATEGORY_ID, USER_ID) }
        confirmVerified(expenseService, categoryService)
    }

    @Test
    fun `mismatched confirmation callback does not delete current expense`() {
        val result = handle(UserCommand.ConfirmExpenseDeletion(OTHER_EXPENSE_ID))

        assertThat(result.response.message).isEqualTo(BotMessage.SelectionExpired)
        assertThat(result.response.actions).containsExactly(BotAction.ClearInlineKeyboard)
        assertThat(result.response.delivery).isEqualTo(ResponseDelivery.EditMessage(CARD_MESSAGE_ID))
        assertThat(result.nextState).isEqualTo(AWAITING_DELETION_CONFIRMATION)
        confirmVerified(expenseService, categoryService)
    }

    @Test
    fun `non deletion command repeats confirmation`() {
        every { expenseService.findExpenseForUser(USER_ID, EXPENSE_ID) } returns expense()
        every { categoryService.findCategoryForUser(CATEGORY_ID, USER_ID) } returns category()

        val result =
            handler.handle(
                input =
                    makeUserInput(
                        userId = USER_ID,
                        chatId = CHAT_ID,
                        text = "500 такси",
                        command = UserCommand.PlainText("500 такси"),
                    ),
                currentState = AWAITING_DELETION_CONFIRMATION,
            )

        assertThat(result.response.message)
            .isEqualTo(
                BotMessage.ExpenseSaved(
                    amount = EXPENSE_AMOUNT,
                    categoryName = CATEGORY_NAME,
                    expenseDate = EXPENSE_DATE,
                    description = EXPENSE_DESCRIPTION,
                    showDeletionConfirmation = true,
                ),
            )
        assertThat(result.response.actions).containsExactly(BotAction.ShowExpenseDeletionConfirmation(EXPENSE_ID))
        assertThat(result.response.delivery).isEqualTo(ResponseDelivery.EditMessage(CARD_MESSAGE_ID))
        assertThat(result.nextState).isEqualTo(AWAITING_DELETION_CONFIRMATION)
        verify(exactly = 1) { expenseService.findExpenseForUser(USER_ID, EXPENSE_ID) }
        verify(exactly = 1) { categoryService.findCategoryForUser(CATEGORY_ID, USER_ID) }
        confirmVerified(expenseService, categoryService)
    }

    private fun handle(command: UserCommand) =
        handler.handle(
            input =
                makeUserInput(
                    userId = USER_ID,
                    chatId = CHAT_ID,
                    callbackData = "callback",
                    callbackMessageId = CARD_MESSAGE_ID,
                    command = command,
                ),
            currentState = AWAITING_DELETION_CONFIRMATION,
        )

    private companion object {
        const val USER_ID = 123L
        const val CHAT_ID = 456L
        const val CARD_MESSAGE_ID = 789
        const val CATEGORY_NAME = "Еда"
        const val EXPENSE_DESCRIPTION = "такси"
        val CATEGORY_ID: UUID = UUID.fromString("00000000-0000-0000-0000-000000000001")
        val EXPENSE_ID: UUID = UUID.fromString("00000000-0000-0000-0000-000000000101")
        val OTHER_EXPENSE_ID: UUID = UUID.fromString("00000000-0000-0000-0000-000000000102")
        val EXPENSE_AMOUNT: Money = Money.of(BigDecimal("500.00"))
        val EXPENSE_DATE: LocalDate = LocalDate.parse("2026-05-24")
        val AWAITING_DELETION_CONFIRMATION: UserState.AwaitingExpenseDeletionConfirmation =
            UserState.AwaitingExpenseDeletionConfirmation(
                expenseId = EXPENSE_ID,
                cardMessageId = CARD_MESSAGE_ID,
            )
    }

    private fun expense(): Expense =
        Expense(
            id = EXPENSE_ID,
            categoryId = CATEGORY_ID,
            amount = EXPENSE_AMOUNT,
            userId = USER_ID,
            expenseDate = EXPENSE_DATE,
            description = EXPENSE_DESCRIPTION,
        )

    private fun category(): Category =
        Category(
            id = CATEGORY_ID,
            name = CATEGORY_NAME,
            userId = USER_ID,
        )
}
