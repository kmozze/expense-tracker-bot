package me.kmozze.expensetracker.handler

import me.kmozze.expensetracker.model.domain.bot.BotAction
import me.kmozze.expensetracker.model.domain.bot.BotText
import me.kmozze.expensetracker.model.domain.bot.HandlerResponse
import me.kmozze.expensetracker.model.domain.bot.UserInput
import me.kmozze.expensetracker.model.domain.bot.UserState
import me.kmozze.expensetracker.service.CategoryService
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

@Component
class StartCommandHandler(
    private val categoryService: CategoryService,
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    fun handle(input: UserInput): HandlerResponse {
        val userId = input.userId
        logger.info("Initializing start sequence for user {}", userId)

        val isFirstTime = categoryService.initDefaultCategories(userId)

        val welcomeText = if (isFirstTime) BotText.WelcomeFirstTime else BotText.WelcomeBack

        return handlerResponse(
            messages =
                listOf(
                    outgoingMessage(
                        text = welcomeText,
                        actions = listOf(BotAction.RemoveReplyKeyboard),
                    ),
                    outgoingMessage(
                        text = BotText.MainMenu,
                        actions = listOf(BotAction.ShowMainMenu),
                    ),
                ),
            nextState = UserState.Idle,
        )
    }
}
