package com.niku.preferences

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Хранилище настроек приложения (SharedPreferences + StateFlow для реактивности).
 *
 * Отвечает за:
 * 1. Основную валюту приложения (RUB, USD, EUR и т.д.).
 * 2. Офлайн-кэш актуальных курсов валют для мгновенного пересчета при холодном запуске.
 */
class AppPreferences private constructor(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val gson = Gson()

    private val _defaultCurrency = MutableStateFlow(
        prefs.getString(KEY_DEFAULT_CURRENCY, DEFAULT_CURRENCY_VALUE) ?: DEFAULT_CURRENCY_VALUE
    )
    val defaultCurrency: StateFlow<String> = _defaultCurrency.asStateFlow()

    private val _cachedRates = MutableStateFlow<Map<String, Double>>(loadCachedRates())
    val cachedRates: StateFlow<Map<String, Double>> = _cachedRates.asStateFlow()

    /**
     * Установить основную базовую валюту приложения.
     */
    fun setDefaultCurrency(currencyCode: String) {
        prefs.edit().putString(KEY_DEFAULT_CURRENCY, currencyCode).apply()
        _defaultCurrency.value = currencyCode
    }

    /**
     * Сохранить загруженные из сети курсы валют в кэш.
     */
    fun saveCachedRates(rates: Map<String, Double>) {
        val json = gson.toJson(rates)
        prefs.edit().putString(KEY_CACHED_RATES, json).apply()
        _cachedRates.value = rates
    }

    private fun loadCachedRates(): Map<String, Double> {
        val json = prefs.getString(KEY_CACHED_RATES, null) ?: return emptyMap()
        return try {
            val type = object : TypeToken<Map<String, Double>>() {}.type
            gson.fromJson(json, type) ?: emptyMap()
        } catch (e: Exception) {
            emptyMap()
        }
    }

    companion object {
        private const val PREFS_NAME = "moneybox_preferences"
        private const val KEY_DEFAULT_CURRENCY = "key_default_currency"
        private const val KEY_CACHED_RATES = "key_cached_rates"
        private const val DEFAULT_CURRENCY_VALUE = "RUB"

        @Volatile
        private var instance: AppPreferences? = null

        fun getInstance(context: Context): AppPreferences {
            return instance ?: synchronized(this) {
                instance ?: AppPreferences(context.applicationContext).also { instance = it }
            }
        }
    }
}
