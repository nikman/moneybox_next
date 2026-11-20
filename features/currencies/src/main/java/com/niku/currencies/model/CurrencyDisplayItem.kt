package com.niku.currencies.model

/**
 * Элемент списка котировок и быстрый конвертер валют.
 */
data class CurrencyDisplayItem(
    val code: String,           // Символьный код (USD, EUR, CNY, KZT)
    val name: String,           // Название (Доллар США, Евро)
    val flagEmoji: String,      // Флаг страны (🇺🇸, 🇪🇺, 🇨🇳)
    val rate: Double,           // Курс относительно базовой валюты
    val convertedAmount: Double // Рассчитанная итоговая сумма
)
