package me.kmozze.expensetracker.model.domain

import me.kmozze.expensetracker.model.entity.Category
import java.util.UUID

sealed class BotAction {
    data object ShowMainMenu : BotAction()

    data class ShowCategorySelection(
        val categories: List<Category>,
    ) : BotAction()

    data object ShowExpenseDateSelection : BotAction()

    data object ShowCancel : BotAction()

    data class ShowExpenseCardActions(
        val expenseId: UUID,
    ) : BotAction()

    data object ClearInlineKeyboard : BotAction()

    data object RemoveReplyKeyboard : BotAction()
}
