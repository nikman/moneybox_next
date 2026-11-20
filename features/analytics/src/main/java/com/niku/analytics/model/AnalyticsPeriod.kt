package com.niku.analytics.model

/**
 * Периоды фильтрации отчетов в аналитике.
 */
enum class AnalyticsPeriod(val title: String) {
    WEEK("Неделя"),
    MONTH("Месяц"),
    YEAR("Год"),
    ALL_TIME("Всё время")
}
