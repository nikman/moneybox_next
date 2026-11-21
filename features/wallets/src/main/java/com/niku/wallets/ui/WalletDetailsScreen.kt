package com.niku.wallets.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.niku.moneybox.core.database.model.TransactionEntity
import com.niku.moneybox.core.database.model.WalletEntity
import com.niku.moneybox.core.database.model.WalletType
import com.niku.preferences.CurrencyFormatter
import com.niku.wallets.ui.dialogs.AddTransactionDialog
import com.niku.wallets.ui.dialogs.AddWalletDialog
import java.util.Locale

/**
 * Пользовательское отображение типов кошельков на русском языке.
 */
fun WalletType.getDisplayName(): String = when (this) {
    WalletType.CASH -> "Наличные"
    WalletType.DEBIT_CARD -> "Дебетовая карта"
    WalletType.CREDIT_CARD -> "Кредитная карта"
    WalletType.SAVINGS -> "Вклад / Сбережения"
    WalletType.INVESTMENT -> "Инвестиционный счет"
    WalletType.CRYPTO -> "Криптовалюта"
    WalletType.OTHER -> "Другое"
}

/**
 * Экран детализации конкретного кошелька/счета.
 * Отображает актуальный баланс, тип счета, статус учета в общем балансе,
 * позволяет быстро добавить операцию, отредактировать кошелек и просмотреть историю операций.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WalletDetailsScreen(
    walletId: Long,
    viewModel: WalletsViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Обработка системной кнопки «Назад»
    BackHandler(onBack = onBack)

    val uiState by viewModel.uiState.collectAsState()
    val wallet = uiState.wallets.find { it.id == walletId }

    // Если кошелек был удален, автоматически возвращаемся назад
    if (wallet == null) {
        LaunchedEffect(Unit) {
            onBack()
        }
        return
    }

    var showEditWalletDialog by remember { mutableStateOf(false) }
    var showAddTransactionDialog by remember { mutableStateOf(false) }
    var pendingEditTransaction by remember { mutableStateOf<TransactionEntity?>(null) }
    var pendingDeleteTransaction by remember { mutableStateOf<TransactionEntity?>(null) }

    val categoryMap = remember(uiState.categories) {
        uiState.categories.associateBy { it.id }
    }
    val txWithTagsMap = remember(uiState.transactionsWithTags) {
        uiState.transactionsWithTags.associateBy { it.transaction.id }
    }

    // Фильтруем последние операции (до 20 штук), относящиеся к данному кошельку
    val walletTransactions = remember(uiState.transactions, walletId) {
        uiState.transactions
            .filter { it.fromWalletId == walletId || it.toWalletId == walletId }
            .take(20)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = wallet.name,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Назад на главный экран"
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { showEditWalletDialog = true }) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Редактировать кошелек"
                        )
                    }
                }
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showAddTransactionDialog = true },
                icon = { Icon(imageVector = Icons.Default.Add, contentDescription = null) },
                text = { Text("Добавить операцию", fontWeight = FontWeight.Bold) },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            )
        },
        modifier = modifier
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Карточка информации о счете
            WalletInfoCard(
                wallet = wallet,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            )

            // Заметная кнопка добавления операции
            Button(
                onClick = { showAddTransactionDialog = true },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(imageVector = Icons.Default.Add, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Добавить операцию", fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Заголовок списка операций
            Text(
                text = "Последние операции по счету",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
            )

            // Список операций кошелька
            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 88.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (walletTransactions.isEmpty()) {
                    item {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 12.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                        ) {
                            Text(
                                text = "По этому счету пока нет операций.\nНажмите «Добавить операцию», чтобы внести расход или доход.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(20.dp)
                            )
                        }
                    }
                } else {
                    items(walletTransactions, key = { it.id }) { transaction ->
                        val category = transaction.categoryId?.let { categoryMap[it] }
                        val tags = txWithTagsMap[transaction.id]?.tags ?: emptyList()

                        val dismissState = rememberSwipeToDismissBoxState(
                            confirmValueChange = { dismissValue ->
                                if (dismissValue == SwipeToDismissBoxValue.EndToStart) {
                                    pendingDeleteTransaction = transaction
                                    false
                                } else {
                                    false
                                }
                            }
                        )

                        SwipeToDismissBox(
                            state = dismissState,
                            backgroundContent = {
                                val color = if (dismissState.targetValue == SwipeToDismissBoxValue.EndToStart) {
                                    MaterialTheme.colorScheme.errorContainer
                                } else {
                                    Color.Transparent
                                }
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(color, shape = RoundedCornerShape(12.dp))
                                        .padding(horizontal = 16.dp),
                                    contentAlignment = Alignment.CenterEnd
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = "Удалить операцию",
                                        tint = MaterialTheme.colorScheme.onErrorContainer
                                    )
                                }
                            },
                            content = {
                                TransactionItem(
                                    transaction = transaction,
                                    category = category,
                                    tags = tags,
                                    walletCurrency = wallet.currency,
                                    currentWalletId = wallet.id,
                                    onClick = {
                                        pendingEditTransaction = transaction
                                    }
                                )
                            }
                        )
                    }
                }
            }
        }
    }

    // Диалог редактирования кошелька
    if (showEditWalletDialog) {
        AddWalletDialog(
            initialWallet = wallet,
            defaultCurrency = uiState.defaultCurrency,
            onDismiss = { showEditWalletDialog = false },
            onDelete = {
                viewModel.deleteWallet(wallet.id)
                showEditWalletDialog = false
                onBack()
            },
            onConfirm = { name, type, currency, balance, excludeFromBalance ->
                val updated = wallet.copy(
                    name = name,
                    type = type,
                    currency = currency,
                    balance = balance,
                    excludeFromBalance = excludeFromBalance
                )
                viewModel.updateWallet(updated)
                showEditWalletDialog = false
            }
        )
    }

    // Диалог добавления новой операции с предзаполненным walletId
    if (showAddTransactionDialog) {
        AddTransactionDialog(
            wallets = uiState.wallets,
            categories = uiState.categories,
            allTags = uiState.tags,
            initialWalletId = wallet.id,
            onDismiss = { showAddTransactionDialog = false },
            onCreateNewTag = { tagName, callback ->
                viewModel.createTag(tagName, callback)
            },
            onConfirm = { amount, type, fromWalletId, toWalletId, categoryId, comment, date, tagIds ->
                viewModel.addTransaction(amount, type, fromWalletId, toWalletId, categoryId, comment, date, tagIds)
                showAddTransactionDialog = false
            }
        )
    }

    // Диалог редактирования операции
    if (pendingEditTransaction != null) {
        val oldTx = pendingEditTransaction!!
        val currentTxTags = txWithTagsMap[oldTx.id]?.tags?.map { it.id } ?: emptyList()

        AddTransactionDialog(
            wallets = uiState.wallets,
            categories = uiState.categories,
            allTags = uiState.tags,
            initialTransaction = oldTx,
            initialSelectedTagIds = currentTxTags,
            isEditing = true,
            onDismiss = { pendingEditTransaction = null },
            onCreateNewTag = { tagName, callback ->
                viewModel.createTag(tagName, callback)
            },
            onConfirm = { amount, type, fromWalletId, toWalletId, categoryId, comment, date, tagIds ->
                val newTx = oldTx.copy(
                    amount = amount,
                    type = type,
                    fromWalletId = fromWalletId,
                    toWalletId = toWalletId,
                    categoryId = categoryId,
                    comment = comment,
                    date = date
                )
                viewModel.updateTransaction(oldTx, newTx, tagIds)
                pendingEditTransaction = null
            }
        )
    }

    // Диалог подтверждения удаления операции
    if (pendingDeleteTransaction != null) {
        val txToDelete = pendingDeleteTransaction!!
        val currencySymbol = CurrencyFormatter.getSymbol(wallet.currency)
        AlertDialog(
            onDismissRequest = { pendingDeleteTransaction = null },
            title = { Text("Удаление операции") },
            text = {
                Text(
                    "Удалить транзакцию на сумму ${
                        String.format(
                            Locale.getDefault(),
                            "%,.2f %s",
                            txToDelete.amount,
                            currencySymbol
                        )
                    }? Баланс кошелька будет пересчитан."
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val target = pendingDeleteTransaction
                        pendingDeleteTransaction = null
                        target?.let { viewModel.deleteTransaction(it) }
                    }
                ) {
                    Text("Удалить", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingDeleteTransaction = null }) {
                    Text("Отмена")
                }
            }
        )
    }
}

/**
 * Карточка с ключевой информацией о кошельке:
 * актуальный баланс с пиктограммой валюты, тип счета и статус исключения из баланса.
 */
@Composable
fun WalletInfoCard(
    wallet: WalletEntity,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Текущий баланс",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                )

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f)
                ) {
                    Text(
                        text = wallet.type.getDisplayName(),
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = String.format(
                    Locale.getDefault(),
                    "%,.2f %s",
                    wallet.balance,
                    CurrencyFormatter.getSymbol(wallet.currency)
                ),
                style = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )

            if (wallet.excludeFromBalance) {
                Spacer(modifier = Modifier.height(12.dp))
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.8f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "Не учитывается в общем балансе",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}
