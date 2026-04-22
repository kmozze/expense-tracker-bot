package me.kmozze.expensetracker.handler

import me.kmozze.expensetracker.exception.AppException
import me.kmozze.expensetracker.exception.SystemErrorCode
import me.kmozze.expensetracker.model.domain.Action
import me.kmozze.expensetracker.model.domain.HandlerResponse
import me.kmozze.expensetracker.model.domain.HandlerResult
import me.kmozze.expensetracker.model.domain.Message
import me.kmozze.expensetracker.model.domain.UserState
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

@Component
class ErrorHandler {
    private val logger = LoggerFactory.getLogger(javaClass)

    fun handle(
        userId: Long,
        exception: Exception,
    ): HandlerResult {
        val errorMessage =
            when (exception) {
                is AppException -> {
                    logger.warn("AppException for user {}: {}", userId, exception.message, exception)
                    Message.Error(exception.errorCode)
                }
                else -> {
                    logger.error("Unexpected exception for user {}", userId, exception)
                    Message.Error(SystemErrorCode.INTERNAL_ERROR)
                }
            }

        return HandlerResult(
            response =
                HandlerResponse(
                    message = errorMessage,
                    actions = listOf(Action.ShowMainMenu),
                ),
            nextState = UserState.Idle,
        )
    }
}
