package com.niku.moneybox.core.database.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Сущность метки/тега операций (например: #продукты, #подарок, #такси).
 * Содержит статистику [usageCount] для умной сортировки по популярности.
 */
@Entity(
    tableName = "tags",
    indices = [
        Index(value = ["name"], unique = true)
    ]
)
data class TagEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    val id: Long = 0,

    @ColumnInfo(name = "name")
    val name: String,

    @ColumnInfo(name = "usageCount")
    val usageCount: Int = 0
)
