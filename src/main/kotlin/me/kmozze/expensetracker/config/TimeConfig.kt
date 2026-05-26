package me.kmozze.expensetracker.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.time.Clock
import java.time.ZoneId

@Configuration
class TimeConfig {
    @Bean
    fun clock(): Clock = Clock.system(EXPENSE_DATE_ZONE)

    private companion object {
        val EXPENSE_DATE_ZONE: ZoneId = ZoneId.of("Europe/Moscow")
    }
}
