package com.niku.auth.biometric

import android.content.Context
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_STRONG
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_WEAK
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity

/**
 * Менеджер биометрической аутентификации приложения.
 *
 * Отвечает за:
 * 1. Проверку наличия и доступности биометрических сканеров (BiometricManager).
 * 2. Запуск системного диалогового окна сканирования отпечатка пальца / Face ID (BiometricPrompt).
 * 3. Возврат результата аутентификации через обратный вызов (callback).
 */
class BiometricAuthenticator {

    /**
     * Проверяет доступность биометрических датчиков на устройстве.
     */
    fun checkBiometricStatus(context: Context): BiometricStatus {
        val biometricManager = BiometricManager.from(context)
        val authenticators = BIOMETRIC_STRONG or BIOMETRIC_WEAK

        return when (biometricManager.canAuthenticate(authenticators)) {
            BiometricManager.BIOMETRIC_SUCCESS -> BiometricStatus.Available
            BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE -> BiometricStatus.NoHardware
            BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE -> BiometricStatus.HardwareUnavailable
            BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED -> BiometricStatus.NoneEnrolled
            else -> BiometricStatus.Error("Биометрическая аутентификация недоступна")
        }
    }

    /**
     * Запускает системный диалог сканирования биометрии.
     *
     * @param activity FragmentActivity, необходимая для отображения системного BiometricPrompt.
     * @param title Заголовок окна аутентификации.
     * @param subtitle Подзаголовок окна.
     * @param negativeButtonText Текст кнопки отмены.
     * @param onResult Функция обратного вызова с результатом [BiometricAuthResult].
     */
    fun authenticate(
        activity: FragmentActivity,
        title: String = "Вход в MoneyBox",
        subtitle: String = "Подтвердите личность для входа в приложение",
        negativeButtonText: String = "Отмена",
        onResult: (BiometricAuthResult) -> Unit,
    ) {
        val status = checkBiometricStatus(activity)
        if (status != BiometricStatus.Available) {
            val reason = when (status) {
                BiometricStatus.NoHardware -> "На устройстве отсутствует сканер биометрии"
                BiometricStatus.HardwareUnavailable -> "Сканер биометрии временно недоступен"
                BiometricStatus.NoneEnrolled -> "Биометрические данные не настроены в устройстве"
                is BiometricStatus.Error -> status.message
                else -> "Биометрическая аутентификация недоступна"
            }
            onResult(BiometricAuthResult.Unavailable(reason))
            return
        }

        val executor = ContextCompat.getMainExecutor(activity)

        val callback = object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                super.onAuthenticationSucceeded(result)
                onResult(BiometricAuthResult.Success)
            }

            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                super.onAuthenticationError(errorCode, errString)
                if ((errorCode == BiometricPrompt.ERROR_USER_CANCELED) ||
                    (errorCode == BiometricPrompt.ERROR_NEGATIVE_BUTTON)
                ) {
                    onResult(BiometricAuthResult.Cancelled)
                } else {
                    onResult(BiometricAuthResult.Error(errorCode, errString.toString()))
                }
            }

            override fun onAuthenticationFailed() {
                super.onAuthenticationFailed()
                onResult(BiometricAuthResult.Error(-1, "Личность не распознана. Попробуйте снова."))
            }
        }

        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle(title)
            .setSubtitle(subtitle)
            .setNegativeButtonText(negativeButtonText)
            .setAllowedAuthenticators(BIOMETRIC_STRONG or BIOMETRIC_WEAK)
            .build()

        val biometricPrompt = BiometricPrompt(activity, executor, callback)
        biometricPrompt.authenticate(promptInfo)
    }
}
