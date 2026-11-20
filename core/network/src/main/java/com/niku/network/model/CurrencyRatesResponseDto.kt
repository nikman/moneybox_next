package com.niku.network.model

import com.google.gson.annotations.SerializedName

/**
 * DTO модель ответа от API курсов валют (https://open.er-api.com/v6/latest/RUB).
 */
data class CurrencyRatesResponseDto(
    @SerializedName("result")
    val result: String,

    @SerializedName("base_code")
    val baseCode: String,

    @SerializedName("time_last_update_unix")
    val timeLastUpdateUnix: Long? = null,

    @SerializedName("rates")
    val rates: Map<String, Double>? = null
)
