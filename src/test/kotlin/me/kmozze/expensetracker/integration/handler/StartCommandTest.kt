package me.kmozze.expensetracker.integration.handler

import me.kmozze.expensetracker.adapter.input.UserCommandParser
import me.kmozze.expensetracker.handler.DialogueRouter
import me.kmozze.expensetracker.integration.AbstractIntegrationTest
import me.kmozze.expensetracker.model.domain.BotAction
import me.kmozze.expensetracker.model.domain.BotMessage
import me.kmozze.expensetracker.model.domain.UserInput
import me.kmozze.expensetracker.model.domain.UserState
import me.kmozze.expensetracker.model.entity.Category
import me.kmozze.expensetracker.repository.ICategoryRepository
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Transactional
class StartCommandTest : AbstractIntegrationTest() {
    @Autowired
    private lateinit var dialogueRouter: DialogueRouter

    @Autowired
    private lateinit var categoryRepository: ICategoryRepository

    @Test
    fun `receive welcome message and default categories created`() {
        val userId = 1001L
        val input = userInput(userId = userId, chatId = 12345L, text = "/start")

        val result = dialogueRouter.process(input)

        assertThat(result.response.message).isEqualTo(BotMessage.WelcomeFirstTime)
        assertThat(result.response.actions).containsExactly(BotAction.ShowMainMenu)
        assertThat(result.nextState).isEqualTo(UserState.Idle)

        val categoriesExist = categoryRepository.existsByUserId(userId)
        assertThat(categoriesExist).isTrue()
    }

    @Test
    fun `receive welcome back message`() {
        val userId = 10002L

        val existingCategory =
            Category(
                id = UUID.randomUUID(),
                name = "Всякое",
                userId = userId,
            )
        categoryRepository.create(existingCategory)

        val input = userInput(userId = userId, chatId = 54321L, text = "/start")
        val result = dialogueRouter.process(input)

        assertThat(result.response.message).isEqualTo(BotMessage.WelcomeBack)
        assertThat(result.response.actions).containsExactly(BotAction.ShowMainMenu)
        assertThat(result.nextState).isEqualTo(UserState.Idle)
    }

    private fun userInput(
        userId: Long,
        chatId: Long,
        text: String? = null,
        callbackData: String? = null,
    ): UserInput =
        UserInput(
            userId = userId,
            chatId = chatId,
            text = text,
            callbackData = callbackData,
            command = UserCommandParser.parse(text = text, callbackData = callbackData),
        )
}
