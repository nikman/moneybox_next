package com.niku.moneybox.core.database.model

/**
 * Тип финансовой операции (транзакции).
 */
enum class TransactionType {
    INCOME,   // Доход (пополнение)
    EXPENSE,  // Расход (списание)
    TRANSFER  // Перевод между двумя кошельками
}
