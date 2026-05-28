package me.kmozze.expensetracker.model.domain

data class CallbackAnswer(
    val text: BotText? = null,
    val showAlert: Boolean = false,
)
