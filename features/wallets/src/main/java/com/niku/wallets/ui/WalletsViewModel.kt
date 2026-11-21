package com.niku.wallets.ui

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.niku.moneybox.core.database.AppDatabase
import com.niku.moneybox.core.database.backup.BackupManager
import com.niku.moneybox.core.database.csv.CsvImporter
import com.niku.moneybox.core.database.dao.CategoryDao
import com.niku.moneybox.core.database.dao.TransactionDao
import com.niku.moneybox.core.database.dao.WalletDao
import com.niku.moneybox.core.database.model.CategoryEntity
import com.niku.moneybox.core.database.model.CategoryType
import com.niku.moneybox.core.database.model.PaymentTemplateEntity
import com.niku.moneybox.core.database.model.TagEntity
import com.niku.moneybox.core.database.model.TransactionEntity
import com.niku.moneybox.core.database.model.TransactionType
import com.niku.moneybox.core.database.model.TransactionWithTags
import com.niku.moneybox.core.database.model.WalletEntity
import com.niku.moneybox.core.database.model.WalletType
import com.niku.preferences.AppPreferences
import com.niku.preferences.CurrencyFormatter
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * Состояние UI для экрана управления кошельками.
 */
data class WalletsUiState(
    val wallets: List<WalletEntity> = emptyList(),
    val transactions: List<TransactionEntity> = emptyList(),
    val transactionsWithTags: List<TransactionWithTags> = emptyList(),
    val categories: List<CategoryEntity> = emptyList(),
    val templates: List<PaymentTemplateEntity> = emptyList(),
    val tags: List<TagEntity> = emptyList(),
    val defaultCurrency: String = "RUB",
    val cachedRates: Map<String, Double> = emptyMap(),
    val totalBalance: Double = 0.0,
    val isLoading: Boolean = false,
    val userMessage: String? = null
)

/**
 * ViewModel для управления кошельками, транзакциями, переводами, шаблонами, тегами и бэкапом.
 */
class WalletsViewModel(
    private val walletDao: WalletDao,
    private val transactionDao: TransactionDao,
    private val categoryDao: CategoryDao,
    private val database: AppDatabase,
    private val appPreferences: AppPreferences
) : ViewModel() {

    private val templateDao = database.paymentTemplateDao()
    private val backupManager = BackupManager(database)

    private val walletsFlow = walletDao.getWalletsFlow()
    private val transactionsWithTagsFlow = transactionDao.getTransactionsWithTagsFlow()
    private val categoriesFlow = categoryDao.getCategoriesFlow()
    private val templatesFlow = templateDao.getTemplatesFlow()
    private val tagsFlow = transactionDao.getTagsFlow()

    /**
     * Основной StateFlow состояния экрана.
     */
    @Suppress("UNCHECKED_CAST")
    val uiState: StateFlow<WalletsUiState> = combine(
        walletsFlow,
        transactionsWithTagsFlow,
        categoriesFlow,
        templatesFlow,
        tagsFlow,
        appPreferences.defaultCurrency,
        appPreferences.cachedRates
    ) { flows ->
        val wallets = flows[0] as List<WalletEntity>
        val txsWithTags = flows[1] as List<TransactionWithTags>
        val categories = flows[2] as List<CategoryEntity>
        val templates = flows[3] as List<PaymentTemplateEntity>
        val tags = flows[4] as List<TagEntity>
        val defaultCurrency = flows[5] as String
        val rates = flows[6] as Map<String, Double>

        val transactions = txsWithTags.map { it.transaction }

        // Исключаем кошельки с флагом excludeFromBalance из общего итогового баланса
        val total = wallets
            .filter { !it.excludeFromBalance }
            .sumOf { wallet ->
                CurrencyFormatter.convert(wallet.balance, wallet.currency, defaultCurrency, rates)
            }

        WalletsUiState(
            wallets = wallets,
            transactions = transactions,
            transactionsWithTags = txsWithTags,
            categories = categories,
            templates = templates,
            tags = tags,
            defaultCurrency = defaultCurrency,
            cachedRates = rates,
            totalBalance = total,
            isLoading = false
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = WalletsUiState(isLoading = true)
    )

    fun createTag(name: String, onCreated: ((Long) -> Unit)? = null) {
        val cleanName = name.trim()
        if (cleanName.isBlank()) return
        viewModelScope.launch {
            runCatching {
                val tagId = transactionDao.insertTag(TagEntity(name = cleanName))
                onCreated?.invoke(tagId)
            }
        }
    }

    fun createTemplate(
        name: String,
        type: TransactionType,
        walletId: Long,
        categoryId: Long?,
        amount: Double
    ) {
        viewModelScope.launch {
            runCatching {
                val template = PaymentTemplateEntity(
                    name = name,
                    walletId = walletId,
                    categoryId = categoryId,
                    amount = amount,
                    type = type
                )
                templateDao.insertTemplate(template)
            }
        }
    }

    fun updateTemplate(template: PaymentTemplateEntity) {
        viewModelScope.launch {
            runCatching {
                templateDao.updateTemplate(template)
            }
        }
    }

    fun deleteTemplate(template: PaymentTemplateEntity) {
        viewModelScope.launch {
            runCatching {
                templateDao.deleteTemplate(template)
            }
        }
    }

    fun addCategory(name: String, type: CategoryType, icon: String, color: String) {
        viewModelScope.launch {
            runCatching {
                val category = CategoryEntity(
                    name = name,
                    type = type,
                    icon = icon,
                    color = color
                )
                categoryDao.insertCategory(category)
            }
        }
    }

    fun deleteCategory(category: CategoryEntity) {
        viewModelScope.launch {
            runCatching {
                categoryDao.deleteCategory(category)
            }
        }
    }

    fun createWallet(
        name: String,
        type: WalletType,
        currency: String = "RUB",
        initialBalance: Double = 0.0,
        excludeFromBalance: Boolean = false
    ) {
        viewModelScope.launch {
            runCatching {
                val wallet = WalletEntity(
                    name = name,
                    type = type,
                    currency = currency,
                    balance = initialBalance,
                    excludeFromBalance = excludeFromBalance
                )
                walletDao.insertWallet(wallet)
            }
        }
    }

    fun updateWallet(wallet: WalletEntity) {
        viewModelScope.launch {
            runCatching {
                walletDao.updateWallet(wallet)
            }
        }
    }

    fun deleteWallet(walletId: Long) {
        viewModelScope.launch {
            runCatching {
                walletDao.deleteWalletWithTransactions(walletId)
            }
        }
    }

    fun addTransaction(
        amount: Double,
        type: TransactionType,
        fromWalletId: Long?,
        toWalletId: Long?,
        categoryId: Long? = null,
        comment: String? = null,
        date: Long = System.currentTimeMillis(),
        selectedTagIds: List<Long> = emptyList()
    ) {
        viewModelScope.launch {
            runCatching {
                val transaction = TransactionEntity(
                    fromWalletId = fromWalletId,
                    toWalletId = toWalletId,
                    amount = amount,
                    type = type,
                    categoryId = categoryId,
                    date = date,
                    comment = comment
                )
                transactionDao.insertTransactionWithTags(transaction, selectedTagIds)
            }
        }
    }

    fun addTransaction(
        amount: Double,
        type: TransactionType,
        walletId: Long,
        categoryId: Long? = null,
        comment: String? = null,
        date: Long = System.currentTimeMillis(),
        selectedTagIds: List<Long> = emptyList()
    ) {
        addTransaction(
            amount = amount,
            type = type,
            fromWalletId = if (type == TransactionType.EXPENSE) walletId else null,
            toWalletId = if (type == TransactionType.INCOME) walletId else null,
            categoryId = categoryId,
            comment = comment,
            date = date,
            selectedTagIds = selectedTagIds
        )
    }

    fun deleteTransaction(transaction: TransactionEntity) {
        viewModelScope.launch {
            runCatching {
                transactionDao.deleteTransactionAndRevertBalance(transaction)
            }
        }
    }

    fun updateTransaction(
        oldTransaction: TransactionEntity,
        newTransaction: TransactionEntity,
        selectedTagIds: List<Long> = emptyList()
    ) {
        viewModelScope.launch {
            runCatching {
                transactionDao.updateTransactionWithTags(oldTransaction, newTransaction, selectedTagIds)
            }
        }
    }

    fun transferBetweenWallets(
        fromWalletId: Long,
        toWalletId: Long,
        amount: Double,
        comment: String? = null,
        date: Long = System.currentTimeMillis()
    ) {
        if (fromWalletId == toWalletId) return

        viewModelScope.launch {
            runCatching {
                val transferTransaction = TransactionEntity(
                    fromWalletId = fromWalletId,
                    toWalletId = toWalletId,
                    amount = amount,
                    type = TransactionType.TRANSFER,
                    categoryId = null,
                    date = date,
                    comment = comment ?: "Перевод между счетами"
                )
                transactionDao.insertTransactionAndUpdateBalance(transferTransaction)
            }
        }
    }

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

    fun importCsv(context: Context, uri: Uri, onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            runCatching {
                val inputStream = context.contentResolver.openInputStream(uri)
                    ?: throw IllegalStateException("Не удалось открыть CSV файл для чтения")
                val csvImporter = CsvImporter(database)
                csvImporter.importCsv(inputStream).getOrThrow()
            }.onSuccess { result ->
                val message = "Успешно импортировано из CSV:\n• Операций: ${result.transactionsCount}\n• Счетов: ${result.walletsCount}\n• Категорий: ${result.categoriesCount}\n• Тегов: ${result.tagsCount}"
                onResult(true, message)
            }.onFailure { error ->
                onResult(false, "Ошибка импорта CSV: ${error.localizedMessage}")
            }
        }
    }
}

/**
 * Фабрика ViewModel.
 */
class WalletsViewModelFactory(
    private val walletDao: WalletDao,
    private val transactionDao: TransactionDao,
    private val categoryDao: CategoryDao,
    private val database: AppDatabase,
    private val appPreferences: AppPreferences
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(WalletsViewModel::class.java)) {
            return WalletsViewModel(walletDao, transactionDao, categoryDao, database, appPreferences) as T
        }
        throw IllegalArgumentException("Неизвестный класс ViewModel: ${modelClass.name}")
    }
}
