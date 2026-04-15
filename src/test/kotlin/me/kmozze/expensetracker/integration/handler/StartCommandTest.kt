package me.kmozze.expensetracker.integration.handler

import me.kmozze.expensetracker.handler.DialogueRouter
import me.kmozze.expensetracker.integration.AbstractIntegrationTest
import me.kmozze.expensetracker.model.domain.Action
import me.kmozze.expensetracker.model.domain.Message
import me.kmozze.expensetracker.model.domain.UserInput
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
        val input = UserInput(userId = userId, chatId = 12345L, text = "/start")

        val response = dialogueRouter.process(input)

        assertThat(response.message).isEqualTo(Message.WelcomeFirstTime)
        assertThat(response.actions).containsExactly(Action.ShowMainMenu)

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

        val input = UserInput(userId = userId, chatId = 54321L, text = "/start")
        val response = dialogueRouter.process(input)

        assertThat(response.message).isEqualTo(Message.WelcomeBack)
        assertThat(response.actions).containsExactly(Action.ShowMainMenu)
    }
}
