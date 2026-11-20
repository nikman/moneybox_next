package com.niku.wallets.ui.dialogs

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.niku.moneybox.core.database.model.CategoryEntity
import com.niku.moneybox.core.database.model.CategoryType
import com.niku.moneybox.core.database.model.PaymentTemplateEntity
import com.niku.moneybox.core.database.model.TransactionType
import com.niku.moneybox.core.database.model.WalletEntity

/**
 * Диалог создания или редактирования шаблона платежа.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTemplateDialog(
    wallets: List<WalletEntity>,
    categories: List<CategoryEntity>,
    initialTemplate: PaymentTemplateEntity? = null,
    onDismiss: () -> Unit,
    onDelete: (() -> Unit)? = null,
    onConfirm: (name: String, type: TransactionType, walletId: Long, categoryId: Long?, amount: Double) -> Unit
) {
    var name by remember { mutableStateOf(initialTemplate?.name ?: "") }
    var selectedType by remember { mutableStateOf(initialTemplate?.type ?: TransactionType.EXPENSE) }
    var selectedWallet by remember {
        mutableStateOf(wallets.find { it.id == initialTemplate?.walletId } ?: wallets.firstOrNull())
    }
    var selectedCategory by remember {
        mutableStateOf(categories.find { it.id == initialTemplate?.categoryId })
    }
    var amountText by remember { mutableStateOf(initialTemplate?.amount?.toString() ?: "") }

    var walletExpanded by remember { mutableStateOf(false) }
    var categoryExpanded by remember { mutableStateOf(false) }

    val filteredCategories = remember(categories, selectedType) {
        val targetType = if (selectedType == TransactionType.INCOME) CategoryType.INCOME else CategoryType.EXPENSE
        categories.filter { it.type == targetType }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = if (initialTemplate != null) "Редактировать шаблон" else "Новый шаблон") },
        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Название шаблона (например: Кофе с собой)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(12.dp))

                Row(modifier = Modifier.fillMaxWidth()) {
                    FilterChip(
                        selected = selectedType == TransactionType.EXPENSE,
                        onClick = {
                            selectedType = TransactionType.EXPENSE
                            selectedCategory = null
                        },
                        label = { Text("Расход") },
                        modifier = Modifier.weight(1f)
                    )
                    FilterChip(
                        selected = selectedType == TransactionType.INCOME,
                        onClick = {
                            selectedType = TransactionType.INCOME
                            selectedCategory = null
                        },
                        label = { Text("Доход") },
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it },
                    label = { Text("Сумма") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Кошелек
                ExposedDropdownMenuBox(
                    expanded = walletExpanded,
                    onExpandedChange = { walletExpanded = !walletExpanded }
                ) {
                    OutlinedTextField(
                        value = selectedWallet?.name ?: "Выберите кошелек",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Кошелек") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = walletExpanded) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor()
                    )

                    ExposedDropdownMenu(
                        expanded = walletExpanded,
                        onDismissRequest = { walletExpanded = false }
                    ) {
                        wallets.forEach { wallet ->
                            DropdownMenuItem(
                                text = { Text("${wallet.name} (${wallet.balance} ${wallet.currency})") },
                                onClick = {
                                    selectedWallet = wallet
                                    walletExpanded = false
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Категория
                ExposedDropdownMenuBox(
                    expanded = categoryExpanded,
                    onExpandedChange = { categoryExpanded = !categoryExpanded }
                ) {
                    OutlinedTextField(
                        value = selectedCategory?.name ?: "Выберите категорию",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Категория") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = categoryExpanded) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor()
                    )

                    ExposedDropdownMenu(
                        expanded = categoryExpanded,
                        onDismissRequest = { categoryExpanded = false }
                    ) {
                        filteredCategories.forEach { category ->
                            DropdownMenuItem(
                                text = {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Surface(
                                            modifier = Modifier.size(16.dp),
                                            shape = CircleShape,
                                            color = parseColorHex(category.color)
                                        ) {}
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text(text = category.name, modifier = Modifier.padding(start = 8.dp))
                                    }
                                },
                                onClick = {
                                    selectedCategory = category
                                    categoryExpanded = false
                                }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (onDelete != null) {
                    TextButton(onClick = onDelete) {
                        Text("Удалить", color = MaterialTheme.colorScheme.error)
                    }
                }
                TextButton(
                    onClick = {
                        val amount = amountText.toDoubleOrNull()
                        val wallet = selectedWallet
                        if (name.isNotBlank() && amount != null && amount > 0 && wallet != null) {
                            onConfirm(name.trim(), selectedType, wallet.id, selectedCategory?.id, amount)
                        }
                    }
                ) {
                    Text(if (initialTemplate != null) "Сохранить" else "Создать")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Отмена")
            }
        }
    )
}
