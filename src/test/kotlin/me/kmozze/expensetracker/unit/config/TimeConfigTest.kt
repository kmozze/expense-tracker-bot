package me.kmozze.expensetracker.unit.config

import me.kmozze.expensetracker.config.TimeConfig
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset

class TimeConfigTest {
    @Test
    fun `clock uses Moscow time zone for user-facing quick dates`() {
        val clock = TimeConfig().clock()
        val instant = Instant.parse("2026-05-25T21:30:00Z")

        assertThat(clock.zone).isEqualTo(ZoneId.of("Europe/Moscow"))
        assertThat(LocalDate.now(Clock.fixed(instant, clock.zone))).isEqualTo(LocalDate.parse("2026-05-26"))
        assertThat(LocalDate.now(Clock.fixed(instant, ZoneOffset.UTC))).isEqualTo(LocalDate.parse("2026-05-25"))
    }
}
