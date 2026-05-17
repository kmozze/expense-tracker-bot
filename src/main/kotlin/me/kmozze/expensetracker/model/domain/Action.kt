package me.kmozze.expensetracker.model.domain

import me.kmozze.expensetracker.model.entity.Category

sealed class Action {
    object ShowMainMenu : Action()

    data class ShowCategorySelection(
        val categories: List<Category>,
    ) : Action()
}
