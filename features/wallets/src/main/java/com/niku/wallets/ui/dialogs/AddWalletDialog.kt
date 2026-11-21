package com.niku.wallets.ui.dialogs

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import com.niku.moneybox.core.database.model.WalletEntity
import com.niku.moneybox.core.database.model.WalletType
import com.niku.wallets.ui.getDisplayName

/**
 * Диалоговое окно создания или редактирования кошелька с возможностью исключения из общего баланса и отчетов.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddWalletDialog(
    initialWallet: WalletEntity? = null,
    defaultCurrency: String = "RUB",
    nextSortOrder: Int = 1,
    onDismiss: () -> Unit,
    onDelete: (() -> Unit)? = null,
    onConfirm: (name: String, type: WalletType, currency: String, initialBalance: Double, excludeFromBalance: Boolean, sortOrder: Int) -> Unit
) {
    var name by remember { mutableStateOf(initialWallet?.name ?: "") }
    var selectedType by remember { mutableStateOf(initialWallet?.type ?: WalletType.DEBIT_CARD) }
    var currency by remember { mutableStateOf(initialWallet?.currency ?: defaultCurrency) }
    var balanceText by remember { mutableStateOf(initialWallet?.balance?.toString() ?: "0") }
    var sortOrderText by remember { mutableStateOf(initialWallet?.sortOrder?.toString() ?: nextSortOrder.toString()) }
    var excludeFromBalance by remember { mutableStateOf(initialWallet?.excludeFromBalance ?: false) }
    var expanded by remember { mutableStateOf(false) }

    var showDeleteConfirmDialog by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = if (initialWallet != null) "Редактировать кошелек" else "Создать кошелек")
                if (initialWallet != null && onDelete != null) {
                    IconButton(onClick = { showDeleteConfirmDialog = true }) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Удалить кошелек",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
        },
        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Название (например: Т-Банк, Наличные)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(12.dp))

                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = !expanded }
                ) {
                    OutlinedTextField(
                        value = selectedType.getDisplayName(),
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Тип счета") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor()
                    )

                    ExposedDropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        WalletType.entries.forEach { type ->
                            DropdownMenuItem(
                                text = { Text(type.getDisplayName()) },
                                onClick = {
                                    selectedType = type
                                    expanded = false
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = currency,
                        onValueChange = { currency = it },
                        label = { Text("Валюта (RUB, USD, EUR)") },
                        singleLine = true,
                        modifier = Modifier
                            .weight(1f)
                            .padding(end = 8.dp)
                    )

                    OutlinedTextField(
                        value = balanceText,
                        onValueChange = { balanceText = it },
                        label = { Text("Текущий баланс") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = sortOrderText,
                    onValueChange = { sortOrderText = it },
                    label = { Text("Порядок сортировки") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { excludeFromBalance = !excludeFromBalance }
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = excludeFromBalance,
                        onCheckedChange = { excludeFromBalance = it }
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Не учитывать в общем балансе и отчетах",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (name.isNotBlank()) {
                        val balance = balanceText.toDoubleOrNull() ?: 0.0
                        val sortOrder = sortOrderText.toIntOrNull() ?: nextSortOrder
                        onConfirm(name.trim(), selectedType, currency.trim().ifEmpty { defaultCurrency }, balance, excludeFromBalance, sortOrder)
                    }
                }
            ) {
                Text(if (initialWallet != null) "Сохранить" else "Создать")
            }
        },
        dismissButton = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (initialWallet != null && onDelete != null) {
                    TextButton(onClick = { showDeleteConfirmDialog = true }) {
                        Text("Удалить", color = MaterialTheme.colorScheme.error)
                    }
                }
                TextButton(onClick = onDismiss) {
                    Text("Отмена")
                }
            }
        }
    )

    if (showDeleteConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmDialog = false },
            title = { Text("Удалить кошелек?") },
            text = { Text("Удалить кошелек \"${initialWallet?.name}\" и все связанные с ним транзакции? Это действие нельзя отменить.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteConfirmDialog = false
                        onDelete?.invoke()
                    }
                ) {
                    Text("Удалить", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmDialog = false }) {
                    Text("Отмена")
                }
            }
        )
    }
}
