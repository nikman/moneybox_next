package com.niku.currencies.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.niku.currencies.model.CurrencyDisplayItem
import com.niku.network.repository.CurrencyRepository
import com.niku.network.util.NetworkResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Состояние экрана курсов валют.
 */
data class CurrenciesUiState(
    val baseCurrency: String = "RUB",
    val inputAmount: Double = 1000.0,
    val currencyItems: List<CurrencyDisplayItem> = emptyList(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)

/**
 * ViewModel для загрузки курсов валют через репозиторий модуля :core:network
 * и онлайн-пересчета сумм в конвертере.
 */
class CurrenciesViewModel(
    private val repository: CurrencyRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(CurrenciesUiState())
    val uiState: StateFlow<CurrenciesUiState> = _uiState.asStateFlow()

    // Метаданные популярных валют (Название и Флаг страны)
    private val currencyMetadata = mapOf(
        "RUB" to Pair("Российский рубль", "🇷🇺"),
        "USD" to Pair("Доллар США", "🇺🇸"),
        "EUR" to Pair("Евро", "🇪🇺"),
        "CNY" to Pair("Китайский юань", "🇨🇳"),
        "KZT" to Pair("Казахстанский тенге", "🇰🇿"),
        "BYN" to Pair("Белорусский рубль", "🇧🇾"),
        "GBP" to Pair("Британский фунт", "🇬🇧"),
        "TRY" to Pair("Турецкая лира", "🇹🇷"),
        "AED" to Pair("Дирхам ОАЭ", "🇦🇪")
    )

    init {
        loadCurrencyRates()
    }

    /**
     * Загрузка актуальных курсов валют из сети.
     */
    fun loadCurrencyRates() {
        val currentBase = _uiState.value.baseCurrency
        val currentAmount = _uiState.value.inputAmount

        _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)

        viewModelScope.launch {
            when (val result = repository.getLatestRates(currentBase)) {
                is NetworkResult.Success -> {
                    val ratesMap = result.data.rates ?: emptyMap()
                    val items = currencyMetadata.filterKeys { it != currentBase }.mapNotNull { (code, meta) ->
                        val rate = ratesMap[code] ?: return@mapNotNull null
                        CurrencyDisplayItem(
                            code = code,
                            name = meta.first,
                            flagEmoji = meta.second,
                            rate = rate,
                            convertedAmount = currentAmount * rate
                        )
                    }
                    _uiState.value = _uiState.value.copy(
                        currencyItems = items,
                        isLoading = false,
                        errorMessage = null
                    )
                }
                is NetworkResult.Error -> {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = result.message
                    )
                }
            }
        }
    }

    /**
     * Изменение базовой валюты конвертации.
     */
    fun changeBaseCurrency(newBase: String) {
        if (_uiState.value.baseCurrency != newBase) {
            _uiState.value = _uiState.value.copy(baseCurrency = newBase)
            loadCurrencyRates()
        }
    }

    /**
     * Пересчет сумм при вводе нового значения пользователем.
     */
    fun updateInputAmount(newAmountText: String) {
        val amount = newAmountText.toDoubleOrNull() ?: 0.0
        val currentItems = _uiState.value.currencyItems.map { item ->
            item.copy(convertedAmount = amount * item.rate)
        }
        _uiState.value = _uiState.value.copy(
            inputAmount = amount,
            currencyItems = currentItems
        )
    }
}

/**
 * Фабрика ViewModel курсов валют.
 */
class CurrenciesViewModelFactory(
    private val repository: CurrencyRepository
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(CurrenciesViewModel::class.java)) {
            return CurrenciesViewModel(repository) as T
        }
        throw IllegalArgumentException("Неизвестный класс ViewModel: ${modelClass.name}")
    }
}
