package me.kmozze.expensetracker.model.domain.bot

data class UserInput(
    val userId: Long,
    val chatId: Long,
    val text: String? = null,
    val callbackData: String? = null,
    val callbackMessageId: Int? = null,
    val command: UserCommand,
)
