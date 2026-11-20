package com.niku.moneybox.core.database.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Сущность Кошелька / Счета в БД Room.
 *
 * Оптимизация холодного запуска:
 * Поле [balance] хранит актуальный баланс кошелька, который
 * атомарно обновляется внутри SQL-транзакции при любых изменениях транзакций.
 * Благодаря этому при старте приложения список кошельков с их текущим балансом
 * загружается мгновенно (O(1)) без обращения к истории транзакций и их пересчета.
 */
@Entity(tableName = "wallets")
data class WalletEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    val id: Long = 0,

    @ColumnInfo(name = "name")
    val name: String,

    @ColumnInfo(name = "type")
    val type: WalletType,

    @ColumnInfo(name = "currency")
    val currency: String,

    @ColumnInfo(name = "balance")
    val balance: Double = 0.0
)
