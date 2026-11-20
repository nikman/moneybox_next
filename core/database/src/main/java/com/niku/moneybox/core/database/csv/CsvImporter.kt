package com.niku.moneybox.core.database.csv

import androidx.room.withTransaction
import com.niku.moneybox.core.database.AppDatabase
import com.niku.moneybox.core.database.model.CategoryEntity
import com.niku.moneybox.core.database.model.CategoryType
import com.niku.moneybox.core.database.model.TagEntity
import com.niku.moneybox.core.database.model.TransactionEntity
import com.niku.moneybox.core.database.model.TransactionType
import com.niku.moneybox.core.database.model.WalletEntity
import com.niku.moneybox.core.database.model.WalletType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.InputStream
import java.text.SimpleDateFormat
import java.util.Locale
import kotlin.math.abs

/**
 * Итоговый результат умного импорта из CSV.
 */
data class CsvImportResult(
    val transactionsCount: Int,
    val walletsCount: Int,
    val categoriesCount: Int,
    val tagsCount: Int
)

/**
 * Парсер и импортер банковских/финансовых выписок из файлов формата CSV.
 */
class CsvImporter(
    private val database: AppDatabase
) {

    private val dateTimeFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.ROOT)

    /**
     * Парсинг CSV-потока [inputStream] и сохранение транзакций, кошельков, категорий и тегов в БД.
     */
    suspend fun importCsv(inputStream: InputStream): Result<CsvImportResult> = withContext(Dispatchers.IO) {
        runCatching {
            val lines = inputStream.use { stream ->
                stream.bufferedReader(Charsets.UTF_8).readLines()
            }

            if (lines.isEmpty()) {
                throw IllegalArgumentException("Пустой CSV-файл")
            }

            // Пропускаем первую строку с заголовками
            val dataLines = if (lines.first().contains("Дата", ignoreCase = true)) {
                lines.drop(1)
            } else {
                lines
            }

            val rawRows = mutableListOf<RawCsvRow>()

            for ((index, line) in dataLines.withIndex()) {
                if (line.isBlank()) continue
                val tokens = parseCsvLine(line)
                if (tokens.size < 5) continue

                val dateStr = tokens.getOrNull(0) ?: ""
                val timeStr = tokens.getOrNull(1) ?: "00:00:00"
                val walletName = tokens.getOrNull(2) ?: "Импортированный счет"
                val rawAmountStr = tokens.getOrNull(3) ?: "0"
                val currency = tokens.getOrNull(4) ?: "RUB"
                val categoryName = tokens.getOrNull(6) ?: ""
                val tagsStr = tokens.getOrNull(8) ?: ""
                val comment = tokens.getOrNull(9) ?: ""
                val direction = tokens.getOrNull(10)?.lowercase(Locale.ROOT) ?: ""

                val amount = parseAmount(rawAmountStr) ?: continue
                val timestamp = try {
                    dateTimeFormat.parse("$dateStr $timeStr")?.time ?: System.currentTimeMillis()
                } catch (e: Exception) {
                    System.currentTimeMillis()
                }

                rawRows.add(
                    RawCsvRow(
                        lineIndex = index,
                        dateStr = dateStr,
                        timeStr = timeStr,
                        timestamp = timestamp,
                        walletName = walletName,
                        amount = amount,
                        currency = if (currency.isBlank()) "RUB" else currency,
                        categoryName = categoryName,
                        tagsStr = tagsStr,
                        comment = comment,
                        direction = direction
                    )
                )
            }

            if (rawRows.isEmpty()) {
                throw IllegalArgumentException("В CSV-файле не найдено корректных строк с операциями")
            }

            val walletDao = database.walletDao()
            val transactionDao = database.transactionDao()
            val categoryDao = database.categoryDao()

            var newWalletsCreated = 0
            var newCategoriesCreated = 0
            var newTagsCreated = 0
            var importedTransactionsCount = 0

            database.withTransaction {
                val existingWallets = walletDao.getAllWallets().toMutableList()
                val walletMap = existingWallets.associateBy { it.name.lowercase(Locale.ROOT) }.toMutableMap()

                val existingCategories = categoryDao.getAllCategories().toMutableList()
                val categoryMap = existingCategories.associateBy { Pair(it.name.lowercase(Locale.ROOT), it.type) }.toMutableMap()

                val existingTags = transactionDao.getAllTags().toMutableList()
                val tagMap = existingTags.associateBy { it.name.lowercase(Locale.ROOT) }.toMutableMap()

                suspend fun getOrCreateWallet(name: String, currency: String): WalletEntity {
                    val key = name.lowercase(Locale.ROOT)
                    val existing = walletMap[key]
                    if (existing != null) return existing

                    val newWallet = WalletEntity(
                        name = name,
                        type = WalletType.DEBIT_CARD,
                        currency = currency,
                        balance = 0.0
                    )
                    val newId = walletDao.insertWallet(newWallet)
                    val created = newWallet.copy(id = newId)
                    walletMap[key] = created
                    newWalletsCreated++
                    return created
                }

                suspend fun getOrCreateCategory(name: String, type: CategoryType): CategoryEntity? {
                    if (name.isBlank()) return null
                    val key = Pair(name.lowercase(Locale.ROOT), type)
                    val existing = categoryMap[key]
                    if (existing != null) return existing

                    val newCat = CategoryEntity(
                        name = name,
                        type = type,
                        icon = "category",
                        color = if (type == CategoryType.INCOME) "#43A047" else "#1E88E5"
                    )
                    val newId = categoryDao.insertCategory(newCat)
                    val created = newCat.copy(id = newId)
                    categoryMap[key] = created
                    newCategoriesCreated++
                    return created
                }

                suspend fun getOrCreateTag(name: String): TagEntity? {
                    val clean = name.trim().lowercase(Locale.ROOT)
                    if (clean.isBlank()) return null
                    val existing = tagMap[clean]
                    if (existing != null) return existing

                    val newTag = TagEntity(name = name.trim())
                    val newId = transactionDao.insertTag(newTag)
                    val created = newTag.copy(id = newId)
                    tagMap[clean] = created
                    newTagsCreated++
                    return created
                }

                suspend fun parseTagIds(tagsStr: String): List<Long> {
                    if (tagsStr.isBlank()) return emptyList()
                    val tagNames = tagsStr.split(":", ",").map { it.trim() }.filter { it.isNotBlank() }
                    return tagNames.mapNotNull { getOrCreateTag(it)?.id }
                }

                val processedIndices = mutableSetOf<Int>()

                // 1. Поиск и парная сборка переводов ("out" + "in")
                for (i in rawRows.indices) {
                    if (processedIndices.contains(i)) continue
                    val rowOut = rawRows[i]

                    if (rowOut.direction == "out") {
                        val matchingInIndex = rawRows.indices.find { j ->
                            j != i && !processedIndices.contains(j) &&
                                    rawRows[j].direction == "in" &&
                                    abs(rawRows[j].timestamp - rowOut.timestamp) <= 10000 &&
                                    abs(abs(rawRows[j].amount) - abs(rowOut.amount)) < 0.01
                        }

                        if (matchingInIndex != null) {
                            val rowIn = rawRows[matchingInIndex]

                            val fromWallet = getOrCreateWallet(rowOut.walletName, rowOut.currency)
                            val toWallet = getOrCreateWallet(rowIn.walletName, rowIn.currency)
                            val tagIds = parseTagIds("${rowOut.tagsStr}:${rowIn.tagsStr}")

                            val transferTx = TransactionEntity(
                                fromWalletId = fromWallet.id,
                                toWalletId = toWallet.id,
                                amount = abs(rowOut.amount),
                                type = TransactionType.TRANSFER,
                                categoryId = null,
                                date = rowOut.timestamp,
                                comment = rowOut.comment.ifBlank { rowIn.comment.ifBlank { "Перевод между счетами" } }
                            )

                            transactionDao.insertTransactionWithTags(transferTx, tagIds)
                            importedTransactionsCount++

                            processedIndices.add(i)
                            processedIndices.add(matchingInIndex)
                        }
                    }
                }

                // 2. Обработка обычных доходов и расходов
                for (i in rawRows.indices) {
                    if (processedIndices.contains(i)) continue
                    val row = rawRows[i]

                    val wallet = getOrCreateWallet(row.walletName, row.currency)
                    val isIncome = row.amount > 0
                    val type = if (isIncome) TransactionType.INCOME else TransactionType.EXPENSE
                    val categoryType = if (isIncome) CategoryType.INCOME else CategoryType.EXPENSE

                    val category = getOrCreateCategory(row.categoryName, categoryType)
                    val tagIds = parseTagIds(row.tagsStr)

                    val tx = TransactionEntity(
                        fromWalletId = if (type == TransactionType.EXPENSE) wallet.id else null,
                        toWalletId = if (type == TransactionType.INCOME) wallet.id else null,
                        amount = abs(row.amount),
                        type = type,
                        categoryId = category?.id,
                        date = row.timestamp,
                        comment = row.comment.ifBlank { null }
                    )

                    transactionDao.insertTransactionWithTags(tx, tagIds)
                    importedTransactionsCount++
                    processedIndices.add(i)
                }
            }

            CsvImportResult(
                transactionsCount = importedTransactionsCount,
                walletsCount = newWalletsCreated,
                categoriesCount = newCategoriesCreated,
                tagsCount = newTagsCreated
            )
        }
    }

    private fun parseCsvLine(line: String): List<String> {
        val result = mutableListOf<String>()
        val current = StringBuilder()
        var inQuotes = false

        for (ch in line) {
            when {
                ch == '"' -> inQuotes = !inQuotes
                ch == ',' && !inQuotes -> {
                    result.add(current.toString().trim())
                    current.clear()
                }
                else -> current.append(ch)
            }
        }
        result.add(current.toString().trim())
        return result
    }

    private fun parseAmount(amountStr: String): Double? {
        val clean = amountStr
            .replace("\"", "")
            .replace("\u00A0", "")
            .replace("\\s+".toRegex(), "")
            .replace(",", ".")
        return clean.toDoubleOrNull()
    }

    private data class RawCsvRow(
        val lineIndex: Int,
        val dateStr: String,
        val timeStr: String,
        val timestamp: Long,
        val walletName: String,
        val amount: Double,
        val currency: String,
        val categoryName: String,
        val tagsStr: String,
        val comment: String,
        val direction: String
    )
}
