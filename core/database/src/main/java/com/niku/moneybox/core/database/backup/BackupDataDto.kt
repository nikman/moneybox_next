package com.niku.moneybox.core.database.backup

import com.niku.moneybox.core.database.model.CategoryEntity
import com.niku.moneybox.core.database.model.PaymentTemplateEntity
import com.niku.moneybox.core.database.model.TagEntity
import com.niku.moneybox.core.database.model.TransactionEntity
import com.niku.moneybox.core.database.model.TransactionTagCrossRef
import com.niku.moneybox.core.database.model.WalletEntity

/**
 * DTO модель полной резервной копии данных приложения MoneyBox.
 */
data class BackupDataDto(
    val version: Int = 3,
    val createdAt: Long = System.currentTimeMillis(),
    val wallets: List<WalletEntity> = emptyList(),
    val transactions: List<TransactionEntity> = emptyList(),
    val categories: List<CategoryEntity> = emptyList(),
    val templates: List<PaymentTemplateEntity> = emptyList(),
    val tags: List<TagEntity> = emptyList(),
    val transactionTagCrossRefs: List<TransactionTagCrossRef> = emptyList()
)
