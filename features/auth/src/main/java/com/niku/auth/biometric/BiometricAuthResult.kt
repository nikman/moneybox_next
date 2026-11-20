package com.niku.auth.biometric

/**
 * Результат выполнения биометрической аутентификации.
 */
sealed interface BiometricAuthResult {
    /** Успешная аутентификация (отпечаток или лицо распознаны) */
    data object Success : BiometricAuthResult

    /** Ошибка биометрии (неверный отпечаток, ошибка сканера и т.д.) */
    data class Error(val errorCode: Int, val errorMessage: String) : BiometricAuthResult

    /** Отмена аутентификации пользователем (нажатие "Отмена" или кнопки Назад) */
    data object Cancelled : BiometricAuthResult

    /** Биометрические датчики недоступны или не настроены в системе */
    data class Unavailable(val reason: String) : BiometricAuthResult
}
