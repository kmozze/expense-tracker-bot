package me.kmozze.expensetracker.model.domain

sealed class Message {
    data object WelcomeFirstTime : Message()

    data object WelcomeBack : Message()

    data object UnknownCommand : Message()
}
