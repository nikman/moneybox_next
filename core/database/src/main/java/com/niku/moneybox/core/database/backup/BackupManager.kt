package com.niku.moneybox.core.database.backup

import androidx.room.withTransaction
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.niku.moneybox.core.database.AppDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.InputStream
import java.io.OutputStream

/**
 * Менеджер экспорта и импорта базы данных (Backup & Restore).
 */
class BackupManager(
    private val database: AppDatabase
) {
    private val gson: Gson = GsonBuilder()
        .setPrettyPrinting()
        .create()

    /**
     * Экспорт всей базы данных в [OutputStream] в формате JSON.
     */
    suspend fun exportBackup(outputStream: OutputStream): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val walletDao = database.walletDao()
            val transactionDao = database.transactionDao()
            val categoryDao = database.categoryDao()
            val templateDao = database.paymentTemplateDao()

            val backupData = BackupDataDto(
                wallets = walletDao.getAllWallets(),
                transactions = transactionDao.getAllTransactions(),
                categories = categoryDao.getAllCategories(),
                templates = templateDao.getAllTemplates()
            )

            val jsonString = gson.toJson(backupData)
            outputStream.use { stream ->
                stream.write(jsonString.toByteArray(Charsets.UTF_8))
                stream.flush()
            }
        }
    }

    /**
     * Импорт резервной копии из [InputStream] в рамках единой SQL-транзакции.
     */
    suspend fun importBackup(inputStream: InputStream): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val jsonString = inputStream.use { stream ->
                stream.bufferedReader(Charsets.UTF_8).readText()
            }

            val backupData = gson.fromJson(jsonString, BackupDataDto::class.java)
                ?: throw IllegalArgumentException("Неверный формат резервной копии JSON")

            val walletDao = database.walletDao()
            val transactionDao = database.transactionDao()
            val categoryDao = database.categoryDao()
            val templateDao = database.paymentTemplateDao()

            // Атомарная очистка и перезапись таблиц
            database.withTransaction {
                transactionDao.deleteAllTransactions()
                walletDao.deleteAllWallets()
                categoryDao.deleteAllCategories()
                templateDao.deleteAllTemplates()

                if (backupData.wallets.isNotEmpty()) {
                    walletDao.insertWallets(backupData.wallets)
                }
                if (backupData.categories.isNotEmpty()) {
                    categoryDao.insertCategories(backupData.categories)
                }
                if (backupData.transactions.isNotEmpty()) {
                    transactionDao.insertTransactions(backupData.transactions)
                }
                if (backupData.templates.isNotEmpty()) {
                    templateDao.insertTemplates(backupData.templates)
                }
            }
        }
    }
}
