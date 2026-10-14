package com.pocketpal.app.ui.profile

import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import com.pocketpal.app.R
import com.pocketpal.app.data.profile.AvatarImageEncoder
import com.pocketpal.app.navigation.AppPermissions
import com.pocketpal.app.ui.components.PocketPrimaryButton
import com.pocketpal.app.ui.theme.PocketColors
import kotlinx.coroutines.launch
import java.io.File

/**
 * Hoja inferior de la foto de perfil (pág. 20, §8.3): **Tomar foto**,
 * **Elegir de la galería**, **Quitar foto actual** (solo si hay foto) y **Cancelar**.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PhotoActionSheet(
    hasPhoto: Boolean,
    onTakePhoto: () -> Unit,
    onPickFromGallery: () -> Unit,
    onRemovePhoto: () -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState()
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = PocketColors.Cream,
    ) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 24.dp)) {
            PhotoActionRow(
                icon = Icons.Outlined.PhotoCamera,
                label = stringResource(R.string.photo_sheet_take),
                onClick = { onTakePhoto(); onDismiss() },
            )
            PhotoActionRow(
                icon = Icons.Outlined.Image,
                label = stringResource(R.string.photo_sheet_gallery),
                onClick = { onPickFromGallery(); onDismiss() },
            )
            if (hasPhoto) {
                PhotoActionRow(
                    icon = Icons.Outlined.Delete,
                    label = stringResource(R.string.photo_sheet_remove),
                    destructive = true,
                    onClick = { onRemovePhoto(); onDismiss() },
                )
            }
            Spacer(Modifier.height(16.dp))
            PocketPrimaryButton(
                stringResource(R.string.common_cancel),
                onDismiss,
                containerColor = Color.White,
                contentColor = PocketColors.Muted,
                border = BorderStroke(1.dp, PocketColors.Gray),
            )
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun PhotoActionRow(
    icon: ImageVector,
    label: String,
    destructive: Boolean = false,
    onClick: () -> Unit,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .size(56.dp)
                .background(
                    if (destructive) PocketColors.Coral else PocketColors.Rose,
                    RoundedCornerShape(18.dp),
                ),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                icon,
                null,
                tint = if (destructive) Color.White else PocketColors.Coral,
                modifier = Modifier.size(26.dp),
            )
        }
        Spacer(Modifier.width(16.dp))
        Text(
            label,
            fontSize = 17.sp,
            fontWeight = FontWeight.SemiBold,
            color = if (destructive) PocketColors.Coral else PocketColors.Ink,
        )
    }
}

/**
 * Lanza la cámara o el selector de fotos del sistema y entrega la imagen ya
 * comprimida (RNF-04: máx. 512 px, JPEG ~80 %); `null` si no se pudo leer.
 *
 * - **Tomar foto:** con `CAMERA` declarado en el Manifest el permiso se pide en este
 *   momento (§8.3, reutiliza [AppPermissions]); la captura comparte un `content://`
 *   del caché con FileProvider.
 * - **Galería:** `PickVisualMedia` (Photo Picker del sistema), que no pide permisos.
 */
@Composable
fun rememberAvatarPickers(onImage: (ByteArray?) -> Unit): AvatarPickers {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val currentOnImage by rememberUpdatedState(onImage)
    var cameraUri by remember { mutableStateOf<Uri?>(null) }

    fun deliver(uri: Uri) {
        scope.launch { currentOnImage(AvatarImageEncoder.encode(context, uri)) }
    }

    val takePicture = rememberLauncherForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { success ->
        if (success) cameraUri?.let { deliver(it) }
    }
    val pickMedia = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri -> uri?.let { deliver(it) } }
    val requestPermission = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) cameraUri?.let { takePicture.launch(it) }
    }

    return remember(context) {
        AvatarPickers(
            takePhoto = {
                val uri = createCameraUri(context)
                cameraUri = uri
                if (AppPermissions.isGranted(context, AppPermissions.camera)) {
                    takePicture.launch(uri)
                } else {
                    requestPermission.launch(AppPermissions.camera)
                }
            },
            pickFromGallery = {
                pickMedia.launch(
                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                )
            },
        )
    }
}

class AvatarPickers(
    val takePhoto: () -> Unit,
    val pickFromGallery: () -> Unit,
)

/** Archivo temporal en el caché compartido con la cámara mediante FileProvider. */
private fun createCameraUri(context: Context): Uri {
    val dir = File(context.cacheDir, "camera").apply { mkdirs() }
    val file = File.createTempFile("avatar_", ".jpg", dir)
    return FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
}
