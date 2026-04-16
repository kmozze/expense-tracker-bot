package me.kmozze.expensetracker.adapter.ui

import me.kmozze.expensetracker.model.domain.Message
import org.springframework.stereotype.Component

@Component
class MessageFormatter {
    fun format(message: Message): String =
        when (message) {
            is Message.WelcomeFirstTime ->
                "👋 Добро пожаловать! Я создал для тебя базовые категории расходов."

            is Message.WelcomeBack ->
                "С возвращением! 💸 Я готов записывать твои новые траты."

            is Message.UnknownCommand ->
                "Я не понял команду 😕\nЧто бы вернуться в главное меню напиши /start"

            is Message.Error ->
                "❌ ${message.text}"
        }
}
