package com.niku.moneybox.core.database.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Сущность Шаблона платежа / быстрого транша (например "Кофе с собой", "Аренда", "Коммуналка").
 *
 * Внешние ключи (Foreign Keys):
 * - [walletId] -> WalletEntity.id (Кошелек операции)
 * - [categoryId] -> CategoryEntity.id (Категория операции)
 */
@Entity(
    tableName = "payment_templates",
    foreignKeys = [
        ForeignKey(
            entity = WalletEntity::class,
            parentColumns = ["id"],
            childColumns = ["walletId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = CategoryEntity::class,
            parentColumns = ["id"],
            childColumns = ["categoryId"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [
        Index(value = ["walletId"]),
        Index(value = ["categoryId"])
    ]
)
data class PaymentTemplateEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    val id: Long = 0,

    @ColumnInfo(name = "name")
    val name: String,

    @ColumnInfo(name = "walletId")
    val walletId: Long,

    @ColumnInfo(name = "categoryId")
    val categoryId: Long? = null,

    @ColumnInfo(name = "amount")
    val amount: Double,

    @ColumnInfo(name = "type")
    val type: TransactionType
)
