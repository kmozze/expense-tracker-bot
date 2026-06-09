package me.kmozze.expensetracker.integration.flow

import me.kmozze.expense.tracker.jooq.tables.references.CATEGORY
import me.kmozze.expense.tracker.jooq.tables.references.EXPENSE
import me.kmozze.expensetracker.integration.AbstractIntegrationTest
import me.kmozze.expensetracker.model.domain.bot.BotAction
import me.kmozze.expensetracker.model.domain.bot.BotText
import me.kmozze.expensetracker.model.domain.bot.HandlerResponse
import me.kmozze.expensetracker.model.domain.bot.OutgoingMessage
import me.kmozze.expensetracker.model.domain.bot.ResponseDelivery
import me.kmozze.expensetracker.model.domain.bot.UserState
import org.assertj.core.api.Assertions.assertThat
import org.jooq.DSLContext
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.springframework.beans.factory.annotation.Autowired

abstract class AbstractFlowIntegrationTest : AbstractIntegrationTest() {
    @Autowired
    private lateinit var dsl: DSLContext

    @BeforeEach
    fun cleanDatabaseBeforeTest() {
        truncateFlowTables()
    }

    @AfterEach
    fun cleanDatabaseAfterTest() {
        truncateFlowTables()
    }

    private fun truncateFlowTables() {
        dsl
            .truncate(EXPENSE, CATEGORY)
            .cascade()
            .execute()
    }

    protected fun HandlerResponse.assertSingleMessage(
        text: BotText,
        actions: List<BotAction>? = null,
        delivery: ResponseDelivery? = null,
    ): OutgoingMessage {
        assertThat(outgoingMessages).hasSize(1)
        return assertMessage(
            index = 0,
            text = text,
            actions = actions,
            delivery = delivery,
        )
    }

    protected fun HandlerResponse.assertMessage(
        index: Int,
        text: BotText,
        actions: List<BotAction>? = null,
        delivery: ResponseDelivery? = null,
    ): OutgoingMessage {
        val message = outgoingMessages[index]
        assertThat(message.text).isEqualTo(text)
        actions?.let { assertThat(message.actions).containsExactlyElementsOf(it) }
        delivery?.let { assertThat(message.delivery).isEqualTo(it) }
        return message
    }

    protected fun HandlerResponse.assertMessageTexts(vararg texts: BotText) {
        assertThat(outgoingMessages.map { it.text }).containsExactly(*texts)
    }

    protected fun HandlerResponse.assertLastMessageActions(vararg actions: BotAction) {
        assertThat(outgoingMessages.last().actions).containsExactly(*actions)
    }

    protected fun HandlerResponse.assertNextState(state: UserState) {
        assertThat(nextState).isEqualTo(state)
    }
}
