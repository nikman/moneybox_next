package com.niku.wallets.ui

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.niku.moneybox.core.database.model.CategoryEntity
import com.niku.moneybox.core.database.model.TagEntity
import com.niku.moneybox.core.database.model.TransactionEntity
import com.niku.moneybox.core.database.model.TransactionType
import com.niku.moneybox.core.database.model.WalletEntity
import com.niku.preferences.CurrencyFormatter
import com.niku.wallets.ui.dialogs.AddTransactionDialog
import com.niku.wallets.ui.dialogs.AddWalletDialog
import com.niku.wallets.ui.dialogs.TransferDialog
import com.niku.wallets.ui.dialogs.parseColorHex
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * Главный экран кошельков с визуальной индикацией исключенных из общего баланса счетов.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WalletsScreen(
    viewModel: WalletsViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()

    var showAddWalletDialog by remember { mutableStateOf(false) }
    var pendingEditWallet by remember { mutableStateOf<WalletEntity?>(null) }
    var selectedWalletIdForDetails by remember { mutableStateOf<Long?>(null) }
    var showAddTransactionDialog by remember { mutableStateOf(false) }
    var showTransferDialog by remember { mutableStateOf(false) }
    var showCategoriesScreen by remember { mutableStateOf(false) }

    var pendingDeleteTransaction by remember { mutableStateOf<TransactionEntity?>(null) }
    var pendingEditTransaction by remember { mutableStateOf<TransactionEntity?>(null) }

    val categoryMap = remember(uiState.categories) {
        uiState.categories.associateBy { it.id }
    }
    val walletMap = remember(uiState.wallets) {
        uiState.wallets.associateBy { it.id }
    }
    val txWithTagsMap = remember(uiState.transactionsWithTags) {
        uiState.transactionsWithTags.associateBy { it.transaction.id }
    }

    if (showCategoriesScreen) {
        CategoriesScreen(
            viewModel = viewModel,
            onBack = { showCategoriesScreen = false }
        )
        return
    }

    if (selectedWalletIdForDetails != null) {
        val currentWalletId = selectedWalletIdForDetails!!
        val currentWallet = uiState.wallets.find { it.id == currentWalletId }
        if (currentWallet != null) {
            WalletDetailsScreen(
                walletId = currentWalletId,
                viewModel = viewModel,
                onBack = { selectedWalletIdForDetails = null }
            )
            return
        } else {
            selectedWalletIdForDetails = null
        }
    }

    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        uri?.let { fileUri ->
            viewModel.exportBackup(context, fileUri) { _, message ->
                Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
            }
        }
    }

    var pendingImportUri by remember { mutableStateOf<Uri?>(null) }
    var showImportConfirmDialog by remember { mutableStateOf(false) }

    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let { fileUri ->
            pendingImportUri = fileUri
            showImportConfirmDialog = true
        }
    }

    val csvImportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let { fileUri ->
            viewModel.importCsv(context, fileUri) { _, message ->
                Toast.makeText(context, message, Toast.LENGTH_LONG).show()
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Мои Кошельки", fontWeight = FontWeight.Bold) },
                actions = {
                    IconButton(onClick = { showCategoriesScreen = true }) {
                        Icon(imageVector = Icons.Default.List, contentDescription = "Категории")
                    }
                    IconButton(
                        onClick = {
                            csvImportLauncher.launch(arrayOf("*/*", "text/csv"))
                        }
                    ) {
                        Icon(imageVector = Icons.Default.Add, contentDescription = "Импорт CSV")
                    }
                    IconButton(
                        onClick = {
                            exportLauncher.launch("moneybox_backup_${System.currentTimeMillis()}.json")
                        }
                    ) {
                        Icon(imageVector = Icons.Default.Share, contentDescription = "Экспорт бэкапа")
                    }
                    IconButton(
                        onClick = {
                            importLauncher.launch(arrayOf("application/json", "*/*"))
                        }
                    ) {
                        Icon(imageVector = Icons.Default.Add, contentDescription = "Импорт бэкапа")
                    }
                }
            )
        },
        modifier = modifier
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            TotalBalanceCard(
                totalBalance = uiState.totalBalance,
                defaultCurrency = uiState.defaultCurrency,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = { showAddWalletDialog = true },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("+ Кошелек")
                }
                Button(
                    onClick = { showAddTransactionDialog = true },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("+ Операция")
                }
                OutlinedButton(
                    onClick = { showTransferDialog = true },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("⇄ Перевод")
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Счета и кошельки",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
            )

            if (uiState.wallets.isEmpty()) {
                Text(
                    text = "Кошельки не добавлены. Нажмите '+ Кошелек'",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
                )
            } else {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(uiState.wallets, key = { it.id }) { wallet ->
                        WalletCardItem(
                            wallet = wallet,
                            onClick = { selectedWalletIdForDetails = wallet.id }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "История операций (свайп влево — удалить)",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
            )

            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (uiState.transactions.isEmpty()) {
                    item {
                        Text(
                            text = "Операций пока нет",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(vertical = 16.dp)
                        )
                    }
                } else {
                    items(uiState.transactions, key = { it.id }) { transaction ->
                        val category = transaction.categoryId?.let { categoryMap[it] }
                        val walletCurrency = (transaction.fromWalletId ?: transaction.toWalletId)?.let { walletMap[it]?.currency } ?: uiState.defaultCurrency
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
                                        contentDescription = "Удалить",
                                        tint = MaterialTheme.colorScheme.onErrorContainer
                                    )
                                }
                            },
                            content = {
                                TransactionItem(
                                    transaction = transaction,
                                    category = category,
                                    tags = tags,
                                    walletCurrency = walletCurrency,
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

    if (pendingEditWallet != null) {
        val walletToEdit = pendingEditWallet!!
        AddWalletDialog(
            initialWallet = walletToEdit,
            defaultCurrency = uiState.defaultCurrency,
            onDismiss = { pendingEditWallet = null },
            onDelete = {
                viewModel.deleteWallet(walletToEdit.id)
                pendingEditWallet = null
            },
            onConfirm = { name, type, currency, balance, excludeFromBalance ->
                val updated = walletToEdit.copy(
                    name = name,
                    type = type,
                    currency = currency,
                    balance = balance,
                    excludeFromBalance = excludeFromBalance
                )
                viewModel.updateWallet(updated)
                pendingEditWallet = null
            }
        )
    }

    if (pendingDeleteTransaction != null) {
        val transactionToDelete = pendingDeleteTransaction!!
        val symbol = (transactionToDelete.fromWalletId ?: transactionToDelete.toWalletId)?.let { walletMap[it]?.currency }?.let { CurrencyFormatter.getSymbol(it) } ?: "₽"
        AlertDialog(
            onDismissRequest = { pendingDeleteTransaction = null },
            title = { Text("Удаление операции") },
            text = {
                Text("Удалить транзакцию на сумму ${String.format(Locale.getDefault(), "%,.2f %s", transactionToDelete.amount, symbol)}? Баланс кошелька будет пересчитан.")
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

    if (showImportConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showImportConfirmDialog = false },
            title = { Text("Восстановление данных") },
            text = { Text("Внимание! Текущие данные кошельков и операций будут заменены данными из импортируемого бэкапа. Продолжить?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showImportConfirmDialog = false
                        pendingImportUri?.let { fileUri ->
                            viewModel.importBackup(context, fileUri) { _, message ->
                                Toast.makeText(context, message, Toast.LENGTH_LONG).show()
                            }
                        }
                    }
                ) {
                    Text("Импортировать", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showImportConfirmDialog = false }) {
                    Text("Отмена")
                }
            }
        )
    }

    if (showAddWalletDialog) {
        AddWalletDialog(
            defaultCurrency = uiState.defaultCurrency,
            onDismiss = { showAddWalletDialog = false },
            onConfirm = { name, type, currency, initialBalance, excludeFromBalance ->
                viewModel.createWallet(name, type, currency, initialBalance, excludeFromBalance)
                showAddWalletDialog = false
            }
        )
    }

    if (showAddTransactionDialog) {
        AddTransactionDialog(
            wallets = uiState.wallets,
            categories = uiState.categories,
            allTags = uiState.tags,
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

    if (showTransferDialog) {
        TransferDialog(
            wallets = uiState.wallets,
            onDismiss = { showTransferDialog = false },
            onConfirm = { fromId, toId, amount, comment, date ->
                viewModel.transferBetweenWallets(fromId, toId, amount, comment, date)
                showTransferDialog = false
            }
        )
    }
}

@Composable
fun TotalBalanceCard(
    totalBalance: Double,
    defaultCurrency: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = "Общий баланс (в $defaultCurrency)",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = String.format(Locale.getDefault(), "%,.2f %s", totalBalance, CurrencyFormatter.getSymbol(defaultCurrency)),
                style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
    }
}

@Composable
fun WalletCardItem(
    wallet: WalletEntity,
    onClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val cardAlpha = if (wallet.excludeFromBalance) 0.65f else 1.0f

    Card(
        modifier = modifier
            .width(160.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = cardAlpha)
        )
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = wallet.name,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    maxLines = 1,
                    modifier = Modifier.weight(1f)
                )
                if (wallet.excludeFromBalance) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = "Скрыт из общего баланса",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
            Text(
                text = wallet.type.getDisplayName(),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = String.format(Locale.getDefault(), "%,.2f %s", wallet.balance, CurrencyFormatter.getSymbol(wallet.currency)),
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                color = if (wallet.excludeFromBalance) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.primary
            )
        }
    }
}

fun formatRelativeDate(timestamp: Long): String {
    val now = Calendar.getInstance()
    val time = Calendar.getInstance().apply { timeInMillis = timestamp }

    val isSameYear = now.get(Calendar.YEAR) == time.get(Calendar.YEAR)
    val isToday = isSameYear && now.get(Calendar.DAY_OF_YEAR) == time.get(Calendar.DAY_OF_YEAR)

    val yesterday = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -1) }
    val isYesterday = isSameYear && yesterday.get(Calendar.DAY_OF_YEAR) == time.get(Calendar.DAY_OF_YEAR)

    val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
    val formattedTime = timeFormat.format(Date(timestamp))

    return when {
        isToday -> "Сегодня, $formattedTime"
        isYesterday -> "Вчера, $formattedTime"
        isSameYear -> SimpleDateFormat("d MMM, HH:mm", Locale("ru")).format(Date(timestamp))
        else -> SimpleDateFormat("d MMM yyyy, HH:mm", Locale("ru")).format(Date(timestamp))
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TransactionItem(
    transaction: TransactionEntity,
    category: CategoryEntity?,
    tags: List<TagEntity> = emptyList(),
    walletCurrency: String,
    currentWalletId: Long? = null,
    onClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val (typeText, color, sign) = when (transaction.type) {
        TransactionType.INCOME -> Triple("Доход", Color(0xFF2E7D32), "+")
        TransactionType.EXPENSE -> Triple("Расход", Color(0xFFC62828), "-")
        TransactionType.TRANSFER -> {
            if (currentWalletId != null) {
                if (transaction.toWalletId == currentWalletId) {
                    Triple("Входящий перевод", Color(0xFF2E7D32), "+")
                } else {
                    Triple("Исходящий перевод", Color(0xFFC62828), "-")
                }
            } else {
                Triple("Перевод", Color(0xFF1565C0), "⇄ ")
            }
        }
    }

    val displayTitle = category?.name ?: transaction.comment ?: typeText
    val currencySymbol = CurrencyFormatter.getSymbol(walletCurrency)

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            category?.let { cat ->
                Surface(
                    modifier = Modifier.size(16.dp),
                    shape = CircleShape,
                    color = parseColorHex(cat.color)
                ) {}
                Spacer(modifier = Modifier.width(12.dp))
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = displayTitle,
                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold)
                )
                Text(
                    text = formatRelativeDate(transaction.date),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                if (tags.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        tags.forEach { tag ->
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)
                            ) {
                                Text(
                                    text = "#${tag.name}",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontSize = 10.sp,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                )
                            }
                        }
                    }
                }
            }
            Text(
                text = "$sign${String.format(Locale.getDefault(), "%,.2f %s", transaction.amount, currencySymbol)}",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = color
            )
        }
    }
}
