package com.niku.moneybox.core.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.niku.moneybox.core.database.model.TransactionEntity
import com.niku.moneybox.core.database.model.TransactionType
import kotlinx.coroutines.flow.Flow

/**
 * DAO интерфейс для работы с финансовыми транзакциями.
 *
 * Включает атомарные методы в рамках одной базы данных SQL-транзакции (@Transaction).
 */
@Dao
interface TransactionDao {

    @Query("SELECT * FROM transactions ORDER BY date DESC")
    fun getTransactionsFlow(): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE date BETWEEN :startDate AND :endDate ORDER BY date DESC")
    fun getTransactionsByPeriodFlow(startDate: Long, endDate: Long): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE fromWalletId = :walletId OR toWalletId = :walletId ORDER BY date DESC")
    fun getTransactionsByWalletFlow(walletId: Long): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions")
    suspend fun getAllTransactions(): List<TransactionEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransaction(transaction: TransactionEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransactions(transactions: List<TransactionEntity>)

    @Update
    suspend fun updateTransaction(transaction: TransactionEntity): Int

    @Delete
    suspend fun deleteTransaction(transaction: TransactionEntity): Int

    @Query("DELETE FROM transactions")
    suspend fun deleteAllTransactions()

    /**
     * Атомарное изменение баланса указанного кошелька на значение [delta].
     */
    @Query("UPDATE wallets SET balance = balance + :delta WHERE id = :walletId")
    suspend fun updateWalletBalance(walletId: Long, delta: Double): Int

    /**
     * Атомарное добавление транзакции и авто-обновление балансов кошельков в рамках единой SQL-транзакции (@Transaction).
     *
     * В зависимости от типа транзакции:
     * - [TransactionType.INCOME] (Доход): Увеличивает баланс целевого кошелька (toWalletId или fromWalletId).
     * - [TransactionType.EXPENSE] (Расход): Уменьшает баланс исходного кошелька (fromWalletId или toWalletId).
     * - [TransactionType.TRANSFER] (Перевод): Уменьшает баланс отправителя (fromWalletId) и увеличивает баланс получателя (toWalletId).
     */
    @Transaction
    suspend fun insertTransactionAndUpdateBalance(transaction: TransactionEntity): Long {
        val transactionId = insertTransaction(transaction)
        val amount = transaction.amount
        when (transaction.type) {
            TransactionType.INCOME -> {
                val targetWalletId = transaction.toWalletId ?: transaction.fromWalletId
                targetWalletId?.let { walletId ->
                    updateWalletBalance(walletId, amount)
                }
            }
            TransactionType.EXPENSE -> {
                val sourceWalletId = transaction.fromWalletId ?: transaction.toWalletId
                sourceWalletId?.let { walletId ->
                    updateWalletBalance(walletId, -amount)
                }
            }
            TransactionType.TRANSFER -> {
                transaction.fromWalletId?.let { fromId ->
                    updateWalletBalance(fromId, -amount)
                }
                transaction.toWalletId?.let { toId ->
                    updateWalletBalance(toId, amount)
                }
            }
        }
        return transactionId
    }

    /**
     * Атомарное удаление транзакции с автоматическим откатом соответствующих балансов кошельков.
     */
    @Transaction
    suspend fun deleteTransactionAndUpdateBalance(transaction: TransactionEntity): Int {
        val deletedCount = deleteTransaction(transaction)
        val amount = transaction.amount
        when (transaction.type) {
            TransactionType.INCOME -> {
                val targetWalletId = transaction.toWalletId ?: transaction.fromWalletId
                targetWalletId?.let { walletId ->
                    updateWalletBalance(walletId, -amount)
                }
            }
            TransactionType.EXPENSE -> {
                val sourceWalletId = transaction.fromWalletId ?: transaction.toWalletId
                sourceWalletId?.let { walletId ->
                    updateWalletBalance(walletId, amount)
                }
            }
            TransactionType.TRANSFER -> {
                transaction.fromWalletId?.let { fromId ->
                    updateWalletBalance(fromId, amount)
                }
                transaction.toWalletId?.let { toId ->
                    updateWalletBalance(toId, -amount)
                }
            }
        }
        return deletedCount
    }

    /**
     * Псевдоним метода для отката баланса при удалении транзакции.
     */
    @Transaction
    suspend fun deleteTransactionAndRevertBalance(transaction: TransactionEntity): Int {
        return deleteTransactionAndUpdateBalance(transaction)
    }

    /**
     * Атомарное обновление транзакции (например при редактировании суммы или кошелька).
     */
    @Transaction
    suspend fun updateTransactionAndUpdateBalance(
        oldTransaction: TransactionEntity,
        newTransaction: TransactionEntity
    ): Long {
        deleteTransactionAndUpdateBalance(oldTransaction)
        return insertTransactionAndUpdateBalance(newTransaction)
    }

    /**
     * Псевдоним метода для пересчета балансов при обновлении транзакции.
     */
    @Transaction
    suspend fun updateTransactionAndRecalculateBalance(
        oldTransaction: TransactionEntity,
        newTransaction: TransactionEntity
    ): Long {
        return updateTransactionAndUpdateBalance(oldTransaction, newTransaction)
    }
}
