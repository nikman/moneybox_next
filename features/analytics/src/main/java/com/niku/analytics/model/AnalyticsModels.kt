package com.niku.analytics.model

import androidx.compose.ui.graphics.Color

/**
 * Элемент диаграммы распределения по категориям.
 */
data class CategoryChartItem(
    val categoryId: Long?,
    val categoryName: String,
    val amount: Double,
    val percentage: Float,
    val color: Color
)

/**
 * Точка графика динамики (трендов) доходов и расходов за выбранный интервал.
 */
data class TrendPoint(
    val label: String,
    val incomeAmount: Double,
    val expenseAmount: Double,
    val timestamp: Long
)
