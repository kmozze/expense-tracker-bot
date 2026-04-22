package me.kmozze.expensetracker.model.domain

import me.kmozze.expensetracker.exception.ErrorCode

sealed class Message {
    data object WelcomeFirstTime : Message()

    data object WelcomeBack : Message()

    data object UnknownCommand : Message()

    data class Error(
        val errorCode: ErrorCode,
    ) : Message()
}
