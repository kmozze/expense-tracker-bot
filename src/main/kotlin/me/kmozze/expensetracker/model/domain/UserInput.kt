package me.kmozze.expensetracker.model.domain

data class UserInput(
    val userId: Long,
    val chatId: Long,
    val text: String? = null,
    val callbackData: String? = null,
)
