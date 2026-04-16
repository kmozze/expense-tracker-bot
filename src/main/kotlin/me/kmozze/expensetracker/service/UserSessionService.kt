package me.kmozze.expensetracker.service

import me.kmozze.expensetracker.model.domain.UserSession
import me.kmozze.expensetracker.model.domain.UserState
import org.springframework.stereotype.Service
import java.util.concurrent.ConcurrentHashMap

@Service
class UserSessionService {
    private val sessions = ConcurrentHashMap<Long, UserSession>()

    fun getOrCreate(userId: Long): UserSession = sessions.computeIfAbsent(userId) { UserSession(it) }

    fun getCurrentState(userId: Long): UserState = getOrCreate(userId).currentState

    fun setNextState(
        userId: Long,
        nextState: UserState?,
    ) {
        sessions.compute(userId) { _, oldSession ->
            val session = oldSession ?: UserSession(userId)
            session.copy(nextState = nextState)
        }
    }

    fun applyNextState(userId: Long) {
        sessions.compute(userId) { _, oldSession ->
            val session = oldSession ?: UserSession(userId)
            session.nextState?.let { next ->
                session.copy(currentState = next, nextState = null)
            } ?: session
        }
    }

    fun clear(userId: Long) {
        sessions.remove(userId)
    }

    fun setStateImmediately(
        userId: Long,
        state: UserState,
    ) {
        sessions[userId] = UserSession(userId, currentState = state)
    }
}
