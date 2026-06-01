package me.kmozze.expensetracker.model.domain

import java.time.Clock
import java.time.LocalDate

enum class ExpenseDateChoice {
    Today,
    Yesterday,
    ManualInput,
    ;

    fun toDate(clock: Clock): LocalDate? =
        when (this) {
            Today -> LocalDate.now(clock)
            Yesterday -> LocalDate.now(clock).minusDays(1)
            ManualInput -> null
        }
}
