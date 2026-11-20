package com.niku.moneybox.core.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.niku.moneybox.core.database.model.PaymentTemplateEntity
import kotlinx.coroutines.flow.Flow

/**
 * DAO интерфейс для управления шаблонами платежей.
 */
@Dao
interface PaymentTemplateDao {

    @Query("SELECT * FROM payment_templates ORDER BY name ASC")
    fun getTemplatesFlow(): Flow<List<PaymentTemplateEntity>>

    @Query("SELECT * FROM payment_templates")
    suspend fun getAllTemplates(): List<PaymentTemplateEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTemplate(template: PaymentTemplateEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTemplates(templates: List<PaymentTemplateEntity>): List<Long>

    @Update
    suspend fun updateTemplate(template: PaymentTemplateEntity): Int

    @Delete
    suspend fun deleteTemplate(template: PaymentTemplateEntity): Int

    @Query("DELETE FROM payment_templates")
    suspend fun deleteAllTemplates()
}
