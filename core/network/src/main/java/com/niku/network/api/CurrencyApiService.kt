package com.niku.network.api

import com.niku.network.model.CurrencyRatesResponseDto
import retrofit2.http.GET
import retrofit2.http.Path

/**
 * Retrofit интерфейс для выполнения запросов к API котировок валют.
 */
interface CurrencyApiService {

    @GET("v6/latest/{baseCurrency}")
    suspend fun getLatestRates(
        @Path("baseCurrency") baseCurrency: String
    ): CurrencyRatesResponseDto
}
