package me.kmozze.expensetracker.unit.handler.statehandler

import io.mockk.confirmVerified
import io.mockk.every
import io.mockk.junit5.MockKExtension
import io.mockk.mockk
import io.mockk.verify
import me.kmozze.expensetracker.exception.BusinessErrorCode
import me.kmozze.expensetracker.handler.statehandler.AwaitingCategorySelectionHandler
import me.kmozze.expensetracker.model.domain.BotAction
import me.kmozze.expensetracker.model.domain.BotText
import me.kmozze.expensetracker.model.domain.ExpenseDraft
import me.kmozze.expensetracker.model.domain.Money
import me.kmozze.expensetracker.model.domain.ResponseDelivery
import me.kmozze.expensetracker.model.domain.UserCommand
import me.kmozze.expensetracker.model.domain.UserState
import me.kmozze.expensetracker.model.entity.Category
import me.kmozze.expensetracker.service.CategoryService
import me.kmozze.expensetracker.support.makeUserInput
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import java.math.BigDecimal
import java.util.UUID

@ExtendWith(MockKExtension::class)
class AwaitingCategorySelectionHandlerTest {
    private val categoryService: CategoryService = mockk()
    private lateinit var handler: AwaitingCategorySelectionHandler

    @BeforeEach
    fun setUp() {
        handler =
            AwaitingCategorySelectionHandler(
                categoryService = categoryService,
            )
    }

    @Test
    fun `selected category opens expense date selection`() {
        val category = category(id = CATEGORY_ID)
        every { categoryService.findCategoryForUser(CATEGORY_ID, USER_ID) } returns category

        val result = handle(UserCommand.SelectCategory(CATEGORY_ID))

        assertThat(
            result.response.outgoingMessages
                .single()
                .text,
        ).isEqualTo(
            BotText.SelectExpenseDate(
                amount = EXPENSE_AMOUNT,
                categoryName = category.name,
                description = EXPENSE_DESCRIPTION,
            ),
        )
        assertThat(
            result.response.outgoingMessages
                .single()
                .actions,
        ).containsExactly(BotAction.ShowExpenseDateSelection)
        assertThat(
            result.response.outgoingMessages
                .single()
                .delivery,
        ).isEqualTo(ResponseDelivery.EditMessage(CARD_MESSAGE_ID))
        assertThat(result.nextState)
            .isEqualTo(
                UserState.AwaitingExpenseDateSelection(
                    expenseDraft = EXPENSE_DRAFT.copy(categoryId = CATEGORY_ID),
                    categoryName = category.name,
                    cardMessageId = CARD_MESSAGE_ID,
                ),
            )
        verify(exactly = 1) { categoryService.findCategoryForUser(CATEGORY_ID, USER_ID) }
        confirmVerified(categoryService)
    }

    @Test
    fun `missing category keeps category selection open with error`() {
        val categories = listOf(category(id = CATEGORY_ID), category(id = SECOND_CATEGORY_ID))
        every { categoryService.findCategoryForUser(CATEGORY_ID, USER_ID) } returns null
        every { categoryService.getCategories(USER_ID) } returns categories

        val result = handle(UserCommand.SelectCategory(CATEGORY_ID))

        assertThat(
            result.response.outgoingMessages
                .single()
                .text,
        ).isEqualTo(BotText.Error(BusinessErrorCode.CATEGORY_NOT_FOUND))
        assertThat(
            result.response.outgoingMessages
                .single()
                .actions,
        ).containsExactly(BotAction.ShowCategorySelection(categories))
        assertThat(
            result.response.outgoingMessages
                .single()
                .delivery,
        ).isEqualTo(ResponseDelivery.EditMessage(CARD_MESSAGE_ID))
        assertThat(result.nextState).isEqualTo(AWAITING_CATEGORY_SELECTION)
        verify(exactly = 1) { categoryService.findCategoryForUser(CATEGORY_ID, USER_ID) }
        verify(exactly = 1) { categoryService.getCategories(USER_ID) }
        confirmVerified(categoryService)
    }

    @Test
    fun `invalid category selection keeps category selection open with error`() {
        val categories = listOf(category(id = CATEGORY_ID), category(id = SECOND_CATEGORY_ID))
        every { categoryService.getCategories(USER_ID) } returns categories

        val result = handle(UserCommand.InvalidCategorySelection)

        assertThat(
            result.response.outgoingMessages
                .single()
                .text,
        ).isEqualTo(BotText.Error(BusinessErrorCode.INVALID_CATEGORY_SELECTION))
        assertThat(
            result.response.outgoingMessages
                .single()
                .actions,
        ).containsExactly(BotAction.ShowCategorySelection(categories))
        assertThat(
            result.response.outgoingMessages
                .single()
                .delivery,
        ).isEqualTo(ResponseDelivery.EditMessage(CARD_MESSAGE_ID))
        assertThat(result.nextState).isEqualTo(AWAITING_CATEGORY_SELECTION)
        verify(exactly = 1) { categoryService.getCategories(USER_ID) }
        confirmVerified(categoryService)
    }

    @Test
    fun `cancel returns idle without service calls`() {
        val result = handle(UserCommand.Cancel)

        assertThat(
            result.response.outgoingMessages
                .single()
                .text,
        ).isEqualTo(BotText.ExpenseCanceled)
        assertThat(
            result.response.outgoingMessages
                .single()
                .actions,
        ).containsExactly(BotAction.ClearInlineKeyboard)
        assertThat(
            result.response.outgoingMessages
                .single()
                .delivery,
        ).isEqualTo(ResponseDelivery.EditMessage(CARD_MESSAGE_ID))
        assertThat(result.nextState).isEqualTo(UserState.Idle)
        confirmVerified(categoryService)
    }

    @Test
    fun `non-selection command repeats category selection`() {
        val categories = listOf(category(id = CATEGORY_ID), category(id = SECOND_CATEGORY_ID))
        every { categoryService.getCategories(USER_ID) } returns categories

        val result = handle(UserCommand.PlainText("700 кофе"))

        assertThat(
            result.response.outgoingMessages
                .single()
                .text,
        ).isEqualTo(BotText.SelectCategory(EXPENSE_AMOUNT, EXPENSE_DESCRIPTION))
        assertThat(
            result.response.outgoingMessages
                .single()
                .actions,
        ).containsExactly(BotAction.ShowCategorySelection(categories))
        assertThat(
            result.response.outgoingMessages
                .single()
                .delivery,
        ).isEqualTo(ResponseDelivery.SendNewMessage)
        assertThat(result.nextState).isEqualTo(AWAITING_CATEGORY_SELECTION)
        verify(exactly = 1) { categoryService.getCategories(USER_ID) }
        confirmVerified(categoryService)
    }

    private fun handle(command: UserCommand) =
        handler.handle(
            input =
                makeUserInput(
                    userId = USER_ID,
                    chatId = CHAT_ID,
                    callbackData = callbackDataFor(command),
                    callbackMessageId = callbackMessageIdFor(command),
                    command = command,
                ),
            currentState = AWAITING_CATEGORY_SELECTION,
        )

    private fun callbackDataFor(command: UserCommand): String? =
        when (command) {
            is UserCommand.PlainText -> null
            else -> "callback"
        }

    private fun callbackMessageIdFor(command: UserCommand): Int? =
        when (command) {
            is UserCommand.PlainText -> null
            else -> CARD_MESSAGE_ID
        }

    private fun category(id: UUID): Category =
        Category(
            id = id,
            name = "Еда",
            userId = USER_ID,
        )

    private companion object {
        const val USER_ID = 123L
        const val CHAT_ID = 456L
        const val CARD_MESSAGE_ID = 789
        const val EXPENSE_DESCRIPTION = "такси"
        val EXPENSE_AMOUNT: Money = Money.of(BigDecimal("500.00"))
        val EXPENSE_DRAFT: ExpenseDraft = ExpenseDraft(EXPENSE_AMOUNT, EXPENSE_DESCRIPTION)
        val AWAITING_CATEGORY_SELECTION: UserState.AwaitingCategorySelection =
            UserState.AwaitingCategorySelection(EXPENSE_DRAFT)
        val CATEGORY_ID: UUID = UUID.fromString("00000000-0000-0000-0000-000000000001")
        val SECOND_CATEGORY_ID: UUID = UUID.fromString("00000000-0000-0000-0000-000000000002")
    }
}
