package me.kmozze.expensetracker.handler

import me.kmozze.expensetracker.model.domain.Action
import me.kmozze.expensetracker.model.domain.HandlerResponse
import me.kmozze.expensetracker.model.domain.Message
import me.kmozze.expensetracker.model.domain.UserInput
import me.kmozze.expensetracker.service.CategoryService
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

@Component
class StartHandler(
    private val categoryService: CategoryService,
) : IHandler {
    private val logger = LoggerFactory.getLogger(javaClass)

    override fun handle(input: UserInput): HandlerResponse {
        val userId = input.userId
        logger.info("Initializing start sequence for user {}", userId)

        val isFirstTime = categoryService.initDefaultCategories(userId)

        val welcomeText = if (isFirstTime) Message.WelcomeFirstTime else Message.WelcomeBack

        return HandlerResponse(
            message = welcomeText,
            actions = listOf(Action.ShowMainMenu),
        )
    }
}
