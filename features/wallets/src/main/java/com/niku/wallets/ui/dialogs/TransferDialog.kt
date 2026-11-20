package com.niku.wallets.ui.dialogs

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.niku.moneybox.core.database.model.WalletEntity

/**
 * Диалоговое окно быстрого перевода средств между счетами с выбором даты.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransferDialog(
    wallets: List<WalletEntity>,
    onDismiss: () -> Unit,
    onConfirm: (fromWalletId: Long, toWalletId: Long, amount: Double, comment: String?, date: Long) -> Unit
) {
    var fromWallet by remember { mutableStateOf(wallets.firstOrNull()) }
    var toWallet by remember { mutableStateOf(wallets.getOrNull(1) ?: wallets.firstOrNull()) }
    var amountText by remember { mutableStateOf("") }
    var comment by remember { mutableStateOf("") }
    var selectedDateMillis by remember { mutableStateOf(System.currentTimeMillis()) }

    var fromExpanded by remember { mutableStateOf(false) }
    var toExpanded by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = "Перевод между счетами") },
        text = {
            Column {
                DateTimePickerField(
                    selectedTimestamp = selectedDateMillis,
                    onTimestampSelected = { selectedDateMillis = it }
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Кошелек списания
                ExposedDropdownMenuBox(
                    expanded = fromExpanded,
                    onExpandedChange = { fromExpanded = !fromExpanded }
                ) {
                    OutlinedTextField(
                        value = fromWallet?.name ?: "Откуда",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Со счета") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = fromExpanded) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor()
                    )

                    ExposedDropdownMenu(
                        expanded = fromExpanded,
                        onDismissRequest = { fromExpanded = false }
                    ) {
                        wallets.forEach { wallet ->
                            DropdownMenuItem(
                                text = { Text("${wallet.name} (${wallet.balance} ${wallet.currency})") },
                                onClick = {
                                    fromWallet = wallet
                                    fromExpanded = false
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Кошелек зачисления
                ExposedDropdownMenuBox(
                    expanded = toExpanded,
                    onExpandedChange = { toExpanded = !toExpanded }
                ) {
                    OutlinedTextField(
                        value = toWallet?.name ?: "Куда",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("На счет") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = toExpanded) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor()
                    )

                    ExposedDropdownMenu(
                        expanded = toExpanded,
                        onDismissRequest = { toExpanded = false }
                    ) {
                        wallets.forEach { wallet ->
                            DropdownMenuItem(
                                text = { Text("${wallet.name} (${wallet.balance} ${wallet.currency})") },
                                onClick = {
                                    toWallet = wallet
                                    toExpanded = false
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it },
                    label = { Text("Сумма перевода") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = comment,
                    onValueChange = { comment = it },
                    label = { Text("Примечание (необязательно)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val amount = amountText.toDoubleOrNull()
                    val from = fromWallet
                    val to = toWallet
                    if (amount != null && amount > 0 && from != null && to != null && from.id != to.id) {
                        onConfirm(from.id, to.id, amount, comment.ifBlank { null }, selectedDateMillis)
                    }
                }
            ) {
                Text("Перевести")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Отмена")
            }
        }
    )
}
