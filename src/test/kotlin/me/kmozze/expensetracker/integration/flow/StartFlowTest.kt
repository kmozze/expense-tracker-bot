package me.kmozze.expensetracker.integration.flow

import me.kmozze.expensetracker.handler.DialogueRouter
import me.kmozze.expensetracker.model.domain.BotAction
import me.kmozze.expensetracker.model.domain.BotText
import me.kmozze.expensetracker.model.domain.OutgoingMessage
import me.kmozze.expensetracker.model.domain.UserState
import me.kmozze.expensetracker.model.entity.Category
import me.kmozze.expensetracker.repository.ICategoryRepository
import me.kmozze.expensetracker.support.processUserInput
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.util.UUID

class StartFlowTest : AbstractFlowIntegrationTest() {
    @Autowired
    private lateinit var dialogueRouter: DialogueRouter

    @Autowired
    private lateinit var categoryRepository: ICategoryRepository

    @Test
    fun `receive welcome message and default categories created`() {
        val userId = 1001L

        val result = dialogueRouter.processUserInput(userId = userId, chatId = 12345L, text = "/start")

        assertThat(result.response.outgoingMessages)
            .containsExactly(
                OutgoingMessage(
                    text = BotText.WelcomeFirstTime,
                    actions = listOf(BotAction.RemoveReplyKeyboard),
                ),
                OutgoingMessage(
                    text = BotText.MainMenu,
                    actions = listOf(BotAction.ShowMainMenu),
                ),
            )
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

        val result = dialogueRouter.processUserInput(userId = userId, chatId = 54321L, text = "/start")

        assertThat(result.response.outgoingMessages)
            .containsExactly(
                OutgoingMessage(
                    text = BotText.WelcomeBack,
                    actions = listOf(BotAction.RemoveReplyKeyboard),
                ),
                OutgoingMessage(
                    text = BotText.MainMenu,
                    actions = listOf(BotAction.ShowMainMenu),
                ),
            )
        assertThat(result.nextState).isEqualTo(UserState.Idle)
    }
}
