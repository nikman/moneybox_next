package com.niku.analytics.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
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
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.abs
import com.niku.analytics.model.AnalyticsPeriod
import com.niku.analytics.model.CategoryChartItem
import com.niku.analytics.model.MonthlyBalancePoint
import com.niku.analytics.model.TrendPoint
import com.niku.moneybox.core.database.model.TransactionType
import com.niku.preferences.CurrencyFormatter
import java.util.Locale

/**
 * UI-экран Аналитики и Отчетов с отображением данных в базовой валюте приложения.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnalyticsScreen(
    viewModel: AnalyticsViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val currencySymbol = CurrencyFormatter.getSymbol(uiState.defaultCurrency)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Аналитика и Отчеты", fontWeight = FontWeight.Bold) }
            )
        },
        modifier = modifier
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. ПЕРВЫЙ БЛОК: График баланса за последние 12 месяцев
            item {
                MonthlyBalanceChartCard(
                    points = uiState.monthlyBalancePoints,
                    currencySymbol = currencySymbol
                )
            }

            // 2. Фильтр периода
            item {
                PeriodSelectorSection(
                    selectedPeriod = uiState.selectedPeriod,
                    onPeriodSelected = { viewModel.selectPeriod(it) }
                )
            }

            // 3. Сводка доходов, расходов и остатка
            item {
                SummaryReportCard(
                    totalIncome = uiState.totalIncome,
                    totalExpense = uiState.totalExpense,
                    netBalance = uiState.netBalance,
                    currencySymbol = currencySymbol
                )
            }

            // 4. Диаграмма распределения по категориям
            item {
                CategoryAnalyticsCard(
                    selectedType = uiState.selectedType,
                    categoryItems = uiState.categoryItems,
                    currencySymbol = currencySymbol,
                    onTypeSelected = { viewModel.selectType(it) }
                )
            }

            // 5. График динамики (трендов)
            item {
                TrendChartCard(
                    trendPoints = uiState.trendPoints
                )
            }
        }
    }
}

/**
 * Блок 12-месячного графика баланса с интерактивными точками.
 */
@Composable
fun MonthlyBalanceChartCard(
    points: List<MonthlyBalancePoint>,
    currencySymbol: String,
    modifier: Modifier = Modifier
) {
    var selectedPoint by remember { mutableStateOf<MonthlyBalancePoint?>(null) }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "График баланса (в $currencySymbol)",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                ChartLegendItem(color = Color(0xFF4CAF50), title = "Доход")
                ChartLegendItem(color = Color(0xFFF44336), title = "Расход")
                ChartLegendItem(color = Color(0xFF2196F3), title = "Баланс")
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (points.isEmpty()) {
                Text(
                    text = "Нет данных за последние 12 месяцев",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 16.dp)
                )
            } else {
                MonthlyBalanceCanvasChart(
                    points = points,
                    selectedPoint = selectedPoint,
                    onPointSelected = { selectedPoint = it },
                    currencySymbol = currencySymbol,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                )

                val displayPoint = selectedPoint ?: points.lastOrNull()
                displayPoint?.let { pt ->
                    Spacer(modifier = Modifier.height(16.dp))
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = "Месяц: ${pt.monthLabel} ${pt.year} г.",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Доход: +${String.format(Locale.getDefault(), "%,.0f %s", pt.incomeAmount, currencySymbol)}", color = Color(0xFF4CAF50), fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                                Text("Расход: -${String.format(Locale.getDefault(), "%,.0f %s", pt.expenseAmount, currencySymbol)}", color = Color(0xFFF44336), fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                                Text("Баланс: ${String.format(Locale.getDefault(), "%,.0f %s", pt.balanceAmount, currencySymbol)}", color = Color(0xFF2196F3), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ChartLegendItem(color: Color, title: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Surface(
            modifier = Modifier.size(10.dp),
            shape = CircleShape,
            color = color
        ) {}
        Spacer(modifier = Modifier.width(6.dp))
        Text(text = title, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
fun MonthlyBalanceCanvasChart(
    points: List<MonthlyBalancePoint>,
    selectedPoint: MonthlyBalancePoint?,
    onPointSelected: (MonthlyBalancePoint) -> Unit,
    currencySymbol: String = "₽",
    modifier: Modifier = Modifier
) {
    val incomeColor = Color(0xFF4CAF50)
    val expenseColor = Color(0xFFF44336)
    val balanceColor = Color(0xFF2196F3)
    val onSurfaceColor = MaterialTheme.colorScheme.onSurface
    val gridColor = onSurfaceColor.copy(alpha = 0.1f)
    val textMeasurer = rememberTextMeasurer()
    val textColor = MaterialTheme.colorScheme.onSurfaceVariant

    val allValues = points.flatMap { listOf(it.incomeAmount, it.expenseAmount, it.balanceAmount) }
    val minVal = allValues.minOrNull() ?: 0.0
    val maxVal = allValues.maxOrNull() ?: 0.0

    // Расчет диапазона оси Y с обязательным включением нуля (0.0)
    val effectiveMin = minOf(minVal, 0.0)
    val effectiveMax = maxOf(maxVal, 0.0)
    val rawRange = effectiveMax - effectiveMin
    val range = if (rawRange == 0.0) 1000.0 else rawRange
    val adjustedMin = if (rawRange == 0.0) 0.0 else effectiveMin
    val adjustedMax = if (rawRange == 0.0) 1000.0 else effectiveMax

    Box(modifier = modifier) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(points) {
                    detectTapGestures { offset ->
                        val leftPadding = 56.dp.toPx()
                        val plotWidth = size.width - leftPadding
                        val tapX = offset.x - leftPadding
                        val xStep = plotWidth / (points.size - 1).coerceAtLeast(1)
                        val index = ((tapX + xStep / 2) / xStep).toInt().coerceIn(0, points.size - 1)
                        onPointSelected(points[index])
                    }
                }
        ) {
            val width = size.width
            val bottomLabelHeight = 24.dp.toPx()
            val chartHeight = size.height - bottomLabelHeight
            val leftPadding = 56.dp.toPx()
            val plotWidth = width - leftPadding
            val xStep = plotWidth / (points.size - 1).coerceAtLeast(1)

            fun getY(value: Double): Float {
                val normalized = ((value - adjustedMin) / range).toFloat()
                return chartHeight - (normalized * chartHeight)
            }

            fun formatCompact(v: Double, symbol: String): String {
                return when {
                    abs(v) >= 1_000_000 -> String.format(Locale.getDefault(), "%.1fM %s", v / 1_000_000, symbol)
                    abs(v) >= 1_000 -> String.format(Locale.getDefault(), "%.0fk %s", v / 1_000, symbol)
                    else -> String.format(Locale.getDefault(), "%.0f %s", v, symbol)
                }
            }

            val yZero = getY(0.0)

            // Ось Y слева с 4 опорными суммами
            val gridLevels = 3
            for (i in 0..gridLevels) {
                val fraction = i.toFloat() / gridLevels
                val valLevel = adjustedMin + fraction * range
                val y = getY(valLevel)

                // Тонкая линия сетки
                drawLine(
                    color = gridColor,
                    start = Offset(leftPadding, y),
                    end = Offset(width, y),
                    strokeWidth = 1.dp.toPx()
                )

                // Если уровень не накладывается на нулевую отметку, рисуем подпись
                if (abs(y - yZero) > 22f) {
                    val labelText = formatCompact(valLevel, currencySymbol)
                    val textLayout = textMeasurer.measure(
                        text = labelText,
                        style = TextStyle(color = textColor, fontSize = 9.sp)
                    )
                    drawText(
                        textLayoutResult = textLayout,
                        topLeft = Offset(4.dp.toPx(), y - textLayout.size.height / 2f)
                    )
                }
            }

            // Явный нулевой уровень (0)
            if (yZero in 0f..chartHeight) {
                drawLine(
                    color = onSurfaceColor.copy(alpha = 0.35f),
                    start = Offset(leftPadding, yZero),
                    end = Offset(width, yZero),
                    strokeWidth = 1.5.dp.toPx()
                )
                val zeroText = "0 $currencySymbol"
                val zeroLayout = textMeasurer.measure(
                    text = zeroText,
                    style = TextStyle(color = onSurfaceColor, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                )
                drawText(
                    textLayoutResult = zeroLayout,
                    topLeft = Offset(4.dp.toPx(), yZero - zeroLayout.size.height / 2f)
                )
            }

            val pathIncome = Path()
            val pathExpense = Path()
            val pathBalance = Path()

            points.forEachIndexed { i, pt ->
                val x = leftPadding + i * xStep
                val yInc = getY(pt.incomeAmount)
                val yExp = getY(pt.expenseAmount)
                val yBal = getY(pt.balanceAmount)

                if (i == 0) {
                    pathIncome.moveTo(x, yInc)
                    pathExpense.moveTo(x, yExp)
                    pathBalance.moveTo(x, yBal)
                } else {
                    pathIncome.lineTo(x, yInc)
                    pathExpense.lineTo(x, yExp)
                    pathBalance.lineTo(x, yBal)
                }
            }

            drawPath(path = pathIncome, color = incomeColor, style = Stroke(width = 2.5.dp.toPx()))
            drawPath(path = pathExpense, color = expenseColor, style = Stroke(width = 2.5.dp.toPx()))
            drawPath(path = pathBalance, color = balanceColor, style = Stroke(width = 3.5.dp.toPx()))

            points.forEachIndexed { i, pt ->
                val x = leftPadding + i * xStep
                val isSelected = selectedPoint == pt

                drawCircle(color = incomeColor, radius = if (isSelected) 5.dp.toPx() else 3.5.dp.toPx(), center = Offset(x, getY(pt.incomeAmount)))
                drawCircle(color = expenseColor, radius = if (isSelected) 5.dp.toPx() else 3.5.dp.toPx(), center = Offset(x, getY(pt.expenseAmount)))
                drawCircle(color = balanceColor, radius = if (isSelected) 6.5.dp.toPx() else 4.dp.toPx(), center = Offset(x, getY(pt.balanceAmount)))

                if (isSelected) {
                    drawLine(
                        color = balanceColor.copy(alpha = 0.4f),
                        start = Offset(x, 0f),
                        end = Offset(x, chartHeight),
                        strokeWidth = 1.5.dp.toPx()
                    )
                }
            }
        }

        // Подписи месяцев снизу с учетом отступа оси Y
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 56.dp)
                .align(Alignment.BottomCenter),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            points.forEach { pt ->
                Text(
                    text = pt.monthLabel,
                    style = MaterialTheme.typography.labelSmall,
                    fontSize = 9.sp,
                    color = if (selectedPoint == pt) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun PeriodSelectorSection(
    selectedPeriod: AnalyticsPeriod,
    onPeriodSelected: (AnalyticsPeriod) -> Unit
) {
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(AnalyticsPeriod.entries.toTypedArray()) { period ->
            FilterChip(
                selected = selectedPeriod == period,
                onClick = { onPeriodSelected(period) },
                label = { Text(period.title) }
            )
        }
    }
}

@Composable
fun SummaryReportCard(
    totalIncome: Double,
    totalExpense: Double,
    netBalance: Double,
    currencySymbol: String
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Сводка за период",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
            )
            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("Доходы", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        text = String.format(Locale.getDefault(), "%,.2f %s", totalIncome, currencySymbol),
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = Color(0xFF2E7D32)
                    )
                }

                Column {
                    Text("Расходы", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        text = String.format(Locale.getDefault(), "%,.2f %s", totalExpense, currencySymbol),
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = Color(0xFFC62828)
                    )
                }

                Column {
                    Text("Чистый остаток", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        text = String.format(Locale.getDefault(), "%,.2f %s", netBalance, currencySymbol),
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = if (netBalance >= 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}

@Composable
fun CategoryAnalyticsCard(
    selectedType: TransactionType,
    categoryItems: List<CategoryChartItem>,
    currencySymbol: String,
    onTypeSelected: (TransactionType) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Категории",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )

                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    FilterChip(
                        selected = selectedType == TransactionType.EXPENSE,
                        onClick = { onTypeSelected(TransactionType.EXPENSE) },
                        label = { Text("Расходы") }
                    )
                    FilterChip(
                        selected = selectedType == TransactionType.INCOME,
                        onClick = { onTypeSelected(TransactionType.INCOME) },
                        label = { Text("Доходы") }
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (categoryItems.isEmpty()) {
                Text(
                    text = "Нет данных по категориям за этот период",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 16.dp)
                )
            } else {
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    DonutChartCanvas(
                        items = categoryItems,
                        currencySymbol = currencySymbol,
                        modifier = Modifier.size(200.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    categoryItems.forEach { item ->
                        CategoryLegendRow(item = item, currencySymbol = currencySymbol)
                    }
                }
            }
        }
    }
}

@Composable
fun DonutChartCanvas(
    items: List<CategoryChartItem>,
    currencySymbol: String,
    modifier: Modifier = Modifier
) {
    val totalAmount = items.sumOf { it.amount }

    Box(contentAlignment = Alignment.Center, modifier = modifier) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val strokeWidth = 36.dp.toPx()
            var startAngle = -90f

            items.forEach { item ->
                val sweepAngle = (item.percentage / 100f) * 360f
                if (sweepAngle > 0f) {
                    drawArc(
                        color = item.color,
                        startAngle = startAngle,
                        sweepAngle = sweepAngle,
                        useCenter = false,
                        style = Stroke(width = strokeWidth)
                    )
                    startAngle += sweepAngle
                }
            }
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "Всего",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = String.format(Locale.getDefault(), "%,.0f %s", totalAmount, currencySymbol),
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
            )
        }
    }
}

@Composable
fun CategoryLegendRow(
    item: CategoryChartItem,
    currencySymbol: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            modifier = Modifier.size(12.dp),
            shape = CircleShape,
            color = item.color
        ) {}

        Spacer(modifier = Modifier.width(8.dp))

        Text(
            text = item.categoryName,
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
            modifier = Modifier.weight(1f)
        )

        Text(
            text = String.format(Locale.getDefault(), "%,.2f %s", item.amount, currencySymbol),
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
        )

        Spacer(modifier = Modifier.width(8.dp))

        Surface(
            shape = RoundedCornerShape(6.dp),
            color = MaterialTheme.colorScheme.secondaryContainer
        ) {
            Text(
                text = String.format(Locale.getDefault(), "%.1f%%", item.percentage),
                style = MaterialTheme.typography.labelSmall,
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
            )
        }
    }
}

@Composable
fun TrendChartCard(
    trendPoints: List<TrendPoint>
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Динамика и Тренды",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
            )
            Spacer(modifier = Modifier.height(16.dp))

            if (trendPoints.isEmpty()) {
                Text(
                    text = "Недостаточно данных для построения графика",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 16.dp)
                )
            } else {
                TrendBarChartCanvas(
                    points = trendPoints,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                )
            }
        }
    }
}

@Composable
fun TrendBarChartCanvas(
    points: List<TrendPoint>,
    modifier: Modifier = Modifier
) {
    val maxVal = points.flatMap { listOf(it.incomeAmount, it.expenseAmount) }.maxOrNull() ?: 1.0
    val incomeColor = Color(0xFF43A047)
    val expenseColor = Color(0xFFE53935)
    val gridColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)

    Canvas(modifier = modifier) {
        val width = size.width
        val height = size.height - 30.dp.toPx()
        val barGroupWidth = width / points.size
        val singleBarWidth = (barGroupWidth * 0.35f)

        val gridLines = 3
        for (i in 0..gridLines) {
            val y = height * (i.toFloat() / gridLines)
            drawLine(
                color = gridColor,
                start = Offset(0f, y),
                end = Offset(width, y),
                strokeWidth = 1.dp.toPx()
            )
        }

        points.forEachIndexed { index, point ->
            val groupX = index * barGroupWidth + (barGroupWidth * 0.15f)

            val incHeight = if (maxVal > 0) ((point.incomeAmount / maxVal) * height).toFloat() else 0f
            val expHeight = if (maxVal > 0) ((point.expenseAmount / maxVal) * height).toFloat() else 0f

            if (incHeight > 0) {
                drawRoundRect(
                    color = incomeColor,
                    topLeft = Offset(groupX, height - incHeight),
                    size = Size(singleBarWidth, incHeight),
                    cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx())
                )
            }

            if (expHeight > 0) {
                drawRoundRect(
                    color = expenseColor,
                    topLeft = Offset(groupX + singleBarWidth + 2.dp.toPx(), height - expHeight),
                    size = Size(singleBarWidth, expHeight),
                    cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx())
                )
            }
        }
    }
}
