package com.niku.wallets.ui

import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
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
import com.niku.moneybox.core.database.model.CategoryEntity
import com.niku.moneybox.core.database.model.PaymentTemplateEntity
import com.niku.moneybox.core.database.model.TransactionType
import com.niku.moneybox.core.database.model.WalletEntity
import com.niku.wallets.ui.dialogs.AddTemplateDialog
import com.niku.wallets.ui.dialogs.AddTransactionDialog
import com.niku.wallets.ui.dialogs.parseColorHex
import java.util.Locale

/**
 * Экран управления шаблонами платежей и быстрыми траншами.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TemplatesScreen(
    viewModel: WalletsViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()

    var showCreateTemplateDialog by remember { mutableStateOf(false) }
    var pendingEditTemplate by remember { mutableStateOf<PaymentTemplateEntity?>(null) }
    var pendingQuickTransactionTemplate by remember { mutableStateOf<PaymentTemplateEntity?>(null) }

    val walletMap = remember(uiState.wallets) { uiState.wallets.associateBy { it.id } }
    val categoryMap = remember(uiState.categories) { uiState.categories.associateBy { it.id } }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Шаблоны платежей", fontWeight = FontWeight.Bold) },
                actions = {
                    IconButton(onClick = { showCreateTemplateDialog = true }) {
                        Icon(imageVector = Icons.Default.Add, contentDescription = "Создать шаблон")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showCreateTemplateDialog = true },
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(imageVector = Icons.Default.Add, contentDescription = "Новый шаблон")
            }
        },
        modifier = modifier
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            if (uiState.templates.isEmpty()) {
                Text(
                    text = "Шаблоны не созданы. Нажмите '+', чтобы добавить частый платёж (например: 'Кофе', 'Аренда', 'Проезд').",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 16.dp)
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(bottom = 80.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(uiState.templates, key = { it.id }) { template ->
                        val wallet = walletMap[template.walletId]
                        val category = template.categoryId?.let { categoryMap[it] }

                        TemplateCardItem(
                            template = template,
                            walletName = wallet?.name ?: "Кошелек #${template.walletId}",
                            category = category,
                            onClick = {
                                pendingEditTemplate = template
                            },
                            onQuickAddClick = {
                                pendingQuickTransactionTemplate = template
                            }
                        )
                    }
                }
            }
        }
    }

    // Диалог проведения быстрой операции из шаблона (нажатие на "+")
    if (pendingQuickTransactionTemplate != null) {
        val tmpl = pendingQuickTransactionTemplate!!
        AddTransactionDialog(
            wallets = uiState.wallets,
            categories = uiState.categories,
            initialAmount = tmpl.amount,
            initialType = tmpl.type,
            initialWalletId = tmpl.walletId,
            initialCategoryId = tmpl.categoryId,
            initialComment = tmpl.name,
            isEditing = false,
            onDismiss = { pendingQuickTransactionTemplate = null },
            onConfirm = { amount, type, walletId, categoryId, comment, date ->
                viewModel.addTransaction(amount, type, walletId, categoryId, comment, date)
                pendingQuickTransactionTemplate = null
            }
        )
    }

    // Диалог редактирования шаблона (нажатие на карточку)
    if (pendingEditTemplate != null) {
        val tmpl = pendingEditTemplate!!
        AddTemplateDialog(
            wallets = uiState.wallets,
            categories = uiState.categories,
            initialTemplate = tmpl,
            onDismiss = { pendingEditTemplate = null },
            onDelete = {
                viewModel.deleteTemplate(tmpl)
                pendingEditTemplate = null
            },
            onConfirm = { name, type, walletId, categoryId, amount ->
                val updated = tmpl.copy(
                    name = name,
                    type = type,
                    walletId = walletId,
                    categoryId = categoryId,
                    amount = amount
                )
                viewModel.updateTemplate(updated)
                pendingEditTemplate = null
            }
        )
    }

    // Диалог создания шаблона
    if (showCreateTemplateDialog) {
        AddTemplateDialog(
            wallets = uiState.wallets,
            categories = uiState.categories,
            onDismiss = { showCreateTemplateDialog = false },
            onConfirm = { name, type, walletId, categoryId, amount ->
                viewModel.createTemplate(name, type, walletId, categoryId, amount)
                showCreateTemplateDialog = false
            }
        )
    }
}

@Composable
fun TemplateCardItem(
    template: PaymentTemplateEntity,
    walletName: String,
    category: CategoryEntity?,
    onClick: () -> Unit,
    onQuickAddClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val (typeText, color, sign) = when (template.type) {
        TransactionType.INCOME -> Triple("Доход", Color(0xFF2E7D32), "+")
        TransactionType.EXPENSE -> Triple("Расход", Color(0xFFC62828), "-")
        TransactionType.TRANSFER -> Triple("Перевод", Color(0xFF1565C0), "⇄ ")
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Маркер цвета категории
            category?.let { cat ->
                Surface(
                    modifier = Modifier.size(18.dp),
                    shape = CircleShape,
                    color = parseColorHex(cat.color)
                ) {}
                Spacer(modifier = Modifier.width(12.dp))
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = template.name,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
                Text(
                    text = "$walletName • ${category?.name ?: typeText}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "$sign${String.format(Locale.getDefault(), "%,.2f ₽", template.amount)}",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                    color = color
                )
            }

            // Явная кнопка "+" в правой части карточки шаблона
            IconButton(onClick = onQuickAddClick) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(36.dp)
                            .padding(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Совершить платёж по шаблону"
                        )
                    }
                }
            }
        }
    }
}
