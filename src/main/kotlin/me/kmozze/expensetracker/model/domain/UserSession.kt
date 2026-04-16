package me.kmozze.expensetracker.model.domain

data class UserSession(
    val userId: Long,
    val currentState: UserState = UserState.Idle,
    val nextState: UserState? = null,
)
