package com.niku.preferences

import java.util.Locale

/**
 * Утилита форматирования денежных сумм, конвертации и локализации символов валют.
 */
object CurrencyFormatter {

    /**
     * Получить значок (пиктограмму) валюты по её коду.
     */
    fun getSymbol(currencyCode: String): String {
        return when (currencyCode.uppercase(Locale.ROOT)) {
            "RUB" -> "₽"
            "USD" -> "$"
            "EUR" -> "€"
            "GBP" -> "£"
            "CNY" -> "¥"
            "KZT" -> "₸"
            "BYN" -> "Br"
            "TRY" -> "₺"
            "AED" -> "د.إ"
            else -> currencyCode
        }
    }

    /**
     * Форматирует числовое значение в красиво оформленную строку с валютой.
     */
    fun format(amount: Double, currencyCode: String = "RUB"): String {
        val symbol = getSymbol(currencyCode)
        return String.format(Locale.getDefault(), "%,.2f %s", amount, symbol)
    }

    /**
     * Конвертирует сумму из [fromCurrency] в [toCurrency] по карте курсов [rates].
     */
    fun convert(
        amount: Double,
        fromCurrency: String,
        toCurrency: String,
        rates: Map<String, Double>
    ): Double {
        if (fromCurrency.equals(toCurrency, ignoreCase = true)) return amount
        if (rates.isEmpty()) return amount

        val fromRate = rates[fromCurrency.uppercase(Locale.ROOT)] ?: 1.0
        val toRate = rates[toCurrency.uppercase(Locale.ROOT)] ?: 1.0

        return if (fromRate > 0) (amount / fromRate) * toRate else amount
    }
}

/**
 * Функция-расширение для удобного вызова у любых чисел типа Double.
 */
fun Double.formatWithCurrency(currencyCode: String = "RUB"): String {
    return CurrencyFormatter.format(this, currencyCode)
}
