package me.kmozze.expensetracker.adapter.ui

import me.kmozze.expensetracker.model.domain.BotAction
import org.springframework.stereotype.Component
import org.telegram.telegrambots.meta.api.methods.send.SendMessage

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
                    sendMessage.replyMarkup = Keyboards.categorySelection(action.categories)
            }
        }
    }
}
