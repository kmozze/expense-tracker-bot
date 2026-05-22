package me.kmozze.expensetracker.integration.handler

import me.kmozze.expensetracker.handler.DialogueRouter
import me.kmozze.expensetracker.integration.AbstractIntegrationTest
import me.kmozze.expensetracker.model.domain.BotAction
import me.kmozze.expensetracker.model.domain.BotMessage
import me.kmozze.expensetracker.model.domain.UserState
import me.kmozze.expensetracker.model.entity.Category
import me.kmozze.expensetracker.repository.ICategoryRepository
import me.kmozze.expensetracker.support.processUserInput
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

        val result = dialogueRouter.processUserInput(userId = userId, chatId = 12345L, text = "/start")

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

        val result = dialogueRouter.processUserInput(userId = userId, chatId = 54321L, text = "/start")

        assertThat(result.response.message).isEqualTo(BotMessage.WelcomeBack)
        assertThat(result.response.actions).containsExactly(BotAction.ShowMainMenu)
        assertThat(result.nextState).isEqualTo(UserState.Idle)
    }
}
