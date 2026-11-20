package com.niku.auth.biometric

/**
 * Статус доступности биометрических датчиков на устройстве.
 */
sealed interface BiometricStatus {
    /** Биометрия доступна и готова к использованию */
    data object Available : BiometricStatus

    /** На устройстве отсутствует аппаратный биометрический сканер */
    data object NoHardware : BiometricStatus

    /** Биометрический сканер временно недоступен */
    data object HardwareUnavailable : BiometricStatus

    /** Отпечаток пальца или Face ID не зарегистрированы в настройках Android */
    data object NoneEnrolled : BiometricStatus

    /** Ошибка определения статуса биометрии */
    data class Error(val message: String) : BiometricStatus
}
