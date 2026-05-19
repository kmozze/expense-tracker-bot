package me.kmozze.expensetracker.integration

import me.kmozze.expensetracker.service.UserSessionService
import org.junit.jupiter.api.AfterEach
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.testcontainers.containers.PostgreSQLContainer

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@ActiveProfiles("test")
abstract class AbstractIntegrationTest {
    @Autowired
    private lateinit var userSessionService: UserSessionService

    @AfterEach
    fun clearUserSessions() {
        userSessionService.clearAll()
    }

    companion object {
        val postgresContainer =
            PostgreSQLContainer("postgres:16-alpine")
                .withDatabaseName("expense_test_db")
                .withUsername("test_user")
                .withPassword("test_password")
                .apply { start() }

        @JvmStatic
        @DynamicPropertySource
        fun overrideProperties(registry: DynamicPropertyRegistry) {
            registry.add("spring.datasource.url") { postgresContainer.jdbcUrl }
            registry.add("spring.datasource.username") { postgresContainer.username }
            registry.add("spring.datasource.password") { postgresContainer.password }
        }
    }
}
