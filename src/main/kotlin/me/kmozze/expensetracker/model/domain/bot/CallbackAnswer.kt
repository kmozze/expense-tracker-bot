package me.kmozze.expensetracker.model.domain.bot

data class CallbackAnswer(
    val text: BotText? = null,
    val showAlert: Boolean = false,
)
