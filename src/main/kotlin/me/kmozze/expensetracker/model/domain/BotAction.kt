package me.kmozze.expensetracker.model.domain

import me.kmozze.expensetracker.model.entity.Category

sealed class BotAction {
    object ShowMainMenu : BotAction()

    data class ShowCategorySelection(
        val categories: List<Category>,
    ) : BotAction()
}
