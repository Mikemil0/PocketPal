package com.pocketpal.app.ui.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Alarm
import androidx.compose.material.icons.outlined.Campaign
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material3.Icon
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
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
import com.pocketpal.app.data.notification.NotificationSettings
import com.pocketpal.app.ui.components.AuthScaffold
import com.pocketpal.app.ui.components.ErrorBanner
import com.pocketpal.app.ui.components.SectionHeader
import com.pocketpal.app.ui.components.SettingsGroup
import com.pocketpal.app.ui.messageRes
import com.pocketpal.app.ui.theme.pocketColors
import kotlinx.coroutines.delay

/**
 * "Notificaciones" (pág. 36, RF-B6): recordatorio diario, resumen semanal, alerta de
 * presupuesto con deslizador (RN-08: 50–100, por defecto 80), progreso de metas y
 * canales. Los cambios se guardan automáticamente (§8.7); el envío real no es de
 * este módulo. El chip SMS está deshabilitado sin teléfono verificado (RN-07).
 */
@Composable
fun NotificationsScreen(
    state: NotificationSettingsViewModel.UiState,
    error: ErrorType?,
    smsEnabled: Boolean,
    onBack: () -> Unit,
    onToggleDailyReminder: () -> Unit,
    onToggleWeeklySummary: () -> Unit,
    onToggleBudgetAlert: () -> Unit,
    onToggleGoalsProgress: () -> Unit,
    onToggleChannelEmail: () -> Unit,
    onToggleChannelSms: () -> Unit,
    onThresholdChange: (Int) -> Unit,
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

    AuthScaffold(
        onBack = onBack,
        backBackground = pocketColors.ChipPink,
        backTint = pocketColors.Coral,
    ) {
        Text(
            stringResource(R.string.notif_title),
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

        val data = (state as? NotificationSettingsViewModel.UiState.Content)?.data

        SectionHeader(stringResource(R.string.notif_section_habit))
        SettingsGroup {
            NotificationRow(
                icon = Icons.Outlined.Alarm,
                iconBackground = pocketColors.Rose,
                iconTint = pocketColors.Coral,
                title = stringResource(R.string.notif_daily),
                subtitle = stringResource(R.string.notif_daily_sub),
                checked = data?.dailyReminder ?: false,
                onCheckedChange = onToggleDailyReminder,
            )
            RowDivider()
            NotificationRow(
                icon = Icons.Outlined.Campaign,
                iconBackground = pocketColors.Rose,
                iconTint = pocketColors.Coral,
                title = stringResource(R.string.notif_weekly),
                subtitle = stringResource(R.string.notif_weekly_sub),
                checked = data?.weeklySummary ?: false,
                onCheckedChange = onToggleWeeklySummary,
            )
        }

        SectionHeader(stringResource(R.string.notif_section_budgets))
        SettingsGroup {
            NotificationRow(
                icon = Icons.Outlined.Security,
                iconBackground = pocketColors.BlueSoft,
                iconTint = pocketColors.BlueIcon,
                title = stringResource(R.string.notif_budget),
                subtitle = stringResource(R.string.notif_budget_sub),
                checked = data?.budgetAlert ?: false,
                onCheckedChange = onToggleBudgetAlert,
            )
            RowDivider()
            ThresholdSlider(
                enabled = data?.budgetAlert ?: false,
                value = data?.budgetThreshold ?: 80,
                onValueChange = onThresholdChange,
            )
        }

        SectionHeader(stringResource(R.string.notif_section_goals))
        SettingsGroup {
            NotificationRow(
                icon = Icons.Outlined.CheckCircle,
                iconBackground = pocketColors.TealSoft,
                iconTint = pocketColors.Teal,
                title = stringResource(R.string.notif_goals),
                subtitle = stringResource(R.string.notif_goals_sub),
                checked = data?.goalsProgress ?: false,
                onCheckedChange = onToggleGoalsProgress,
            )
        }

        SectionHeader(stringResource(R.string.notif_section_channels))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            ChannelChip(
                label = stringResource(R.string.notif_channel_email),
                selected = data?.channelEmail ?: true,
                enabled = true,
                onClick = onToggleChannelEmail,
                modifier = Modifier.weight(1f),
            )
            // RN-07: el canal SMS solo se habilita con un teléfono verificado (Fase 7).
            ChannelChip(
                label = stringResource(R.string.notif_channel_sms),
                selected = data?.channelSms ?: false,
                enabled = smsEnabled,
                onClick = onToggleChannelSms,
                modifier = Modifier.weight(1f),
            )
        }
        Spacer(Modifier.height(10.dp))
        Text(
            stringResource(R.string.notif_sms_note),
            fontSize = 11.sp,
            color = pocketColors.Muted,
            textAlign = TextAlign.Center,
            lineHeight = 15.sp,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(24.dp))
    }
}

/** Fila con ícono en chip de color, textos e interruptor (pág. 36). */
@Composable
private fun NotificationRow(
    icon: ImageVector,
    iconBackground: Color,
    iconTint: Color,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: () -> Unit,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onCheckedChange)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(iconBackground),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, null, tint = iconTint, modifier = Modifier.size(22.dp))
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(title, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = pocketColors.Ink)
            Spacer(Modifier.height(2.dp))
            Text(subtitle, fontSize = 12.sp, color = pocketColors.Muted)
        }
        Spacer(Modifier.width(12.dp))
        Switch(
            checked = checked,
            onCheckedChange = { onCheckedChange() },
            modifier = Modifier.size(48.dp),
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = pocketColors.Teal,
                checkedBorderColor = pocketColors.Teal,
                uncheckedThumbColor = Color.White,
                uncheckedTrackColor = pocketColors.ToggleOff,
                uncheckedBorderColor = pocketColors.ToggleOff,
            ),
        )
    }
}

/** Deslizador "Avisar al llegar a" (RN-08): activo solo con la alerta encendida (§8.7). */
@Composable
private fun ThresholdSlider(enabled: Boolean, value: Int, onValueChange: (Int) -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .alpha(if (enabled) 1f else 0.5f)
            .padding(horizontal = 16.dp, vertical = 8.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                stringResource(R.string.notif_threshold_label),
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = pocketColors.Ink,
                modifier = Modifier.weight(1f),
            )
            Text(
                stringResource(R.string.notif_threshold_value, value),
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = pocketColors.Coral,
            )
        }
        Slider(
            value = value.toFloat(),
            onValueChange = { if (enabled) onValueChange(it.toInt()) },
            valueRange = 50f..100f,
            steps = 49,
            enabled = enabled,
            colors = SliderDefaults.colors(
                thumbColor = Color.White,
                activeTrackColor = pocketColors.Coral,
                inactiveTrackColor = pocketColors.Gray,
                activeTickColor = pocketColors.Coral,
                inactiveTickColor = pocketColors.Gray,
            ),
        )
        Row(Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.notif_threshold_min), fontSize = 11.sp, color = pocketColors.Muted)
            Spacer(Modifier.weight(1f))
            Text(stringResource(R.string.notif_threshold_max), fontSize = 11.sp, color = pocketColors.Muted)
        }
    }
}

/**
 * Chip de canal (pág. 36). La maqueta muestra ambos iguales (azul claro); el estado
 * elegido se marca con borde y texto fuerte, y el deshabilitado baja la opacidad.
 */
@Composable
private fun ChannelChip(
    label: String,
    selected: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier
            .alpha(if (enabled) 1f else 0.5f)
            .clip(RoundedCornerShape(12.dp))
            .background(pocketColors.BlueSoft)
            .border(
                if (selected) 1.5.dp else 1.dp,
                if (selected) pocketColors.BlueIcon else pocketColors.BlueSoft,
                RoundedCornerShape(12.dp),
            )
            .clickable(enabled = enabled, onClick = onClick)
            .padding(vertical = 12.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            label,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            color = if (selected) pocketColors.Ink else pocketColors.Muted,
        )
    }
}

@Composable
private fun RowDivider() {
    Box(Modifier.fillMaxWidth().height(1.dp).background(pocketColors.RoseBorder.copy(alpha = 0.6f)))
}

private const val ERROR_SHOWN_MS = 3_500L
