package me.kmozze.expensetracker.model.domain.bot

import java.util.UUID

sealed class BotAction {
    data object ShowMainMenu : BotAction()

    data class ShowCategorySelection(
        val categoryNames: List<String>,
    ) : BotAction()

    data object ShowExpenseDateSelection : BotAction()

    data object ShowCancel : BotAction()

    data class ShowExpenseCardActions(
        val expenseId: UUID,
    ) : BotAction()

    data class ShowExpenseDeletionConfirmation(
        val expenseId: UUID,
    ) : BotAction()

    data object ShowExpenseEditFieldSelection : BotAction()

    data object ClearInlineKeyboard : BotAction()

    data object RemoveReplyKeyboard : BotAction()
}
