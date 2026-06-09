package me.kmozze.expensetracker.handler

import me.kmozze.expensetracker.model.domain.bot.BotAction
import me.kmozze.expensetracker.model.domain.bot.BotText
import me.kmozze.expensetracker.model.domain.bot.HandlerResponse
import me.kmozze.expensetracker.model.domain.bot.OutgoingMessage
import me.kmozze.expensetracker.model.domain.bot.UserState
import org.springframework.stereotype.Component

@Component
class MenuCommandHandler {
    fun handle(removeReplyKeyboard: Boolean): HandlerResponse =
        handlerResponse(
            messages =
                listOf(
                    outgoingMessage(
                        text = BotText.MainMenuInfo,
                        actions =
                            if (removeReplyKeyboard) {
                                listOf(BotAction.RemoveReplyKeyboard)
                            } else {
                                emptyList()
                            },
                    ),
                    mainMenuActionsMessage(),
                ),
            nextState = UserState.Idle,
        )

    private fun mainMenuActionsMessage(): OutgoingMessage =
        outgoingMessage(
            text = BotText.MainMenuActions,
            actions = listOf(BotAction.ShowMainMenu),
        )
}
