package me.kmozze.expensetracker.service

import me.kmozze.expensetracker.model.domain.UserState
import org.springframework.stereotype.Service
import java.util.concurrent.ConcurrentHashMap

@Service
class UserSessionService {
    private val sessions = ConcurrentHashMap<Long, UserState>()

    fun getState(userId: Long): UserState = sessions.computeIfAbsent(userId) { UserState.Idle }

    fun clear(userId: Long) {
        sessions.remove(userId)
    }

    fun setState(
        userId: Long,
        state: UserState,
    ) {
        sessions[userId] = state
    }
}
