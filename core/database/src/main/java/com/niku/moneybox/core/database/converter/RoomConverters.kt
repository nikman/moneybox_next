package com.niku.moneybox.core.database.converter

import androidx.room.TypeConverter
import com.niku.moneybox.core.database.model.CategoryType
import com.niku.moneybox.core.database.model.TransactionType
import com.niku.moneybox.core.database.model.WalletType

/**
 * Конвертеры типов для сохранения перечислений (Enum) в SQLite базу данных Room.
 */
class RoomConverters {

    @TypeConverter
    fun fromWalletType(value: WalletType): String = value.name

    @TypeConverter
    fun toWalletType(value: String): WalletType = runCatching {
        enumValueOf<WalletType>(value)
    }.getOrDefault(WalletType.OTHER)

    @TypeConverter
    fun fromTransactionType(value: TransactionType): String = value.name

    @TypeConverter
    fun toTransactionType(value: String): TransactionType = enumValueOf(value)

    @TypeConverter
    fun fromCategoryType(value: CategoryType): String = value.name

    @TypeConverter
    fun toCategoryType(value: String): CategoryType = enumValueOf(value)
}
