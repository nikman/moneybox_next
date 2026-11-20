package com.niku.wallets.ui.dialogs

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
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
import com.niku.moneybox.core.database.model.TransactionType
import com.niku.moneybox.core.database.model.WalletEntity
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/**
 * Диалог создания или редактирования финансовой операции (Доход / Расход) с поддержкой выбора даты и времени.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTransactionDialog(
    wallets: List<WalletEntity>,
    categories: List<CategoryEntity>,
    initialAmount: Double? = null,
    initialType: TransactionType = TransactionType.EXPENSE,
    initialWalletId: Long? = null,
    initialCategoryId: Long? = null,
    initialComment: String? = null,
    initialDate: Long = System.currentTimeMillis(),
    isEditing: Boolean = false,
    onDismiss: () -> Unit,
    onConfirm: (amount: Double, type: TransactionType, walletId: Long, categoryId: Long?, comment: String?, date: Long) -> Unit
) {
    var amountText by remember { mutableStateOf(initialAmount?.toString() ?: "") }
    var selectedType by remember { mutableStateOf(initialType) }
    var selectedWallet by remember {
        mutableStateOf(wallets.find { it.id == initialWalletId } ?: wallets.firstOrNull())
    }
    var selectedCategory by remember {
        mutableStateOf(categories.find { it.id == initialCategoryId })
    }
    var comment by remember { mutableStateOf(initialComment ?: "") }
    var selectedDateMillis by remember { mutableStateOf(initialDate) }

    var walletExpanded by remember { mutableStateOf(false) }
    var categoryExpanded by remember { mutableStateOf(false) }

    val filteredCategories = remember(categories, selectedType) {
        val targetCategoryType = if (selectedType == TransactionType.INCOME) CategoryType.INCOME else CategoryType.EXPENSE
        categories.filter { it.type == targetCategoryType }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = if (isEditing) "Редактировать операцию" else "Новая операция") },
        text = {
            Column {
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

                // Поле выбора даты и времени
                DateTimePickerField(
                    selectedTimestamp = selectedDateMillis,
                    onTimestampSelected = { selectedDateMillis = it }
                )

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

                // Селектор кошелька
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

                // Селектор категории
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

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = comment,
                    onValueChange = { comment = it },
                    label = { Text("Комментарий (необязательно)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val amount = amountText.toDoubleOrNull()
                    val wallet = selectedWallet
                    if (amount != null && amount > 0 && wallet != null) {
                        onConfirm(amount, selectedType, wallet.id, selectedCategory?.id, comment.ifBlank { null }, selectedDateMillis)
                    }
                }
            ) {
                Text(if (isEditing) "Сохранить" else "Создать")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Отмена")
            }
        }
    )
}

/**
 * Компонент выбора даты и времени операции.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DateTimePickerField(
    selectedTimestamp: Long,
    onTimestampSelected: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }

    var pendingDateCalendar by remember { mutableStateOf<Calendar?>(null) }
    val dateFormat = remember { SimpleDateFormat("dd MMM yyyy, HH:mm", Locale("ru")) }

    Box(modifier = modifier) {
        OutlinedTextField(
            value = dateFormat.format(Date(selectedTimestamp)),
            onValueChange = {},
            readOnly = true,
            label = { Text("Дата и время") },
            trailingIcon = {
                IconButton(onClick = { showDatePicker = true }) {
                    Icon(imageVector = Icons.Default.DateRange, contentDescription = "Выбрать дату")
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .clickable { showDatePicker = true }
        )
    }

    // 1. Диалог выбора даты (DatePickerDialog)
    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = selectedTimestamp
        )

        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        val millis = datePickerState.selectedDateMillis
                        if (millis != null) {
                            val cal = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
                                timeInMillis = millis
                            }
                            val localCal = Calendar.getInstance().apply {
                                set(Calendar.YEAR, cal.get(Calendar.YEAR))
                                set(Calendar.MONTH, cal.get(Calendar.MONTH))
                                set(Calendar.DAY_OF_MONTH, cal.get(Calendar.DAY_OF_MONTH))
                            }
                            pendingDateCalendar = localCal
                            showDatePicker = false
                            showTimePicker = true
                        }
                    }
                ) {
                    Text("Далее")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("Отмена")
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    // 2. Диалог выбора времени (TimePicker)
    if (showTimePicker) {
        val initialCal = Calendar.getInstance().apply { timeInMillis = selectedTimestamp }
        val timePickerState = rememberTimePickerState(
            initialHour = initialCal.get(Calendar.HOUR_OF_DAY),
            initialMinute = initialCal.get(Calendar.MINUTE),
            is24Hour = true
        )

        AlertDialog(
            onDismissRequest = { showTimePicker = false },
            title = { Text("Выберите время") },
            text = {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxWidth()) {
                    TimePicker(state = timePickerState)
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val cal = pendingDateCalendar ?: Calendar.getInstance().apply { timeInMillis = selectedTimestamp }
                        cal.set(Calendar.HOUR_OF_DAY, timePickerState.hour)
                        cal.set(Calendar.MINUTE, timePickerState.minute)
                        cal.set(Calendar.SECOND, 0)
                        cal.set(Calendar.MILLISECOND, 0)

                        onTimestampSelected(cal.timeInMillis)
                        showTimePicker = false
                    }
                ) {
                    Text("Готово")
                }
            },
            dismissButton = {
                TextButton(onClick = { showTimePicker = false }) {
                    Text("Отмена")
                }
            }
        )
    }
}
