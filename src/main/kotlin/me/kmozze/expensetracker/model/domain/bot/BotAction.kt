package me.kmozze.expensetracker.model.domain.bot

import me.kmozze.expensetracker.model.domain.expense.ExpenseListCategoryOption
import me.kmozze.expensetracker.model.domain.expense.ExpenseListFilter
import me.kmozze.expensetracker.model.domain.expense.ExpenseListPage
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

    data class ShowExpenseListSettings(
        val filter: ExpenseListFilter,
    ) : BotAction()

    data class ShowExpenseListPeriodSelection(
        val filter: ExpenseListFilter,
    ) : BotAction()

    data class ShowExpenseListCategorySelection(
        val filter: ExpenseListFilter,
        val categories: List<ExpenseListCategoryOption>,
    ) : BotAction()

    data class ShowExpenseListPage(
        val page: ExpenseListPage,
    ) : BotAction()

    data object ShowExpenseEditFieldSelection : BotAction()

    data object ClearInlineKeyboard : BotAction()

    data object RemoveReplyKeyboard : BotAction()
}
