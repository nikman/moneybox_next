package com.niku.analytics.model

/**
 * Данные точек 12-месячного отчета баланса.
 */
data class MonthlyBalancePoint(
    val monthLabel: String,
    val year: Int,
    val incomeAmount: Double,
    val expenseAmount: Double,
    val balanceAmount: Double,
    val timestamp: Long
)
