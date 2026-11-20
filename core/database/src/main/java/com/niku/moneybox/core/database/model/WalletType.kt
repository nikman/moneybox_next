package com.niku.moneybox.core.database.model

/**
 * Тип кошелька/счета.
 * Поддерживает перевод денежных средств между кошельками любого типа.
 */
enum class WalletType {
    CASH,          // Наличные деньги
    DEBIT_CARD,    // Дебетовая карта
    CREDIT_CARD,   // Кредитная карта
    SAVINGS,       // Сберегательный счет / Вклад
    INVESTMENT,    // Инвестиционный счет
    CRYPTO,        // Криптовалютный кошелек
    OTHER          // Прочее
}
