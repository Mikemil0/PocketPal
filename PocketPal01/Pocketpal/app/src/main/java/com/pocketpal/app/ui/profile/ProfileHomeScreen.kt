package com.pocketpal.app.ui.profile

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.automirrored.outlined.HelpOutline
import androidx.compose.material.icons.outlined.AttachMoney
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.HelpOutline
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pocketpal.app.R
import com.pocketpal.app.auth.ErrorType
import com.pocketpal.app.ui.components.*
import com.pocketpal.app.ui.messageRes
import com.pocketpal.app.ui.theme.PocketColors
import kotlinx.coroutines.delay

/**
 * RF-B1: "Mi perfil" (págs. 33 y 52). Avatar con insignia de lápiz (abre la hoja de
 * foto), nombre y correo, contadores a 0 (§1.3: `movements` y `goals` no existen aún),
 * secciones CUENTA y PREFERENCIAS, Ayuda y Acerca de, y Cerrar sesión con hoja de
 * confirmación.
 *
 * Las filas de pantallas de fases futuras se muestran como en el Figma y, al tocarlas,
 * avisan "Próximamente" (decisión del usuario en la entrega de la Fase 3).
 */
@Composable
fun ProfileHomeScreen(
    state: ProfileViewModel.UiState,
    error: ErrorType?,
    onBack: () -> Unit,
    onOpenPersonalData: () -> Unit,
    onSignOut: () -> Unit,
    onAvatarBytes: (ByteArray?) -> Unit,
    onRemoveAvatar: () -> Unit,
    onErrorShown: () -> Unit,
) {
    val context = LocalContext.current
    val comingSoonText = stringResource(R.string.profile_coming_soon)
    val comingSoon = {
        Toast.makeText(context, comingSoonText, Toast.LENGTH_SHORT).show()
    }
    val pickers = rememberAvatarPickers(onImage = onAvatarBytes)
    var showPhotoSheet by remember { mutableStateOf(false) }
    var showSignOutSheet by remember { mutableStateOf(false) }

    val content = state as? ProfileViewModel.UiState.Content
    val data = content?.data

    // El error se muestra unos segundos y se consume en la ViewModel (RF-A3/RNF-05).
    var shownError by remember { mutableStateOf<ErrorType?>(null) }
    LaunchedEffect(error) {
        if (error != null) {
            shownError = error
            onErrorShown()
            delay(ERROR_SHOWN_MS)
            shownError = null
        }
    }

    AuthScaffold(onBack = onBack) {
        Text(
            stringResource(R.string.profile_title),
            fontSize = 22.sp,
            fontWeight = FontWeight.SemiBold,
            color = PocketColors.Ink,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(16.dp))

        if (shownError != null) {
            ErrorBanner(stringResource(shownError!!.messageRes()))
            Spacer(Modifier.height(12.dp))
        }

        Column(
            Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Avatar(
                name = data?.fullName.orEmpty(),
                email = data?.email.orEmpty(),
                avatarUrl = data?.avatarUrl,
                size = 140.dp,
                contentDescription = stringResource(R.string.a11y_change_photo),
                onEditClick = { showPhotoSheet = true },
            )
            Spacer(Modifier.height(12.dp))
            Text(
                data?.fullName.orEmpty(),
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = PocketColors.Ink,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(2.dp))
            Text(
                data?.email.orEmpty(),
                fontSize = 14.sp,
                color = PocketColors.Muted,
                textAlign = TextAlign.Center,
            )
        }

        Spacer(Modifier.height(20.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            StatCard(
                value = data?.registeredExpenses ?: 0,
                label = stringResource(R.string.profile_stats_expenses),
                modifier = Modifier.weight(1f),
            )
            StatCard(
                value = data?.activeGoals ?: 0,
                label = stringResource(R.string.profile_stats_goals),
                modifier = Modifier.weight(1f),
            )
        }

        SectionHeader(stringResource(R.string.profile_section_account))
        SettingsGroup {
            SettingsRow(
                icon = Icons.Outlined.Person,
                iconBackground = PocketColors.Rose,
                iconTint = PocketColors.Coral,
                title = stringResource(R.string.profile_row_personal_data),
                subtitle = stringResource(R.string.profile_row_personal_data_sub),
                onClick = onOpenPersonalData,
            )
            SettingsRow(
                icon = Icons.Outlined.Lock,
                iconBackground = PocketColors.BlueSoft,
                iconTint = PocketColors.BlueIcon,
                title = stringResource(R.string.profile_row_security),
                subtitle = stringResource(R.string.profile_row_security_sub),
                dividerAbove = true,
                onClick = comingSoon,
            )
            SettingsRow(
                icon = Icons.Outlined.Notifications,
                iconBackground = PocketColors.TealSoft,
                iconTint = PocketColors.Teal,
                title = stringResource(R.string.profile_row_notifications),
                subtitle = stringResource(R.string.profile_row_notifications_sub),
                dividerAbove = true,
                onClick = comingSoon,
            )
        }

        SectionHeader(stringResource(R.string.profile_section_preferences))
        SettingsGroup {
            SettingsRow(
                icon = Icons.Outlined.Tune,
                iconBackground = PocketColors.Rose,
                iconTint = PocketColors.Coral,
                title = stringResource(R.string.profile_row_categories),
                subtitle = stringResource(R.string.profile_row_categories_sub),
                onClick = comingSoon,
            )
            SettingsRow(
                icon = Icons.Outlined.AttachMoney,
                iconBackground = PocketColors.BlueSoft,
                iconTint = PocketColors.BlueIcon,
                title = stringResource(R.string.profile_row_preferences),
                subtitle = stringResource(R.string.profile_row_preferences_sub),
                dividerAbove = true,
                onClick = comingSoon,
            )
            SettingsRow(
                icon = Icons.Outlined.Download,
                iconBackground = PocketColors.TealSoft,
                iconTint = PocketColors.Teal,
                title = stringResource(R.string.profile_row_export),
                subtitle = stringResource(R.string.profile_row_export_sub),
                dividerAbove = true,
                onClick = comingSoon,
            )
        }

        Spacer(Modifier.height(16.dp))
        SettingsGroup {
            SettingsRow(
                icon = Icons.AutoMirrored.Outlined.HelpOutline,
                iconBackground = PocketColors.Rose,
                iconTint = PocketColors.Coral,
                title = stringResource(R.string.profile_row_help),
                onClick = comingSoon,
            )
            SettingsRow(
                icon = Icons.Outlined.Info,
                iconBackground = PocketColors.BlueSoft,
                iconTint = PocketColors.BlueIcon,
                title = stringResource(R.string.profile_row_about),
                dividerAbove = true,
                onClick = comingSoon,
            )
        }

        Spacer(Modifier.height(16.dp))
        SettingsGroup {
            SettingsRow(
                icon = Icons.AutoMirrored.Filled.Logout,
                iconBackground = PocketColors.Rose,
                iconTint = PocketColors.Coral,
                title = stringResource(R.string.profile_row_sign_out),
                titleColor = PocketColors.Coral,
                showChevron = false,
                onClick = { showSignOutSheet = true },
            )
        }
        Spacer(Modifier.height(24.dp))
    }

    if (showPhotoSheet) {
        PhotoActionSheet(
            hasPhoto = data?.avatarUrl != null,
            onTakePhoto = pickers.takePhoto,
            onPickFromGallery = pickers.pickFromGallery,
            onRemovePhoto = onRemoveAvatar,
            onDismiss = { showPhotoSheet = false },
        )
    }

    if (showSignOutSheet) {
        ConfirmSheet(
            icon = Icons.AutoMirrored.Filled.Logout,
            title = stringResource(R.string.signout_title),
            body = stringResource(R.string.signout_body),
            confirmText = stringResource(R.string.signout_confirm),
            cancelText = stringResource(R.string.common_cancel),
            onConfirm = { showSignOutSheet = false; onSignOut() },
            onDismiss = { showSignOutSheet = false },
        )
    }
}

@Composable
private fun StatCard(value: Int, label: String, modifier: Modifier = Modifier) {
    Column(
        modifier
            .background(Color.White, RoundedCornerShape(16.dp))
            .border(1.dp, PocketColors.RoseBorder, RoundedCornerShape(16.dp))
            .padding(vertical = 16.dp, horizontal = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(value.toString(), fontSize = 26.sp, fontWeight = FontWeight.Bold, color = PocketColors.Ink)
        Spacer(Modifier.height(4.dp))
        Text(label, fontSize = 12.sp, color = PocketColors.Muted, textAlign = TextAlign.Center)
    }
}

private const val ERROR_SHOWN_MS = 3_500L
