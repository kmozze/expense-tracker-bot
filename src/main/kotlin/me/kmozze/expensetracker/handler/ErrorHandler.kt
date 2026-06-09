package me.kmozze.expensetracker.handler

import me.kmozze.expensetracker.exception.AppException
import me.kmozze.expensetracker.exception.SystemErrorCode
import me.kmozze.expensetracker.model.domain.BotAction
import me.kmozze.expensetracker.model.domain.BotText
import me.kmozze.expensetracker.model.domain.HandlerResponse
import me.kmozze.expensetracker.model.domain.OutgoingMessage
import me.kmozze.expensetracker.model.domain.UserState
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

@Component
class ErrorHandler {
    private val logger = LoggerFactory.getLogger(javaClass)

    fun handle(
        userId: Long,
        exception: Exception,
    ): HandlerResponse {
        val errorMessage =
            when (exception) {
                is AppException -> {
                    logger.warn("AppException for user {}: {}", userId, exception.message, exception)
                    BotText.Error(exception.errorCode)
                }
                else -> {
                    logger.error("Unexpected exception for user {}", userId, exception)
                    BotText.Error(SystemErrorCode.INTERNAL_ERROR)
                }
            }

        return HandlerResponse(
            outgoingMessages =
                listOf(
                    OutgoingMessage(
                        text = errorMessage,
                        actions = listOf(BotAction.ShowMainMenu),
                    ),
                ),
            nextState = UserState.Idle,
        )
    }
}
