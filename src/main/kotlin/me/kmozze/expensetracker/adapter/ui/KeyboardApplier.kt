package me.kmozze.expensetracker.adapter.ui

import me.kmozze.expensetracker.model.domain.bot.BotAction
import org.springframework.stereotype.Component
import org.telegram.telegrambots.meta.api.methods.send.SendMessage
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardRemove

@Component
class KeyboardApplier {
    fun apply(
        sendMessage: SendMessage,
        actions: List<BotAction>,
    ) {
        actions.forEach { action ->
            when (action) {
                is BotAction.ShowMainMenu ->
                    sendMessage.replyMarkup = Keyboards.mainMenu()
                is BotAction.ShowCategorySelection ->
                    sendMessage.replyMarkup = Keyboards.categorySelection(action.categoryNames)
                is BotAction.ShowExpenseDateSelection ->
                    sendMessage.replyMarkup = Keyboards.expenseDateSelection()
                is BotAction.ShowCancel ->
                    sendMessage.replyMarkup = Keyboards.cancel()
                is BotAction.ShowExpenseCardActions ->
                    sendMessage.replyMarkup = Keyboards.expenseCardActions(action.expenseId)
                is BotAction.ShowExpenseDeletionConfirmation ->
                    sendMessage.replyMarkup = Keyboards.expenseDeletionConfirmation(action.expenseId)
                is BotAction.ShowExpenseListSettings ->
                    sendMessage.replyMarkup = Keyboards.expenseListSettings(action.filter)
                is BotAction.ShowExpenseListPeriodSelection ->
                    sendMessage.replyMarkup = Keyboards.expenseListPeriodSelection(action.filter)
                is BotAction.ShowExpenseListCategorySelection ->
                    sendMessage.replyMarkup = Keyboards.expenseListCategorySelection(action.filter, action.categories)
                is BotAction.ShowExpenseListPage ->
                    sendMessage.replyMarkup = Keyboards.expenseListPage(action.page)
                is BotAction.ShowExpenseEditFieldSelection ->
                    sendMessage.replyMarkup = Keyboards.expenseEditFieldSelection()
                is BotAction.ClearInlineKeyboard ->
                    Unit
                is BotAction.RemoveReplyKeyboard ->
                    sendMessage.replyMarkup = ReplyKeyboardRemove.builder().removeKeyboard(true).build()
            }
        }
    }

    fun apply(
        editMessage: EditMessageText,
        actions: List<BotAction>,
    ) {
        actions.forEach { action ->
            when (action) {
                is BotAction.ShowMainMenu ->
                    editMessage.replyMarkup = Keyboards.mainMenu()
                is BotAction.ShowCategorySelection ->
                    Unit
                is BotAction.ShowExpenseDateSelection ->
                    Unit
                is BotAction.ShowCancel ->
                    Unit
                is BotAction.ShowExpenseCardActions ->
                    editMessage.replyMarkup = Keyboards.expenseCardActions(action.expenseId)
                is BotAction.ShowExpenseDeletionConfirmation ->
                    editMessage.replyMarkup = Keyboards.expenseDeletionConfirmation(action.expenseId)
                is BotAction.ShowExpenseListSettings ->
                    editMessage.replyMarkup = Keyboards.expenseListSettings(action.filter)
                is BotAction.ShowExpenseListPeriodSelection ->
                    editMessage.replyMarkup = Keyboards.expenseListPeriodSelection(action.filter)
                is BotAction.ShowExpenseListCategorySelection ->
                    editMessage.replyMarkup = Keyboards.expenseListCategorySelection(action.filter, action.categories)
                is BotAction.ShowExpenseListPage ->
                    editMessage.replyMarkup = Keyboards.expenseListPage(action.page)
                is BotAction.ShowExpenseEditFieldSelection ->
                    Unit
                is BotAction.ClearInlineKeyboard,
                is BotAction.RemoveReplyKeyboard,
                ->
                    editMessage.replyMarkup = null
            }
        }
    }
}
