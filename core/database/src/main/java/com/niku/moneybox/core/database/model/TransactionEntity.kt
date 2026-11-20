package com.niku.moneybox.core.database.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Сущность Финансовой транзакции (Доход, Расход или Перевод между кошельками).
 *
 * Внешние ключи (Foreign Keys):
 * - [fromWalletId] связывает с кошельком списания (null для Доходов).
 * - [toWalletId] связывает с кошельком пополнения (null для Расходов).
 * - [categoryId] связывает с категорией (null для Переводов).
 *
 * Индексы настроены по кошелькам, категории и дате для мгновенного выполнения запросов,
 * отчетов, фильтрации и экспорта/импорта данных.
 */
@Entity(
    tableName = "transactions",
    foreignKeys = [
        ForeignKey(
            entity = WalletEntity::class,
            parentColumns = ["id"],
            childColumns = ["fromWalletId"],
            onDelete = ForeignKey.SET_NULL
        ),
        ForeignKey(
            entity = WalletEntity::class,
            parentColumns = ["id"],
            childColumns = ["toWalletId"],
            onDelete = ForeignKey.SET_NULL
        ),
        ForeignKey(
            entity = CategoryEntity::class,
            parentColumns = ["id"],
            childColumns = ["categoryId"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [
        Index(value = ["fromWalletId"]),
        Index(value = ["toWalletId"]),
        Index(value = ["categoryId"]),
        Index(value = ["date"])
    ]
)
data class TransactionEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    val id: Long = 0,

    @ColumnInfo(name = "fromWalletId")
    val fromWalletId: Long? = null,

    @ColumnInfo(name = "toWalletId")
    val toWalletId: Long? = null,

    @ColumnInfo(name = "amount")
    val amount: Double,

    @ColumnInfo(name = "type")
    val type: TransactionType,

    @ColumnInfo(name = "categoryId")
    val categoryId: Long? = null,

    @ColumnInfo(name = "date")
    val date: Long,

    @ColumnInfo(name = "comment")
    val comment: String? = null
)
