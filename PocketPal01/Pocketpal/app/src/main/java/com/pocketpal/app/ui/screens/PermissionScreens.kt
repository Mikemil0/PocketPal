package com.pocketpal.app.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CameraAlt
import androidx.compose.material.icons.outlined.Mic
import androidx.compose.material.icons.outlined.NotificationsActive
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pocketpal.app.R
import com.pocketpal.app.navigation.AppPermissions
import com.pocketpal.app.ui.components.PocketPrimaryButton
import com.pocketpal.app.ui.components.ScrollableScreen
import com.pocketpal.app.ui.components.StepProgressBar
import com.pocketpal.app.ui.theme.PocketColors

@Composable
private fun PermissionScreen(
    step: Int,
    icon: ImageVector,
    iconTint: Color,
    iconBg: Color,
    @StringRes title: Int,
    @StringRes question: Int,
    @StringRes description: Int,
    @StringRes primaryText: Int,
    permission: String?,
    onDone: () -> Unit,
) {
    // Siempre avanzamos, sea que el usuario conceda o no el permiso.
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { onDone() }

    ScrollableScreen {
        StepProgressBar(current = step)

        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(vertical = 16.dp)) {
            Box(Modifier.size(72.dp).background(iconBg, RoundedCornerShape(18.dp)), contentAlignment = Alignment.Center) {
                Icon(icon, null, tint = iconTint, modifier = Modifier.size(32.dp))
            }
            Spacer(Modifier.height(16.dp))
            Text(stringResource(title), fontSize = 20.sp, fontWeight = FontWeight.SemiBold, color = PocketColors.Ink, textAlign = TextAlign.Center)
            Text(stringResource(question), fontSize = 12.sp, color = PocketColors.Muted, textAlign = TextAlign.Center)
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(stringResource(description), fontSize = 12.sp, color = PocketColors.Muted, textAlign = TextAlign.Center)
            Spacer(Modifier.height(16.dp))
            PocketPrimaryButton(stringResource(primaryText), { if (permission == null) onDone() else launcher.launch(permission) })
            Spacer(Modifier.height(16.dp))
            Text(stringResource(R.string.perm_not_now), fontSize = 13.sp, fontWeight = FontWeight.Medium, color = PocketColors.Muted,
                modifier = Modifier.clickable(onClick = onDone))
            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
fun NotificationPermissionScreen(onDone: () -> Unit) = PermissionScreen(
    step = 1, icon = Icons.Outlined.NotificationsActive, iconTint = PocketColors.Coral, iconBg = Color(0xFFFFD9D9),
    title = R.string.perm_notif_title, question = R.string.perm_notif_question,
    description = R.string.perm_notif_description, primaryText = R.string.perm_notif_primary,
    permission = AppPermissions.notifications,
    onDone = onDone,
)

@Composable
fun CameraPermissionScreen(onDone: () -> Unit) = PermissionScreen(
    step = 2, icon = Icons.Outlined.CameraAlt, iconTint = PocketColors.Ink, iconBg = Color(0xFFD1D5DB),
    title = R.string.perm_camera_title, question = R.string.perm_camera_question,
    description = R.string.perm_camera_description, primaryText = R.string.perm_camera_primary,
    permission = AppPermissions.camera, onDone = onDone,
)

@Composable
fun MicrophonePermissionScreen(onDone: () -> Unit) = PermissionScreen(
    step = 3, icon = Icons.Outlined.Mic, iconTint = PocketColors.Teal, iconBg = Color(0xFFCDEFE6),
    title = R.string.perm_mic_title, question = R.string.perm_mic_question,
    description = R.string.perm_mic_description, primaryText = R.string.perm_mic_primary,
    permission = AppPermissions.microphone, onDone = onDone,
)
