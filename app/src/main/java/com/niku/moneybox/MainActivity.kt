package com.niku.moneybox

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.CurrencyExchange
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.room.Room
import com.niku.analytics.ui.AnalyticsScreen
import com.niku.analytics.ui.AnalyticsViewModel
import com.niku.analytics.ui.AnalyticsViewModelFactory
import com.niku.auth.ui.AuthScreen
import com.niku.currencies.ui.CurrenciesScreen
import com.niku.currencies.ui.CurrenciesViewModel
import com.niku.currencies.ui.CurrenciesViewModelFactory
import com.niku.moneybox.core.database.AppDatabase
import com.niku.moneybox.ui.theme.MoneyboxTheme
import com.niku.network.RetrofitClient
import com.niku.preferences.AppPreferences
import com.niku.settings.ui.SettingsScreen
import com.niku.settings.ui.SettingsViewModel
import com.niku.settings.ui.SettingsViewModelFactory
import com.niku.wallets.ui.TemplatesScreen
import com.niku.wallets.ui.WalletsScreen
import com.niku.wallets.ui.WalletsViewModel
import com.niku.wallets.ui.WalletsViewModelFactory

/**
 * Модель элементов нижней панели навигации (Bottom Navigation Bar).
 */
sealed class BottomNavItem(
    val route: String,
    val title: String,
    val icon: ImageVector
) {
    data object Wallets : BottomNavItem("wallets", "Кошельки", Icons.Default.AccountBalanceWallet)
    data object Templates : BottomNavItem("templates", "Шаблоны", Icons.Default.Bookmark)
    data object Analytics : BottomNavItem("analytics", "Отчеты", Icons.Default.BarChart)
    data object Currencies : BottomNavItem("currencies", "Валюты", Icons.Default.CurrencyExchange)
    data object Settings : BottomNavItem("settings", "Настройки", Icons.Default.Settings)
}

/**
 * Главная Activity приложения MoneyBox.
 *
 * Связывает все модули (:core:database, :core:network, :core:preferences, :features:auth,
 * :features:wallets, :features:analytics, :features:currencies, :features:settings).
 */
class MainActivity : FragmentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // 1. Инициализация базы данных Room
        val database = Room.databaseBuilder(
            applicationContext,
            AppDatabase::class.java,
            AppDatabase.DATABASE_NAME
        )
            .addCallback(AppDatabase.PREPOPULATE_CALLBACK)
            .fallbackToDestructiveMigration(true)
            .build()

        // 2. Хранилище настроек приложения из :core:preferences
        val appPreferences = AppPreferences.getInstance(applicationContext)

        // 3. Сетевой репозиторий из :core:network с поддержкой кэширования
        val currencyRepository = RetrofitClient.getCurrencyRepository(applicationContext)

        setContent {
            MoneyboxTheme {
                val navController = rememberNavController()

                // Инициализация ViewModels для каждого экрана
                val walletsViewModelFactory = remember {
                    WalletsViewModelFactory(
                        walletDao = database.walletDao(),
                        transactionDao = database.transactionDao(),
                        categoryDao = database.categoryDao(),
                        database = database,
                        appPreferences = appPreferences
                    )
                }
                val walletsViewModel: WalletsViewModel = viewModel(factory = walletsViewModelFactory)

                val analyticsViewModelFactory = remember {
                    AnalyticsViewModelFactory(
                        walletDao = database.walletDao(),
                        transactionDao = database.transactionDao(),
                        categoryDao = database.categoryDao(),
                        appPreferences = appPreferences
                    )
                }
                val analyticsViewModel: AnalyticsViewModel = viewModel(factory = analyticsViewModelFactory)

                val currenciesViewModelFactory = remember {
                    CurrenciesViewModelFactory(repository = currencyRepository)
                }
                val currenciesViewModel: CurrenciesViewModel = viewModel(factory = currenciesViewModelFactory)

                val settingsViewModelFactory = remember {
                    SettingsViewModelFactory(
                        appPreferences = appPreferences,
                        database = database,
                        currencyRepository = currencyRepository
                    )
                }
                val settingsViewModel: SettingsViewModel = viewModel(factory = settingsViewModelFactory)

                // Отслеживание текущего маршрута
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentRoute = navBackStackEntry?.destination?.route

                // Нижнее меню отображается ТОЛЬКО если маршрут не "auth"
                val showBottomBar = currentRoute != null && currentRoute != "auth"

                val bottomNavItems = remember {
                    listOf(
                        BottomNavItem.Wallets,
                        BottomNavItem.Templates,
                        BottomNavItem.Analytics,
                        BottomNavItem.Currencies,
                        BottomNavItem.Settings
                    )
                }

                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    bottomBar = {
                        if (showBottomBar) {
                            NavigationBar {
                                bottomNavItems.forEach { item ->
                                    val selected = currentRoute == item.route
                                    NavigationBarItem(
                                        selected = selected,
                                        onClick = {
                                            if (currentRoute != item.route) {
                                                navController.navigate(item.route) {
                                                    popUpTo(navController.graph.findStartDestination().id) {
                                                        saveState = true
                                                    }
                                                    launchSingleTop = true
                                                    restoreState = true
                                                }
                                            }
                                        },
                                        icon = { Icon(imageVector = item.icon, contentDescription = item.title) },
                                        label = { Text(item.title) }
                                    )
                                }
                            }
                        }
                    }
                ) { innerPadding ->
                    Surface(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                    ) {
                        NavHost(
                            navController = navController,
                            startDestination = "auth"
                        ) {
                            // Экран 1: Аутентификация по биометрии (:features:auth)
                            composable("auth") {
                                AuthScreen(
                                    onAuthSuccess = {
                                        navController.navigate("wallets") {
                                            popUpTo("auth") {
                                                inclusive = true
                                            }
                                        }
                                    },
                                )
                            }

                            // Экран 2: Управление кошельками (:features:wallets)
                            composable("wallets") {
                                WalletsScreen(viewModel = walletsViewModel)
                            }

                            // Экран 3: Шаблоны платежей (:features:wallets)
                            composable("templates") {
                                TemplatesScreen(viewModel = walletsViewModel)
                            }

                            // Экран 4: Аналитика и отчёты (:features:analytics)
                            composable("analytics") {
                                AnalyticsScreen(viewModel = analyticsViewModel)
                            }

                            // Экран 5: Курсы валют и конвертер (:features:currencies)
                            composable("currencies") {
                                CurrenciesScreen(viewModel = currenciesViewModel)
                            }

                            // Экран 6: Настройки приложения (:features:settings)
                            composable("settings") {
                                SettingsScreen(viewModel = settingsViewModel)
                            }
                        }
                    }
                }
            }
        }
    }
}
