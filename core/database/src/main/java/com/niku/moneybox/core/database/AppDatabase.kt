package com.niku.moneybox.core.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.sqlite.db.SupportSQLiteDatabase
import com.niku.moneybox.core.database.converter.RoomConverters
import com.niku.moneybox.core.database.dao.CategoryDao
import com.niku.moneybox.core.database.dao.PaymentTemplateDao
import com.niku.moneybox.core.database.dao.TransactionDao
import com.niku.moneybox.core.database.dao.WalletDao
import com.niku.moneybox.core.database.model.CategoryEntity
import com.niku.moneybox.core.database.model.PaymentTemplateEntity
import com.niku.moneybox.core.database.model.TagEntity
import com.niku.moneybox.core.database.model.TransactionEntity
import com.niku.moneybox.core.database.model.TransactionTagCrossRef
import com.niku.moneybox.core.database.model.WalletEntity

/**
 * Главный класс базы данных Room модуля :core:database (Версия 3).
 *
 * Регистрирует сущности:
 * - [WalletEntity]
 * - [TransactionEntity]
 * - [CategoryEntity]
 * - [PaymentTemplateEntity]
 * - [TagEntity]
 * - [TransactionTagCrossRef]
 */
@Database(
    entities = [
        WalletEntity::class,
        TransactionEntity::class,
        CategoryEntity::class,
        PaymentTemplateEntity::class,
        TagEntity::class,
        TransactionTagCrossRef::class
    ],
    version = 3,
    exportSchema = false
)
@TypeConverters(RoomConverters::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun walletDao(): WalletDao

    abstract fun transactionDao(): TransactionDao

    abstract fun categoryDao(): CategoryDao

    abstract fun paymentTemplateDao(): PaymentTemplateDao

    companion object {
        const val DATABASE_NAME = "moneybox_database.db"

        /**
         * Room Callback для первичного наполнения БД базовыми категориями и тегами при первом запуске.
         */
        val PREPOPULATE_CALLBACK = object : RoomDatabase.Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                // Категории расходов (EXPENSE)
                db.execSQL("INSERT INTO categories (name, type, icon, color) VALUES ('Продукты', 'EXPENSE', 'shopping_cart', '#E53935')")
                db.execSQL("INSERT INTO categories (name, type, icon, color) VALUES ('Транспорт', 'EXPENSE', 'directions_bus', '#1E88E5')")
                db.execSQL("INSERT INTO categories (name, type, icon, color) VALUES ('Коммунальные платежи', 'EXPENSE', 'home', '#FB8C00')")
                db.execSQL("INSERT INTO categories (name, type, icon, color) VALUES ('Штрафы', 'EXPENSE', 'gavel', '#8E24AA')")
                db.execSQL("INSERT INTO categories (name, type, icon, color) VALUES ('Кафе и рестораны', 'EXPENSE', 'restaurant', '#D81B60')")
                db.execSQL("INSERT INTO categories (name, type, icon, color) VALUES ('Здоровье', 'EXPENSE', 'medical_services', '#43A047')")
                db.execSQL("INSERT INTO categories (name, type, icon, color) VALUES ('Развлечения', 'EXPENSE', 'sports_esports', '#00ACC1')")

                // Категории доходов (INCOME)
                db.execSQL("INSERT INTO categories (name, type, icon, color) VALUES ('Зарплата', 'INCOME', 'payments', '#43A047')")
                db.execSQL("INSERT INTO categories (name, type, icon, color) VALUES ('Подработка', 'INCOME', 'work', '#1E88E5')")
                db.execSQL("INSERT INTO categories (name, type, icon, color) VALUES ('Кэшбэк', 'INCOME', 'savings', '#FB8C00')")
                db.execSQL("INSERT INTO categories (name, type, icon, color) VALUES ('Подарки', 'INCOME', 'card_giftcard', '#E53935')")

                // Начальные популярные теги
                db.execSQL("INSERT INTO tags (name) VALUES ('срочно')")
                db.execSQL("INSERT INTO tags (name) VALUES ('отпуск')")
                db.execSQL("INSERT INTO tags (name) VALUES ('семья')")
                db.execSQL("INSERT INTO tags (name) VALUES ('работа')")
            }
        }
    }
}
