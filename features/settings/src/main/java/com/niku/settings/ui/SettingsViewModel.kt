package com.niku.settings.ui

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.niku.moneybox.core.database.AppDatabase
import com.niku.moneybox.core.database.backup.BackupManager
import com.niku.moneybox.core.database.csv.CsvImporter
import com.niku.network.repository.CurrencyRepository
import com.niku.preferences.AppPreferences
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel для экрана Настроек приложения.
 */
class SettingsViewModel(
    private val appPreferences: AppPreferences,
    private val database: AppDatabase,
    private val currencyRepository: CurrencyRepository? = null
) : ViewModel() {

    private val backupManager = BackupManager(database)

    val defaultCurrency: StateFlow<String> = appPreferences.defaultCurrency

    /**
     * Сменить основную базовую валюту приложения.
     */
    fun changeDefaultCurrency(newCurrency: String) {
        appPreferences.setDefaultCurrency(newCurrency)
        currencyRepository?.let { repo ->
            viewModelScope.launch {
                repo.getLatestRates(newCurrency)
            }
        }
    }

    /**
     * Экспорт резервной копии.
     */
    fun exportBackup(context: Context, uri: Uri, onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            runCatching {
                val outputStream = context.contentResolver.openOutputStream(uri)
                    ?: throw IllegalStateException("Не удалось открыть файл для записи")
                backupManager.exportBackup(outputStream).getOrThrow()
            }.onSuccess {
                onResult(true, "Резервная копия успешно экспортирована")
            }.onFailure { error ->
                onResult(false, "Ошибка экспорта: ${error.localizedMessage}")
            }
        }
    }

    /**
     * Импорт резервной копии из JSON.
     */
    fun importBackup(context: Context, uri: Uri, onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            runCatching {
                val inputStream = context.contentResolver.openInputStream(uri)
                    ?: throw IllegalStateException("Не удалось открыть файл для чтения")
                backupManager.importBackup(inputStream).getOrThrow()
            }.onSuccess {
                onResult(true, "Данные базы успешно восстановлены")
            }.onFailure { error ->
                onResult(false, "Ошибка импорта: ${error.localizedMessage}")
            }
        }
    }

    /**
     * Умный импорт выписки из CSV-файла.
     */
    fun importCsv(context: Context, uri: Uri, onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            runCatching {
                val inputStream = context.contentResolver.openInputStream(uri)
                    ?: throw IllegalStateException("Не удалось открыть CSV файл для чтения")
                val csvImporter = CsvImporter(database)
                csvImporter.importCsv(inputStream).getOrThrow()
            }.onSuccess { result ->
                val message = "Импортировано: ${result.transactionsCount} операций, ${result.walletsCount} кошельков, ${result.categoriesCount} категорий, ${result.tagsCount} тегов."
                onResult(true, message)
            }.onFailure { error ->
                onResult(false, "Ошибка импорта CSV: ${error.localizedMessage}")
            }
        }
    }
}

/**
 * Фабрика ViewModel настроек.
 */
class SettingsViewModelFactory(
    private val appPreferences: AppPreferences,
    private val database: AppDatabase,
    private val currencyRepository: CurrencyRepository? = null
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(SettingsViewModel::class.java)) {
            return SettingsViewModel(appPreferences, database, currencyRepository) as T
        }
        throw IllegalArgumentException("Неизвестный класс ViewModel: ${modelClass.name}")
    }
}
