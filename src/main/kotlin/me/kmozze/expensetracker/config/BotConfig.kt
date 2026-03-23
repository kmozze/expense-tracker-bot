package me.kmozze.expensetracker.config

import me.kmozze.expensetracker.bot.ExpenseTrackerBot
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.telegram.telegrambots.meta.TelegramBotsApi
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession

@Configuration
class BotConfig {
    @Bean
    fun telegramBotsApi(expenseTrackerBot: ExpenseTrackerBot): TelegramBotsApi {
        val botsApi = TelegramBotsApi(DefaultBotSession::class.java)
        botsApi.registerBot(expenseTrackerBot)
        return botsApi
    }
}