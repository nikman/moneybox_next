package com.niku.moneybox.core.database.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Сущность Кошелька / Счета в БД Room.
 *
 * Флаг [excludeFromBalance] позволяет исключать накопительные счета или резервные фонды
 * из суммарных итогов на главном экране и в аналитических отчетах.
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
    val balance: Double = 0.0,

    @ColumnInfo(name = "excludeFromBalance")
    val excludeFromBalance: Boolean = false,

    @ColumnInfo(name = "sortOrder")
    val sortOrder: Int = 0
)
