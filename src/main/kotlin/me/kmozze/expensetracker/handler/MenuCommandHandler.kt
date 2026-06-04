package me.kmozze.expensetracker.handler

import me.kmozze.expensetracker.model.domain.BotAction
import me.kmozze.expensetracker.model.domain.BotText
import me.kmozze.expensetracker.model.domain.HandlerResponse
import me.kmozze.expensetracker.model.domain.OutgoingMessage
import me.kmozze.expensetracker.model.domain.UserState
import org.springframework.stereotype.Component

@Component
class MenuCommandHandler {
    fun handle(removeReplyKeyboard: Boolean): HandlerResponse =
        HandlerResponse(
            outgoingMessages =
                listOf(
                    OutgoingMessage(
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
        OutgoingMessage(
            text = BotText.MainMenuActions,
            actions = listOf(BotAction.ShowMainMenu),
        )
}
