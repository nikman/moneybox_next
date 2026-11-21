package com.niku.moneybox.core.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.niku.moneybox.core.database.model.WalletEntity
import kotlinx.coroutines.flow.Flow

/**
 * DAO интерфейс для работы с таблицей кошельков (Wallets).
 */
@Dao
interface WalletDao {

    /**
     * Получить список всех кошельков с их текущим обновленным балансом.
     * Используется при запуске приложения для моментального отображения балансов (холодный старт).
     */
    @Query("SELECT * FROM wallets ORDER BY sortOrder ASC, id ASC")
    fun getWalletsFlow(): Flow<List<WalletEntity>>

    @Query("SELECT * FROM wallets WHERE id = :id")
    suspend fun getWalletById(id: Long): WalletEntity?

    @Query("SELECT * FROM wallets ORDER BY sortOrder ASC, id ASC")
    suspend fun getAllWallets(): List<WalletEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWallet(wallet: WalletEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWallets(wallets: List<WalletEntity>): List<Long>

    @Update
    suspend fun updateWallet(wallet: WalletEntity): Int

    @Update
    suspend fun updateWallets(wallets: List<WalletEntity>)

    @Delete
    suspend fun deleteWallet(wallet: WalletEntity): Int

    @Query("DELETE FROM wallets")
    suspend fun deleteAllWallets()

    /**
     * Прямое атомарное обновление баланса конкретного кошелька.
     */
    @Query("UPDATE wallets SET balance = balance + :delta WHERE id = :walletId")
    suspend fun updateWalletBalance(walletId: Long, delta: Double): Int

    @Query("DELETE FROM transactions WHERE fromWalletId = :walletId OR toWalletId = :walletId")
    suspend fun deleteTransactionsByWalletId(walletId: Long): Int

    @Query("DELETE FROM payment_templates WHERE walletId = :walletId")
    suspend fun deleteTemplatesByWalletId(walletId: Long): Int

    @Query("DELETE FROM wallets WHERE id = :walletId")
    suspend fun deleteWalletById(walletId: Long): Int

    /**
     * Атомарное безопасно удаление кошелька со всеми его транзакциями и шаблонами.
     */
    @Transaction
    suspend fun deleteWalletWithTransactions(walletId: Long) {
        deleteTransactionsByWalletId(walletId)
        deleteTemplatesByWalletId(walletId)
        deleteWalletById(walletId)
    }
}
