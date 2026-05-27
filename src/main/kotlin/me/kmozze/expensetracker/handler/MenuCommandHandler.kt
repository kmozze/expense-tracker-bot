package me.kmozze.expensetracker.handler

import me.kmozze.expensetracker.model.domain.BotAction
import me.kmozze.expensetracker.model.domain.BotText
import me.kmozze.expensetracker.model.domain.HandlerResponse
import me.kmozze.expensetracker.model.domain.HandlerResult
import me.kmozze.expensetracker.model.domain.OutgoingMessage
import me.kmozze.expensetracker.model.domain.UserState
import org.springframework.stereotype.Component

@Component
class MenuCommandHandler {
    fun handle(): HandlerResult =
        HandlerResult(
            response =
                HandlerResponse(
                    outgoingMessages =
                        listOf(
                            OutgoingMessage(
                                text = BotText.Done,
                                actions = listOf(BotAction.RemoveReplyKeyboard),
                            ),
                            OutgoingMessage(
                                text = BotText.MainMenu,
                                actions = listOf(BotAction.ShowMainMenu),
                            ),
                        ),
                ),
            nextState = UserState.Idle,
        )
}
