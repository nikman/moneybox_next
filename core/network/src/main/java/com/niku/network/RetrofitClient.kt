package com.niku.network

import android.content.Context
import com.niku.network.api.CurrencyApiService
import com.niku.network.repository.CurrencyRepository
import com.niku.network.repository.CurrencyRepositoryImpl
import com.niku.preferences.AppPreferences
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

/**
 * Сетевой клиент Retrofit модуля :core:network.
 */
object RetrofitClient {

    private const val BASE_URL = "https://open.er-api.com/"

    private val okHttpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .addInterceptor(HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BODY
            })
            .build()
    }

    val currencyApiService: CurrencyApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(CurrencyApiService::class.java)
    }

    fun getCurrencyRepository(context: Context): CurrencyRepository {
        val prefs = AppPreferences.getInstance(context)
        return CurrencyRepositoryImpl(currencyApiService, prefs)
    }
}
