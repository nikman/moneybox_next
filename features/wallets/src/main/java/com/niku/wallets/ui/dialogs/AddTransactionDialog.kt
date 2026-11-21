package com.niku.wallets.ui.dialogs

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Search
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
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.niku.moneybox.core.database.model.CategoryEntity
import com.niku.moneybox.core.database.model.CategoryType
import com.niku.moneybox.core.database.model.TagEntity
import com.niku.moneybox.core.database.model.TransactionEntity
import com.niku.moneybox.core.database.model.TransactionType
import com.niku.moneybox.core.database.model.WalletEntity
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/**
 * Диалог создания или редактирования финансовой операции (Расход, Доход или Перевод).
 * Поддерживает:
 * - Выбор типа операции (Расход / Доход / Перевод).
 * - При переводе отображает два поля выбора кошельков: «Откуда» (fromWalletId) и «Куда» (toWalletId).
 * - Выбор даты и времени.
 * - Множественный выбор тегов и создание новых тегов на лету.
 * - Предзаполнение всех полей при редактировании существующей транзакции.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AddTransactionDialog(
    wallets: List<WalletEntity>,
    categories: List<CategoryEntity>,
    allTags: List<TagEntity> = emptyList(),
    initialTransaction: TransactionEntity? = null,
    initialAmount: Double? = null,
    initialType: TransactionType = TransactionType.EXPENSE,
    initialWalletId: Long? = null,
    initialFromWalletId: Long? = null,
    initialToWalletId: Long? = null,
    initialCategoryId: Long? = null,
    initialComment: String? = null,
    initialDate: Long = System.currentTimeMillis(),
    initialSelectedTagIds: List<Long> = emptyList(),
    isEditing: Boolean = false,
    onDismiss: () -> Unit,
    onCreateNewTag: ((name: String, onCreated: (Long) -> Unit) -> Unit)? = null,
    onConfirm: (
        amount: Double,
        type: TransactionType,
        fromWalletId: Long?,
        toWalletId: Long?,
        categoryId: Long?,
        comment: String?,
        date: Long,
        selectedTagIds: List<Long>
    ) -> Unit
) {
    // Определяем режим и начальные значения с приоритетом initialTransaction
    val resolvedIsEditing = isEditing || initialTransaction != null
    val resolvedType = initialTransaction?.type ?: initialType
    val resolvedAmount = initialTransaction?.amount?.toString() ?: initialAmount?.toString() ?: ""
    val resolvedComment = initialTransaction?.comment ?: initialComment ?: ""
    val resolvedDate = initialTransaction?.date ?: initialDate
    val resolvedFromWalletId = initialTransaction?.fromWalletId
        ?: initialFromWalletId
        ?: (if (resolvedType != TransactionType.INCOME) initialWalletId else null)
    val resolvedToWalletId = initialTransaction?.toWalletId
        ?: initialToWalletId
        ?: (if (resolvedType == TransactionType.INCOME) initialWalletId else null)
    val resolvedCategoryId = initialTransaction?.categoryId ?: initialCategoryId

    var amountText by remember { mutableStateOf(resolvedAmount) }
    var selectedType by remember { mutableStateOf(resolvedType) }

    // Кошелек-источник (для Расхода и Перевода)
    var selectedFromWallet by remember {
        mutableStateOf(
            wallets.find { it.id == resolvedFromWalletId } ?: wallets.firstOrNull()
        )
    }

    // Кошелек-приемник (для Дохода и Перевода)
    var selectedToWallet by remember {
        mutableStateOf(
            wallets.find { it.id == resolvedToWalletId }
                ?: wallets.firstOrNull { it.id != selectedFromWallet?.id }
                ?: wallets.getOrNull(1)
                ?: wallets.firstOrNull()
        )
    }

    var selectedCategory by remember {
        mutableStateOf(categories.find { it.id == resolvedCategoryId })
    }
    var comment by remember { mutableStateOf(resolvedComment) }
    var selectedDateMillis by remember { mutableStateOf(resolvedDate) }
    var selectedTagIds by remember { mutableStateOf(initialSelectedTagIds.toSet()) }

    // Фиксируем исходный набор тегов, прикрепленных к операции при открытии диалога, чтобы чипсы не прыгали при кликах
    val initialAssignedTagIds = remember { initialSelectedTagIds.toSet() }

    // Умная сортировка тегов:
    // 1) При создании новой операции: теги идут строго по популярности (usageCount DESC, name ASC)
    // 2) При редактировании: сначала теги, уже присвоенные этой операции, а затем остальные по популярности
    val orderedTags = remember(allTags) {
        if (resolvedIsEditing && initialAssignedTagIds.isNotEmpty()) {
            val (assigned, unassigned) = allTags.partition { it.id in initialAssignedTagIds }
            val sortedAssigned = assigned.sortedWith(compareByDescending<TagEntity> { it.usageCount }.thenBy { it.name })
            val sortedUnassigned = unassigned.sortedWith(compareByDescending<TagEntity> { it.usageCount }.thenBy { it.name })
            sortedAssigned + sortedUnassigned
        } else {
            allTags.sortedWith(compareByDescending<TagEntity> { it.usageCount }.thenBy { it.name })
        }
    }

    var tagSearchQuery by remember { mutableStateOf("") }

    // Фильтрация тегов по компактному поисковому запросу
    val displayedTags = remember(orderedTags, tagSearchQuery) {
        if (tagSearchQuery.isBlank()) {
            orderedTags
        } else {
            val query = tagSearchQuery.trim().removePrefix("#").lowercase(Locale.getDefault())
            orderedTags.filter { it.name.lowercase(Locale.getDefault()).contains(query) }
        }
    }

    var fromWalletExpanded by remember { mutableStateOf(false) }
    var toWalletExpanded by remember { mutableStateOf(false) }
    var singleWalletExpanded by remember { mutableStateOf(false) }
    var categoryExpanded by remember { mutableStateOf(false) }

    // Фильтрация категорий по типу транзакции (для перевода категория не требуется)
    val filteredCategories = remember(categories, selectedType) {
        val targetCategoryType = if (selectedType == TransactionType.INCOME) CategoryType.INCOME else CategoryType.EXPENSE
        categories.filter { it.type == targetCategoryType }
    }

    // Проверка валидности ввода
    val parsedAmount = amountText.toDoubleOrNull()
    val isAmountValid = parsedAmount != null && parsedAmount > 0
    val isTransfer = selectedType == TransactionType.TRANSFER
    val isTransferValid = isTransfer && selectedFromWallet != null && selectedToWallet != null && selectedFromWallet?.id != selectedToWallet?.id
    val isExpenseValid = selectedType == TransactionType.EXPENSE && selectedFromWallet != null
    val isIncomeValid = selectedType == TransactionType.INCOME && (selectedToWallet != null || selectedFromWallet != null)
    val isFormValid = isAmountValid && (isTransferValid || isExpenseValid || isIncomeValid)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (resolvedIsEditing) {
                    when (selectedType) {
                        TransactionType.TRANSFER -> "Редактировать перевод"
                        TransactionType.INCOME -> "Редактировать доход"
                        TransactionType.EXPENSE -> "Редактировать расход"
                    }
                } else {
                    when (selectedType) {
                        TransactionType.TRANSFER -> "Новый перевод"
                        TransactionType.INCOME -> "Новый доход"
                        TransactionType.EXPENSE -> "Новый расход"
                    }
                }
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                // Переключатель типов операций (Расход / Доход / Перевод)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
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
                    FilterChip(
                        selected = selectedType == TransactionType.TRANSFER,
                        onClick = {
                            selectedType = TransactionType.TRANSFER
                            selectedCategory = null
                            // Если кошельки совпадают, автоматически выбираем второй доступный
                            if (selectedToWallet?.id == selectedFromWallet?.id) {
                                selectedToWallet = wallets.firstOrNull { it.id != selectedFromWallet?.id }
                                    ?: selectedToWallet
                            }
                        },
                        label = { Text("Перевод") },
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Дата и время операции
                DateTimePickerField(
                    selectedTimestamp = selectedDateMillis,
                    onTimestampSelected = { selectedDateMillis = it }
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Поле ввода суммы
                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it },
                    label = { Text("Сумма") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                // ВЫБОР СЧЕТОВ:
                // При ПЕРЕВОДЕ отображаются два поля («Откуда» и «Куда»)
                if (isTransfer) {
                    // Кошелек списания («Откуда»)
                    ExposedDropdownMenuBox(
                        expanded = fromWalletExpanded,
                        onExpandedChange = { fromWalletExpanded = !fromWalletExpanded }
                    ) {
                        OutlinedTextField(
                            value = selectedFromWallet?.name ?: "Выберите счет списания",
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Со счета (Откуда)") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = fromWalletExpanded) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor()
                        )

                        ExposedDropdownMenu(
                            expanded = fromWalletExpanded,
                            onDismissRequest = { fromWalletExpanded = false }
                        ) {
                            wallets.forEach { wallet ->
                                DropdownMenuItem(
                                    text = { Text("${wallet.name} (${wallet.balance} ${wallet.currency})") },
                                    onClick = {
                                        selectedFromWallet = wallet
                                        fromWalletExpanded = false
                                        if (selectedToWallet?.id == wallet.id) {
                                            selectedToWallet = wallets.firstOrNull { it.id != wallet.id }
                                        }
                                    }
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Кошелек зачисления («Куда»)
                    ExposedDropdownMenuBox(
                        expanded = toWalletExpanded,
                        onExpandedChange = { toWalletExpanded = !toWalletExpanded }
                    ) {
                        OutlinedTextField(
                            value = selectedToWallet?.name ?: "Выберите счет зачисления",
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("На счет (Куда)") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = toWalletExpanded) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor()
                        )

                        ExposedDropdownMenu(
                            expanded = toWalletExpanded,
                            onDismissRequest = { toWalletExpanded = false }
                        ) {
                            wallets.forEach { wallet ->
                                DropdownMenuItem(
                                    text = { Text("${wallet.name} (${wallet.balance} ${wallet.currency})") },
                                    onClick = {
                                        selectedToWallet = wallet
                                        toWalletExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    if (selectedFromWallet != null && selectedToWallet != null && selectedFromWallet?.id == selectedToWallet?.id) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Счет списания и счет зачисления должны отличаться",
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                } else {
                    // При обычном ДОХОДЕ или РАСХОДЕ отображается один счет
                    val isIncome = selectedType == TransactionType.INCOME
                    val currentWallet = if (isIncome) (selectedToWallet ?: selectedFromWallet) else selectedFromWallet

                    ExposedDropdownMenuBox(
                        expanded = singleWalletExpanded,
                        onExpandedChange = { singleWalletExpanded = !singleWalletExpanded }
                    ) {
                        OutlinedTextField(
                            value = currentWallet?.name ?: "Выберите кошелек",
                            onValueChange = {},
                            readOnly = true,
                            label = { Text(if (isIncome) "На счет" else "Со счета") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = singleWalletExpanded) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor()
                        )

                        ExposedDropdownMenu(
                            expanded = singleWalletExpanded,
                            onDismissRequest = { singleWalletExpanded = false }
                        ) {
                            wallets.forEach { wallet ->
                                DropdownMenuItem(
                                    text = { Text("${wallet.name} (${wallet.balance} ${wallet.currency})") },
                                    onClick = {
                                        if (isIncome) {
                                            selectedToWallet = wallet
                                        } else {
                                            selectedFromWallet = wallet
                                        }
                                        singleWalletExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Выбор категории (только для доходов и расходов, не для перевода)
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

                Spacer(modifier = Modifier.height(8.dp))

                // Поле комментария
                OutlinedTextField(
                    value = comment,
                    onValueChange = { comment = it },
                    label = { Text("Комментарий (необязательно)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(12.dp))

                // РАЗДЕЛ ТЕГОВ
                Text(
                    text = "Теги",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                )

                Spacer(modifier = Modifier.height(6.dp))

                // Компактное поле поиска и создания тега
                OutlinedTextField(
                    value = tagSearchQuery,
                    onValueChange = { tagSearchQuery = it },
                    placeholder = { Text("Найти или создать тег...", style = MaterialTheme.typography.bodySmall) },
                    singleLine = true,
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Поиск тегов",
                            modifier = Modifier.size(18.dp)
                        )
                    },
                    trailingIcon = {
                        if (tagSearchQuery.isNotBlank()) {
                            IconButton(
                                onClick = {
                                    val cleanQuery = tagSearchQuery.trim().removePrefix("#")
                                    if (cleanQuery.isNotBlank()) {
                                        val existingTag = allTags.find { it.name.equals(cleanQuery, ignoreCase = true) }
                                        if (existingTag != null) {
                                            selectedTagIds = selectedTagIds + existingTag.id
                                            tagSearchQuery = ""
                                        } else {
                                            onCreateNewTag?.invoke(cleanQuery) { createdId ->
                                                selectedTagIds = selectedTagIds + createdId
                                            }
                                            tagSearchQuery = ""
                                        }
                                    }
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = "Добавить тег",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                if (displayedTags.isNotEmpty()) {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        displayedTags.forEach { tag ->
                            val isSelected = selectedTagIds.contains(tag.id)
                            FilterChip(
                                selected = isSelected,
                                onClick = {
                                    selectedTagIds = if (isSelected) {
                                        selectedTagIds - tag.id
                                    } else {
                                        selectedTagIds + tag.id
                                    }
                                },
                                label = { Text("#${tag.name}") }
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                } else if (tagSearchQuery.isNotBlank()) {
                    Text(
                        text = "Тег «#${tagSearchQuery.trim().removePrefix("#")}» не найден. Нажмите «+», чтобы создать его.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = isFormValid,
                onClick = {
                    val amount = amountText.toDoubleOrNull() ?: return@TextButton
                    when (selectedType) {
                        TransactionType.EXPENSE -> {
                            val wallet = selectedFromWallet ?: return@TextButton
                            onConfirm(
                                amount,
                                TransactionType.EXPENSE,
                                wallet.id,
                                null,
                                selectedCategory?.id,
                                comment.ifBlank { null },
                                selectedDateMillis,
                                selectedTagIds.toList()
                            )
                        }
                        TransactionType.INCOME -> {
                            val wallet = selectedToWallet ?: selectedFromWallet ?: return@TextButton
                            onConfirm(
                                amount,
                                TransactionType.INCOME,
                                null,
                                wallet.id,
                                selectedCategory?.id,
                                comment.ifBlank { null },
                                selectedDateMillis,
                                selectedTagIds.toList()
                            )
                        }
                        TransactionType.TRANSFER -> {
                            val from = selectedFromWallet ?: return@TextButton
                            val to = selectedToWallet ?: return@TextButton
                            if (from.id == to.id) return@TextButton
                            onConfirm(
                                amount,
                                TransactionType.TRANSFER,
                                from.id,
                                to.id,
                                null,
                                comment.ifBlank { null },
                                selectedDateMillis,
                                selectedTagIds.toList()
                            )
                        }
                    }
                }
            ) {
                Text(if (resolvedIsEditing) "Сохранить" else "Создать")
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
