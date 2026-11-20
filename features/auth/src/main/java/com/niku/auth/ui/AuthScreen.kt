package com.niku.auth.ui

import android.content.Context
import android.content.ContextWrapper
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import com.niku.auth.biometric.BiometricAuthResult
import com.niku.auth.biometric.BiometricAuthenticator
import com.niku.auth.biometric.BiometricStatus

/**
 * Вспомогательное расширение для поиска [FragmentActivity] из контекста Compose.
 */
private fun Context.findFragmentActivity(): FragmentActivity? {
    var currentContext = this
    while (currentContext is ContextWrapper) {
        if (currentContext is FragmentActivity) {
            return currentContext
        }
        currentContext = currentContext.baseContext
    }
    return null
}

/**
 * UI-экран авторизации / экран блокировки (AuthScreen / Lock Screen).
 *
 * Особенности:
 * 1. Отображает современный лаконичный интерфейс с логотипом приложения и приветствием.
 * 2. Автоматически запрашивает сканирование биометрии (отпечаток/лицо) при появлении экрана.
 * 3. Содержит кнопку "Войти по биометрии" на случай отмены или ручного вызова сканирования.
 * 4. Вызывает навигационный callback [onAuthSuccess] при успешной аутентификации.
 */
@Composable
fun AuthScreen(
    onAuthSuccess: () -> Unit,
    modifier: Modifier = Modifier,
    authenticator: BiometricAuthenticator = remember { BiometricAuthenticator() },
) {
    val context = LocalContext.current
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var biometricStatus by remember { mutableStateOf<BiometricStatus?>(null) }

    // Вызов системного диалога сканирования биометрии
    val triggerBiometricAuth = {
        val activity = context.findFragmentActivity()
        if (activity != null) {
            authenticator.authenticate(
                activity = activity,
                title = "Вход в MoneyBox",
                subtitle = "Подтвердите личность для доступа к финансам",
                negativeButtonText = "Отмена",
            ) { result ->
                when (result) {
                    is BiometricAuthResult.Success -> {
                        errorMessage = null
                        onAuthSuccess()
                    }
                    is BiometricAuthResult.Error -> {
                        errorMessage = result.errorMessage
                    }
                    is BiometricAuthResult.Unavailable -> {
                        errorMessage = result.reason
                    }
                    is BiometricAuthResult.Cancelled -> {
                        errorMessage = "Авторизация отменена"
                    }
                }
            }
        } else {
            errorMessage = "Для работы биометрии требуется активный FragmentActivity"
        }
    }

    // Автоматический запуск проверки при открытии экрана
    LaunchedEffect(Unit) {
        val status = authenticator.checkBiometricStatus(context)
        biometricStatus = status
        when (status) {
            is BiometricStatus.Available -> triggerBiometricAuth()
            is BiometricStatus.NoneEnrolled -> errorMessage = "Отпечаток пальца или Face ID не настроены в устройстве"
            is BiometricStatus.NoHardware -> errorMessage = "Сканер биометрии отсутствует на данном устройстве"
            is BiometricStatus.HardwareUnavailable -> errorMessage = "Сканер биометрии временно недоступен"
            is BiometricStatus.Error -> errorMessage = status.message
        }
    }

    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            // Логотип / Иконка замка
            Surface(
                modifier = Modifier.size(96.dp),
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.fillMaxSize(),
                ) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = "Логотип MoneyBox",
                        modifier = Modifier.size(44.dp),
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Название приложения
            Text(
                text = "MoneyBox",
                style = MaterialTheme.typography.headlineLarge.copy(
                    fontWeight = FontWeight.Bold,
                ),
                color = MaterialTheme.colorScheme.onBackground,
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Подзаголовок
            Text(
                text = "Учет личных финансов\nПодтвердите личность для входа",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )

            Spacer(modifier = Modifier.height(48.dp))

            // Кнопка ручного вызова биометрии
            Button(
                onClick = { triggerBiometricAuth() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = MaterialTheme.shapes.medium,
            ) {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = null,
                    modifier = Modifier.padding(end = 8.dp),
                )
                Text(
                    text = "Войти по биометрии",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.SemiBold,
                    ),
                )
            }

            // Блок вывода ошибок
            errorMessage?.let { error ->
                Spacer(modifier = Modifier.height(24.dp))
                Text(
                    text = error,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 16.dp),
                )
            }
        }
    }
}
