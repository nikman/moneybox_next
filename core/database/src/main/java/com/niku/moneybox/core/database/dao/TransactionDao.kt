package com.niku.moneybox.core.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.niku.moneybox.core.database.model.TagEntity
import com.niku.moneybox.core.database.model.TransactionEntity
import com.niku.moneybox.core.database.model.TransactionTagCrossRef
import com.niku.moneybox.core.database.model.TransactionType
import com.niku.moneybox.core.database.model.TransactionWithTags
import kotlinx.coroutines.flow.Flow

/**
 * DAO интерфейс для работы с финансовыми транзакциями, тегами и их связями (многие-ко-многим).
 */
@Dao
interface TransactionDao {

    @Query("SELECT * FROM transactions ORDER BY date DESC")
    fun getTransactionsFlow(): Flow<List<TransactionEntity>>

    @Transaction
    @Query("SELECT * FROM transactions ORDER BY date DESC")
    fun getTransactionsWithTagsFlow(): Flow<List<TransactionWithTags>>

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

    // ---------- ТЕГИ (TAGS) ----------

    @Query("SELECT * FROM tags ORDER BY usageCount DESC, name ASC")
    fun getTagsFlow(): Flow<List<TagEntity>>

    @Query("SELECT * FROM tags ORDER BY usageCount DESC, name ASC")
    suspend fun getAllTags(): List<TagEntity>

    @Query("UPDATE tags SET usageCount = usageCount + 1 WHERE id IN (:tagIds)")
    suspend fun incrementTagUsage(tagIds: List<Long>)

    @Query("SELECT tagId FROM transaction_tag_cross_ref WHERE transactionId = :transactionId")
    suspend fun getTagIdsForTransaction(transactionId: Long): List<Long>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTag(tag: TagEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTags(tags: List<TagEntity>): List<Long>

    @Delete
    suspend fun deleteTag(tag: TagEntity)

    @Query("DELETE FROM tags")
    suspend fun deleteAllTags()

    // ---------- СВЯЗИ МНОГИЕ-КО-МНОГИМ (CROSS REF) ----------

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransactionTagCrossRefs(refs: List<TransactionTagCrossRef>)

    @Query("DELETE FROM transaction_tag_cross_ref WHERE transactionId = :transactionId")
    suspend fun deleteTransactionTagCrossRefs(transactionId: Long)

    @Query("DELETE FROM transaction_tag_cross_ref")
    suspend fun deleteAllTransactionTagCrossRefs()

    @Query("SELECT * FROM transaction_tag_cross_ref")
    suspend fun getAllTransactionTagCrossRefs(): List<TransactionTagCrossRef>

    // ---------- АТОМАРНЫЕ SQL-ТРАНЗАКЦИИ (@Transaction) ----------

    @Query("UPDATE wallets SET balance = balance + :delta WHERE id = :walletId")
    suspend fun updateWalletBalance(walletId: Long, delta: Double): Int

    /**
     * Добавление транзакции с привязкой тегов, инкрементом популярности тегов и пересчетом баланса в единой SQL-транзакции.
     */
    @Transaction
    suspend fun insertTransactionWithTags(
        transaction: TransactionEntity,
        tagIds: List<Long>
    ): Long {
        val txId = insertTransactionAndUpdateBalance(transaction)
        if (tagIds.isNotEmpty()) {
            val refs = tagIds.map { tagId -> TransactionTagCrossRef(transactionId = txId, tagId = tagId) }
            insertTransactionTagCrossRefs(refs)
            incrementTagUsage(tagIds)
        }
        return txId
    }

    /**
     * Обновление транзакции с обновлением связей тегов, инкрементом популярности добавленных тегов и баланса в единой SQL-транзакции.
     */
    @Transaction
    suspend fun updateTransactionWithTags(
        oldTransaction: TransactionEntity,
        newTransaction: TransactionEntity,
        tagIds: List<Long>
    ): Long {
        val oldTagIds = getTagIdsForTransaction(oldTransaction.id)
        val addedTagIds = tagIds.filter { it !in oldTagIds }

        deleteTransactionTagCrossRefs(oldTransaction.id)
        val newTxId = updateTransactionAndRecalculateBalance(oldTransaction, newTransaction)
        if (tagIds.isNotEmpty()) {
            val refs = tagIds.map { tagId -> TransactionTagCrossRef(transactionId = newTxId, tagId = tagId) }
            insertTransactionTagCrossRefs(refs)
        }
        if (addedTagIds.isNotEmpty()) {
            incrementTagUsage(addedTagIds)
        }
        return newTxId
    }

    /**
     * Атомарное добавление транзакции и авто-обновление балансов кошельков.
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
     * Атомарное удаление транзакции с откатом баланса.
     */
    @Transaction
    suspend fun deleteTransactionAndUpdateBalance(transaction: TransactionEntity): Int {
        deleteTransactionTagCrossRefs(transaction.id)
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

    @Transaction
    suspend fun deleteTransactionAndRevertBalance(transaction: TransactionEntity): Int {
        return deleteTransactionAndUpdateBalance(transaction)
    }

    @Transaction
    suspend fun updateTransactionAndUpdateBalance(
        oldTransaction: TransactionEntity,
        newTransaction: TransactionEntity
    ): Long {
        deleteTransactionAndUpdateBalance(oldTransaction)
        return insertTransactionAndUpdateBalance(newTransaction)
    }

    @Transaction
    suspend fun updateTransactionAndRecalculateBalance(
        oldTransaction: TransactionEntity,
        newTransaction: TransactionEntity
    ): Long {
        return updateTransactionAndUpdateBalance(oldTransaction, newTransaction)
    }
}
