package me.kmozze.expensetracker

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class ExpenseTrackerBotApplication

fun main(args: Array<String>) {
    runApplication<ExpenseTrackerBotApplication>(*args)
}
