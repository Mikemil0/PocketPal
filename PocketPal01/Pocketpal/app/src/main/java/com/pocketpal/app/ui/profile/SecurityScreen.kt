package com.pocketpal.app.ui.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.outlined.Computer
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Smartphone
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pocketpal.app.R
import com.pocketpal.app.auth.ErrorType
import com.pocketpal.app.ui.components.AuthScaffold
import com.pocketpal.app.ui.components.ConfirmSheet
import com.pocketpal.app.ui.components.ErrorBanner
import com.pocketpal.app.ui.components.SectionHeader
import com.pocketpal.app.ui.components.SettingsGroup
import com.pocketpal.app.ui.components.SettingsRow
import com.pocketpal.app.ui.messageRes
import com.pocketpal.app.ui.relativeTime
import com.pocketpal.app.ui.theme.pocketColors
import kotlinx.coroutines.delay

/**
 * "Seguridad" (pág. 21, RF-B4 y RF-B5): fila de cambiar contraseña con la última
 * actualización (§8.5) y lista de sesiones activas con cierre de un dispositivo
 * concreto (D-01). Para cuentas solo de Google (RN-05) no se ofrece cambiar
 * contraseña: se muestra un texto informativo. La ubicación aproximada se omite
 * (§8.5 lo permite), así que el subtítulo es "Activa" o el último uso.
 */
@Composable
fun SecurityScreen(
    state: SecurityViewModel.UiState,
    error: ErrorType?,
    onBack: () -> Unit,
    onChangePassword: () -> Unit,
    onRevokeDevice: (String) -> Unit,
    onErrorShown: () -> Unit,
) {
    var shownError by remember { mutableStateOf<ErrorType?>(null) }
    LaunchedEffect(error) {
        if (error != null) {
            shownError = error
            onErrorShown()
            delay(ERROR_SHOWN_MS)
            shownError = null
        }
    }
    var revokeTarget by remember { mutableStateOf<SecurityViewModel.DeviceUi?>(null) }

    val content = state as? SecurityViewModel.UiState.Content
    val data = content?.data

    AuthScaffold(
        onBack = onBack,
        backBackground = pocketColors.ChipPink,
        backTint = pocketColors.Coral,
    ) {
        Text(
            stringResource(R.string.security_title),
            fontSize = 22.sp,
            fontWeight = FontWeight.SemiBold,
            color = pocketColors.Ink,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(8.dp))

        if (shownError != null) {
            ErrorBanner(stringResource(shownError!!.messageRes()))
            Spacer(Modifier.height(12.dp))
        }

        SectionHeader(stringResource(R.string.security_section_password))
        if (data?.hasPassword != false) {
            SettingsGroup {
                SettingsRow(
                    icon = Icons.Outlined.Lock,
                    iconBackground = pocketColors.Rose,
                    iconTint = pocketColors.Coral,
                    title = stringResource(R.string.security_change_password),
                    subtitle = data?.passwordUpdatedAt?.let {
                        stringResource(R.string.password_last_update, relativeTime(it))
                    },
                    onClick = onChangePassword,
                )
            }
        } else {
            // RN-05: cuentas solo de Google no tienen contraseña que cambiar.
            Text(
                stringResource(R.string.security_google_only),
                fontSize = 13.sp,
                color = pocketColors.Muted,
                lineHeight = 18.sp,
                modifier = Modifier.fillMaxWidth(),
            )
        }

        SectionHeader(stringResource(R.string.security_section_sessions))
        SettingsGroup {
            data?.devices.orEmpty().forEachIndexed { i, device ->
                if (i > 0) {
                    Box(
                        Modifier.fillMaxWidth().height(1.dp)
                            .background(pocketColors.RoseBorder.copy(alpha = 0.6f))
                    )
                }
                SessionRow(
                    device = device,
                    onRevoke = { revokeTarget = device },
                )
            }
        }
        Spacer(Modifier.height(24.dp))
    }

    revokeTarget?.let { device ->
        ConfirmSheet(
            icon = Icons.AutoMirrored.Filled.Logout,
            title = stringResource(R.string.security_revoke_title),
            body = stringResource(R.string.security_revoke_body),
            confirmText = stringResource(R.string.security_revoke_confirm),
            cancelText = stringResource(R.string.common_cancel),
            onConfirm = {
                revokeTarget = null
                onRevokeDevice(device.id)
            },
            onDismiss = { revokeTarget = null },
        )
    }
}

/** Fila de sesión (pág. 21): ícono en chip de color, nombre, uso y "Cerrar". */
@Composable
private fun SessionRow(
    device: SecurityViewModel.DeviceUi,
    onRevoke: () -> Unit,
) {
    val isComputer = device.platform.equals("web", ignoreCase = true) ||
        device.platform.equals("desktop", ignoreCase = true)
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        DeviceIconChip(
            icon = if (isComputer) Icons.Outlined.Computer else Icons.Outlined.Smartphone,
            background = if (isComputer) pocketColors.BlueSoft else pocketColors.Rose,
            tint = if (isComputer) pocketColors.BlueIcon else pocketColors.Coral,
        )
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                device.deviceName,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                color = pocketColors.Ink,
            )
            Spacer(Modifier.height(2.dp))
            Text(
                if (device.isCurrent) stringResource(R.string.security_device_active)
                else relativeTime(device.lastActiveAt),
                fontSize = 12.sp,
                color = pocketColors.Muted,
            )
        }
        if (!device.isCurrent) {
            Spacer(Modifier.width(12.dp))
            Text(
                stringResource(R.string.security_revoke),
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = pocketColors.Coral,
                modifier = Modifier
                    .clickable(onClick = onRevoke)
                    .padding(8.dp),
            )
        }
    }
}

@Composable
private fun DeviceIconChip(icon: ImageVector, background: Color, tint: Color) {
    Box(
        Modifier
            .size(40.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(background),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, null, tint = tint, modifier = Modifier.size(22.dp))
    }
}

private const val ERROR_SHOWN_MS = 3_500L
