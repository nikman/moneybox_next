package com.niku.analytics.ui

import androidx.compose.ui.graphics.Color
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.niku.analytics.model.AnalyticsPeriod
import com.niku.analytics.model.CategoryChartItem
import com.niku.analytics.model.MonthlyBalancePoint
import com.niku.analytics.model.TrendPoint
import com.niku.moneybox.core.database.dao.CategoryDao
import com.niku.moneybox.core.database.dao.TransactionDao
import com.niku.moneybox.core.database.dao.WalletDao
import com.niku.moneybox.core.database.model.CategoryEntity
import com.niku.moneybox.core.database.model.TransactionEntity
import com.niku.moneybox.core.database.model.TransactionType
import com.niku.moneybox.core.database.model.WalletEntity
import com.niku.preferences.AppPreferences
import com.niku.preferences.CurrencyFormatter
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * Состояние экрана аналитики.
 */
data class AnalyticsUiState(
    val selectedPeriod: AnalyticsPeriod = AnalyticsPeriod.MONTH,
    val selectedType: TransactionType = TransactionType.EXPENSE,
    val defaultCurrency: String = "RUB",
    val totalIncome: Double = 0.0,
    val totalExpense: Double = 0.0,
    val netBalance: Double = 0.0,
    val monthlyBalancePoints: List<MonthlyBalancePoint> = emptyList(),
    val categoryItems: List<CategoryChartItem> = emptyList(),
    val trendPoints: List<TrendPoint> = emptyList(),
    val isLoading: Boolean = false
)

private val ChartPalette = listOf(
    Color(0xFFE53935), Color(0xFF1E88E5), Color(0xFF43A047),
    Color(0xFFFB8C00), Color(0xFF8E24AA), Color(0xFF00ACC1),
    Color(0xFFFDD835), Color(0xFFD81B60), Color(0xFF3949AB),
    Color(0xFF7CB342), Color(0xFF00897B), Color(0xFF6D4C41)
)

/**
 * ViewModel модуля аналитики с мультивалютным пересчетом данных и фильтрацией скрытых счетов.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class AnalyticsViewModel(
    private val walletDao: WalletDao,
    private val transactionDao: TransactionDao,
    private val categoryDao: CategoryDao,
    private val appPreferences: AppPreferences
) : ViewModel() {

    private val selectedPeriodFlow = MutableStateFlow(AnalyticsPeriod.MONTH)
    private val selectedTypeFlow = MutableStateFlow(TransactionType.EXPENSE)

    private fun getPeriodRange(period: AnalyticsPeriod): Pair<Long, Long> {
        val now = System.currentTimeMillis()
        val calendar = Calendar.getInstance().apply { timeInMillis = now }

        return when (period) {
            AnalyticsPeriod.WEEK -> {
                calendar.add(Calendar.DAY_OF_YEAR, -7)
                calendar.set(Calendar.HOUR_OF_DAY, 0)
                calendar.set(Calendar.MINUTE, 0)
                Pair(calendar.timeInMillis, now)
            }
            AnalyticsPeriod.MONTH -> {
                calendar.add(Calendar.DAY_OF_YEAR, -30)
                calendar.set(Calendar.HOUR_OF_DAY, 0)
                calendar.set(Calendar.MINUTE, 0)
                Pair(calendar.timeInMillis, now)
            }
            AnalyticsPeriod.YEAR -> {
                calendar.add(Calendar.YEAR, -1)
                calendar.set(Calendar.HOUR_OF_DAY, 0)
                calendar.set(Calendar.MINUTE, 0)
                Pair(calendar.timeInMillis, now)
            }
            AnalyticsPeriod.ALL_TIME -> Pair(0L, now)
        }
    }

    private val periodTransactionsFlow = selectedPeriodFlow.flatMapLatest { period ->
        val (start, end) = getPeriodRange(period)
        transactionDao.getTransactionsByPeriodFlow(start, end)
    }

    private val categoriesFlow = categoryDao.getCategoriesFlow()
    private val walletsFlow = walletDao.getWalletsFlow()
    private val allTransactionsFlow = transactionDao.getTransactionsFlow()

    @Suppress("UNCHECKED_CAST")
    val uiState: StateFlow<AnalyticsUiState> = combine(
        selectedPeriodFlow,
        selectedTypeFlow,
        periodTransactionsFlow,
        categoriesFlow,
        walletsFlow,
        allTransactionsFlow,
        appPreferences.defaultCurrency,
        appPreferences.cachedRates
    ) { flows ->
        val period = flows[0] as AnalyticsPeriod
        val type = flows[1] as TransactionType
        val periodTransactions = flows[2] as List<TransactionEntity>
        val categories = flows[3] as List<CategoryEntity>
        val wallets = flows[4] as List<WalletEntity>
        val allTransactions = flows[5] as List<TransactionEntity>
        val defaultCurrency = flows[6] as String
        val rates = flows[7] as Map<String, Double>

        // Исключаем скроенные кошельки и транзакции по ним
        val excludedWalletIds = wallets.filter { it.excludeFromBalance }.map { it.id }.toSet()
        val activeWallets = wallets.filter { !it.excludeFromBalance }

        val activePeriodTransactions = periodTransactions.filter { tx ->
            val wId = tx.fromWalletId ?: tx.toWalletId
            wId == null || !excludedWalletIds.contains(wId)
        }

        val activeAllTransactions = allTransactions.filter { tx ->
            val wId = tx.fromWalletId ?: tx.toWalletId
            wId == null || !excludedWalletIds.contains(wId)
        }

        val walletMap = wallets.associateBy { it.id }
        val categoryMap = categories.associateBy { it.id }

        fun TransactionEntity.getAmountInDefault(): Double {
            val walletId = this.fromWalletId ?: this.toWalletId ?: return this.amount
            val currency = walletMap[walletId]?.currency ?: defaultCurrency
            return CurrencyFormatter.convert(this.amount, currency, defaultCurrency, rates)
        }

        // Расчет общих сумм только по активным счетам
        val totalIncome = activePeriodTransactions
            .filter { it.type == TransactionType.INCOME }
            .sumOf { it.getAmountInDefault() }

        val totalExpense = activePeriodTransactions
            .filter { it.type == TransactionType.EXPENSE }
            .sumOf { it.getAmountInDefault() }

        val netBalance = totalIncome - totalExpense

        val filteredTransactions = activePeriodTransactions.filter { it.type == type }
        val targetTotal = if (type == TransactionType.INCOME) totalIncome else totalExpense

        val categoryGrouped = filteredTransactions.groupBy { it.categoryId }
        val categoryItems = categoryGrouped.entries.mapIndexed { index, (catId, list) ->
            val sum = list.sumOf { it.getAmountInDefault() }
            val percent = if (targetTotal > 0) ((sum / targetTotal) * 100).toFloat() else 0f
            val categoryName = if (catId != null) {
                categoryMap[catId]?.name ?: "Категория #$catId"
            } else {
                "Без категории"
            }
            val color = ChartPalette[index % ChartPalette.size]

            CategoryChartItem(
                categoryId = catId,
                categoryName = categoryName,
                amount = sum,
                percentage = percent,
                color = color
            )
        }.sortedByDescending { it.amount }

        val trendPoints = calculateTrendPoints(activePeriodTransactions, period, walletMap, defaultCurrency, rates)
        val monthlyBalancePoints = calculate12MonthBalancePoints(activeWallets, activeAllTransactions, defaultCurrency, rates)

        AnalyticsUiState(
            selectedPeriod = period,
            selectedType = type,
            defaultCurrency = defaultCurrency,
            totalIncome = totalIncome,
            totalExpense = totalExpense,
            netBalance = netBalance,
            monthlyBalancePoints = monthlyBalancePoints,
            categoryItems = categoryItems,
            trendPoints = trendPoints,
            isLoading = false
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = AnalyticsUiState(isLoading = true)
    )

    fun selectPeriod(period: AnalyticsPeriod) {
        selectedPeriodFlow.value = period
    }

    fun selectType(type: TransactionType) {
        if (type != TransactionType.TRANSFER) {
            selectedTypeFlow.value = type
        }
    }

    private fun calculateTrendPoints(
        transactions: List<TransactionEntity>,
        period: AnalyticsPeriod,
        walletMap: Map<Long, WalletEntity>,
        defaultCurrency: String,
        rates: Map<String, Double>
    ): List<TrendPoint> {
        val dateFormat = when (period) {
            AnalyticsPeriod.WEEK -> SimpleDateFormat("EEE", Locale.getDefault())
            AnalyticsPeriod.MONTH -> SimpleDateFormat("dd.MM", Locale.getDefault())
            AnalyticsPeriod.YEAR, AnalyticsPeriod.ALL_TIME -> SimpleDateFormat("MMM", Locale.getDefault())
        }

        fun TransactionEntity.getAmountInDefault(): Double {
            val walletId = this.fromWalletId ?: this.toWalletId ?: return this.amount
            val currency = walletMap[walletId]?.currency ?: defaultCurrency
            return CurrencyFormatter.convert(this.amount, currency, defaultCurrency, rates)
        }

        return transactions.groupBy { transaction ->
            val cal = Calendar.getInstance().apply { timeInMillis = transaction.date }
            if (period == AnalyticsPeriod.YEAR || period == AnalyticsPeriod.ALL_TIME) {
                cal.set(Calendar.DAY_OF_MONTH, 1)
            }
            cal.set(Calendar.HOUR_OF_DAY, 0)
            cal.set(Calendar.MINUTE, 0)
            cal.set(Calendar.SECOND, 0)
            cal.set(Calendar.MILLISECOND, 0)
            cal.timeInMillis
        }.map { (timestamp, list) ->
            val inc = list.filter { it.type == TransactionType.INCOME }.sumOf { it.getAmountInDefault() }
            val exp = list.filter { it.type == TransactionType.EXPENSE }.sumOf { it.getAmountInDefault() }
            TrendPoint(
                label = dateFormat.format(Date(timestamp)),
                incomeAmount = inc,
                expenseAmount = exp,
                timestamp = timestamp
            )
        }.sortedBy { it.timestamp }.takeLast(10)
    }

    private fun calculate12MonthBalancePoints(
        wallets: List<WalletEntity>,
        allTransactions: List<TransactionEntity>,
        defaultCurrency: String,
        rates: Map<String, Double>
    ): List<MonthlyBalancePoint> {
        if (wallets.isEmpty()) return emptyList()

        val walletMap = wallets.associateBy { it.id }

        val currentTotalBalance = wallets.sumOf { wallet ->
            CurrencyFormatter.convert(wallet.balance, wallet.currency, defaultCurrency, rates)
        }

        fun TransactionEntity.getAmountInDefault(): Double {
            val walletId = this.fromWalletId ?: this.toWalletId ?: return this.amount
            val currency = walletMap[walletId]?.currency ?: defaultCurrency
            return CurrencyFormatter.convert(this.amount, currency, defaultCurrency, rates)
        }

        val monthFormat = SimpleDateFormat("MMM", Locale("ru"))

        val tempCal = Calendar.getInstance().apply {
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            add(Calendar.MONTH, -11)
        }

        val buckets = mutableListOf<Triple<Int, String, Long>>()
        for (i in 0 until 12) {
            val yr = tempCal.get(Calendar.YEAR)
            val lbl = monthFormat.format(tempCal.time).replaceFirstChar { it.uppercase() }
            val startTs = tempCal.timeInMillis
            buckets.add(Triple(yr, lbl, startTs))
            tempCal.add(Calendar.MONTH, 1)
        }

        val monthlyData = buckets.map { (yr, lbl, startTs) ->
            val endCal = Calendar.getInstance().apply {
                timeInMillis = startTs
                add(Calendar.MONTH, 1)
            }
            val endTs = endCal.timeInMillis

            val txInMonth = allTransactions.filter { it.date >= startTs && it.date < endTs }
            val inc = txInMonth.filter { it.type == TransactionType.INCOME }.sumOf { it.getAmountInDefault() }
            val exp = txInMonth.filter { it.type == TransactionType.EXPENSE }.sumOf { it.getAmountInDefault() }

            Triple(yr, lbl, Pair(inc, exp))
        }

        var runningBalance = currentTotalBalance
        val reversedPoints = mutableListOf<MonthlyBalancePoint>()

        for (i in 11 downTo 0) {
            val (yr, lbl, incExp) = monthlyData[i]
            val (inc, exp) = incExp
            val netChange = inc - exp

            reversedPoints.add(
                MonthlyBalancePoint(
                    monthLabel = lbl,
                    year = yr,
                    incomeAmount = inc,
                    expenseAmount = exp,
                    balanceAmount = runningBalance,
                    timestamp = System.currentTimeMillis()
                )
            )

            runningBalance -= netChange
        }

        return reversedPoints.reversed()
    }
}

/**
 * Фабрика ViewModel аналитики.
 */
class AnalyticsViewModelFactory(
    private val walletDao: WalletDao,
    private val transactionDao: TransactionDao,
    private val categoryDao: CategoryDao,
    private val appPreferences: AppPreferences
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(AnalyticsViewModel::class.java)) {
            return AnalyticsViewModel(walletDao, transactionDao, categoryDao, appPreferences) as T
        }
        throw IllegalArgumentException("Неизвестный класс ViewModel: ${modelClass.name}")
    }
}
