package com.niku.currencies.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.niku.currencies.model.CurrencyDisplayItem
import java.util.Locale

/**
 * UI-экран курсов валют и онлайнового конвертера.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CurrenciesScreen(
    viewModel: CurrenciesViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    var amountInputText by remember(uiState.inputAmount) {
        mutableStateOf(if (uiState.inputAmount == 0.0) "" else uiState.inputAmount.toString())
    }

    val availableBases = remember { listOf("RUB", "USD", "EUR", "CNY", "KZT") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Курсы Валют и Конвертер", fontWeight = FontWeight.Bold) },
                actions = {
                    IconButton(onClick = { viewModel.loadCurrencyRates() }) {
                        Icon(imageVector = Icons.Default.Refresh, contentDescription = "Обновить")
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
                .padding(horizontal = 16.dp)
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            // Поле ввода суммы
            OutlinedTextField(
                value = amountInputText,
                onValueChange = {
                    amountInputText = it
                    viewModel.updateInputAmount(it)
                },
                label = { Text("Сумма для конвертации") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Переключатель базовой валюты
            Text(
                text = "Базовая валюта:",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(4.dp))

            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(availableBases) { currencyCode ->
                    FilterChip(
                        selected = uiState.baseCurrency == currencyCode,
                        onClick = { viewModel.changeBaseCurrency(currencyCode) },
                        label = { Text(currencyCode) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Сообщение об ошибке соединения
            uiState.errorMessage?.let { error ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = error,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(onClick = { viewModel.loadCurrencyRates() }) {
                            Text("Повторить запрос")
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            // Список котировок
            Text(
                text = "Актуальные котировки:",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
            )

            Spacer(modifier = Modifier.height(8.dp))

            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(bottom = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(uiState.currencyItems, key = { it.code }) { item ->
                    CurrencyRateCardItem(
                        item = item,
                        baseCurrency = uiState.baseCurrency
                    )
                }
            }
        }
    }
}

@Composable
fun CurrencyRateCardItem(
    item: CurrencyDisplayItem,
    baseCurrency: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = item.flagEmoji, fontSize = 28.sp)
            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "${item.code} — ${item.name}",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
                Text(
                    text = String.format(Locale.getDefault(), "1 %s = %,.4f %s", baseCurrency, item.rate, item.code),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Text(
                text = String.format(Locale.getDefault(), "%,.2f %s", item.convertedAmount, item.code),
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}
