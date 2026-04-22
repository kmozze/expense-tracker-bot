package me.kmozze.expensetracker.adapter.ui

import me.kmozze.expensetracker.model.domain.Action
import org.springframework.stereotype.Component
import org.telegram.telegrambots.meta.api.methods.send.SendMessage

@Component
class KeyboardApplier {
    fun apply(
        sendMessage: SendMessage,
        actions: List<Action>,
    ) {
        actions.forEach { action ->
            when (action) {
                is Action.ShowMainMenu ->
                    sendMessage.replyMarkup = Keyboards.mainMenu()
            }
        }
    }
}
