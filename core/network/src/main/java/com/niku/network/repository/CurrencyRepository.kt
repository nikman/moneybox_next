package com.niku.network.repository

import com.niku.network.api.CurrencyApiService
import com.niku.network.model.CurrencyRatesResponseDto
import com.niku.network.util.NetworkResult
import com.niku.preferences.AppPreferences
import java.io.IOException

/**
 * Интерфейс репозитория котировок валют.
 */
interface CurrencyRepository {
    suspend fun getLatestRates(baseCurrency: String): NetworkResult<CurrencyRatesResponseDto>
}

/**
 * Реализация репозитория с обработкой ошибок и офлайн-кэшированием в AppPreferences.
 */
class CurrencyRepositoryImpl(
    private val apiService: CurrencyApiService,
    private val appPreferences: AppPreferences? = null
) : CurrencyRepository {

    override suspend fun getLatestRates(baseCurrency: String): NetworkResult<CurrencyRatesResponseDto> {
        return try {
            val response = apiService.getLatestRates(baseCurrency)
            if (response.result.equals("success", ignoreCase = true) && response.rates != null) {
                appPreferences?.saveCachedRates(response.rates!!)
                NetworkResult.Success(response)
            } else {
                NetworkResult.Error("Сервер вернул неполные данные о курсах валют")
            }
        } catch (e: IOException) {
            val cached = appPreferences?.cachedRates?.value
            if (!cached.isNullOrEmpty()) {
                NetworkResult.Success(
                    CurrencyRatesResponseDto(
                        result = "success",
                        baseCode = baseCurrency,
                        rates = cached
                    )
                )
            } else {
                NetworkResult.Error("Отсутствует интернет-соединение", e)
            }
        } catch (e: Exception) {
            NetworkResult.Error("Ошибка получения данных: ${e.localizedMessage}", e)
        }
    }
}
