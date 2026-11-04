package com.pocketpal.app.ui.dai

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.outlined.CameraAlt
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.RecordVoiceOver
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import com.pocketpal.app.ui.components.ErrorBanner
import com.pocketpal.app.ui.components.PocketPrimaryButton
import com.pocketpal.app.ui.messageRes
import com.pocketpal.app.ui.theme.pocketColors

/** Barra azul con el título centrado (las pantallas de configuración no muestran a Dai arriba). */
@Composable
private fun SettingsHeader(title: String, onBack: () -> Unit) {
    Box(Modifier.fillMaxWidth().background(pocketColors.Navy).padding(vertical = 8.dp)) {
        IconButton(onClick = onBack, modifier = Modifier.align(Alignment.CenterStart)) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.a11y_back), tint = Color.White)
        }
        Text(title, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White, modifier = Modifier.align(Alignment.Center))
    }
}

/**
 * Flujo 4 · "Configuración de Dai" (Figma "11_Configuraciones"): Voz, Cámara, Acerca de
 * Dai y Borrar historial.
 */
@Composable
fun DaiSettingsScreen(
    voiceReplies: Boolean,
    onBack: () -> Unit,
    onVoiceRepliesChange: (Boolean) -> Unit,
    onCameraSettings: () -> Unit,
    onClearHistory: () -> Unit,
) {
    var showAbout by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxSize().background(pocketColors.Navy).systemBarsPadding()) {
        SettingsHeader(stringResource(R.string.dai_settings_title), onBack)
        Column(
            Modifier
                .fillMaxSize()
                .background(pocketColors.Cream)
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            DaiAvatar(DaiMood.Grateful, size = 96.dp)
            Spacer(Modifier.height(10.dp))
            Text("Dai", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = pocketColors.Ink)
            Text(stringResource(R.string.dai_tagline), fontSize = 13.sp, color = pocketColors.Muted)
            Spacer(Modifier.height(24.dp))

            SettingRow(
                icon = Icons.Outlined.RecordVoiceOver,
                iconTint = pocketColors.Coral,
                title = stringResource(R.string.dai_settings_voice),
                subtitle = stringResource(R.string.dai_settings_voice_sub),
                onClick = { onVoiceRepliesChange(!voiceReplies) },
            ) {
                Switch(
                    checked = voiceReplies,
                    onCheckedChange = onVoiceRepliesChange,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = pocketColors.Coral,
                        uncheckedThumbColor = Color.White,
                        uncheckedTrackColor = pocketColors.ToggleOff,
                        uncheckedBorderColor = pocketColors.ToggleOff,
                    ),
                )
            }
            SettingRow(Icons.Outlined.CameraAlt, pocketColors.Teal, stringResource(R.string.dai_settings_camera),
                stringResource(R.string.dai_settings_camera_sub), onCameraSettings)
            SettingRow(Icons.Outlined.Info, pocketColors.BlueIcon, stringResource(R.string.dai_settings_about),
                stringResource(R.string.dai_settings_about_sub), onClick = { showAbout = true })
            SettingRow(Icons.Outlined.DeleteOutline, pocketColors.Coral, stringResource(R.string.dai_settings_clear),
                stringResource(R.string.dai_settings_clear_sub), onClearHistory)

            Spacer(Modifier.height(24.dp))
            Text(stringResource(R.string.dai_settings_footer), fontSize = 10.sp, color = pocketColors.Muted)
        }
    }

    if (showAbout) {
        AlertDialog(
            onDismissRequest = { showAbout = false },
            title = { Text(stringResource(R.string.dai_settings_about)) },
            text = { Text(stringResource(R.string.dai_about_body)) },
            confirmButton = {
                TextButton(onClick = { showAbout = false }) {
                    Text(stringResource(R.string.dai_ok), color = pocketColors.Coral)
                }
            },
        )
    }
}

@Composable
private fun SettingRow(
    icon: ImageVector,
    iconTint: Color,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    trailing: @Composable () -> Unit = {
        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, tint = pocketColors.Muted)
    },
) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White)
            .border(1.dp, pocketColors.RoseBorder, RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier.size(40.dp).clip(CircleShape).background(iconTint.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, null, tint = iconTint, modifier = Modifier.size(20.dp))
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(title, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = pocketColors.Ink)
            Text(subtitle, fontSize = 11.sp, color = pocketColors.Muted)
        }
        trailing()
    }
}

/**
 * Flujo 4 · "Borrar historial" (Figma "12_Borrar_Historial"): Dai triste, la pregunta,
 * "Ten en cuenta" y los botones Eliminar historial / Cancelar.
 */
@Composable
fun DaiClearHistoryScreen(
    deleting: Boolean,
    error: ErrorType?,
    onBack: () -> Unit,
    onConfirm: () -> Unit,
) {
    Column(Modifier.fillMaxSize().background(pocketColors.Navy).systemBarsPadding()) {
        SettingsHeader(stringResource(R.string.dai_settings_clear), onBack)
        Column(
            Modifier
                .fillMaxSize()
                .background(pocketColors.Cream)
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.height(8.dp))
            DaiAvatar(DaiMood.Sad, size = 96.dp)
            Spacer(Modifier.height(16.dp))
            Text(
                stringResource(R.string.dai_clear_title),
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = pocketColors.Ink,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(10.dp))
            Text(stringResource(R.string.dai_clear_body), fontSize = 12.sp, color = pocketColors.Muted, textAlign = TextAlign.Center)
            Spacer(Modifier.height(20.dp))
            Column(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color.White)
                    .border(1.dp, pocketColors.RoseBorder, RoundedCornerShape(14.dp))
                    .padding(14.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.Shield, null, tint = pocketColors.Coral, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.dai_clear_note_title), fontSize = 13.sp, fontWeight = FontWeight.Bold, color = pocketColors.Ink)
                }
                Spacer(Modifier.height(6.dp))
                listOf(R.string.dai_clear_note_1, R.string.dai_clear_note_2, R.string.dai_clear_note_3).forEach {
                    Text("• " + stringResource(it), fontSize = 11.sp, color = pocketColors.Ink, modifier = Modifier.padding(vertical = 2.dp))
                }
            }
            if (error != null) {
                Spacer(Modifier.height(12.dp))
                ErrorBanner(stringResource(error.messageRes()))
            }
            Spacer(Modifier.height(28.dp))
            PocketPrimaryButton(text = stringResource(R.string.dai_clear_confirm), onClick = onConfirm, loading = deleting)
            Spacer(Modifier.height(10.dp))
            PocketPrimaryButton(
                text = stringResource(R.string.dai_cancel),
                onClick = onBack,
                enabled = !deleting,
                containerColor = Color.White,
                contentColor = pocketColors.Coral,
                border = BorderStroke(1.dp, pocketColors.Coral),
            )
        }
    }
}
