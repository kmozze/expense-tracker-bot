package me.kmozze.expensetracker.integration.flow

import me.kmozze.expense.tracker.jooq.tables.references.CATEGORY
import me.kmozze.expense.tracker.jooq.tables.references.EXPENSE
import me.kmozze.expensetracker.integration.AbstractIntegrationTest
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
}
