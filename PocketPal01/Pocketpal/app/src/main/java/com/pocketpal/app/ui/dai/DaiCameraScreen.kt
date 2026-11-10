package com.pocketpal.app.ui.dai

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.clickable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.PhotoLibrary
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import com.pocketpal.app.R
import com.pocketpal.app.ui.components.PocketPrimaryButton
import com.pocketpal.app.ui.components.PocketSecondaryButton
import com.pocketpal.app.ui.theme.PocketColors
import android.graphics.ImageFormat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Flujo 3 · Factura con la cámara.
 *
 * Vista previa (CameraX) → botón de captura (o una foto de la **galería**) → OCR en el
 * teléfono (ML Kit, alfabeto latino) → [ReceiptOcr] rearma las filas de la factura → el
 * TEXTO se entrega con [onTextRecognized]. Si no se pudo leer nada, se muestra el aviso con
 * "Tomar otra foto" / "Ingresar manual".
 */
@androidx.annotation.OptIn(androidx.camera.core.ExperimentalGetImage::class)
@Composable
fun DaiCameraScreen(
    state: DaiReceiptViewModel.UiState,
    onBack: () -> Unit,
    onReading: () -> Unit,
    onTextRecognized: (String) -> Unit,
    onReadFailed: () -> Unit,
    onManual: () -> Unit,
    onRetake: () -> Unit,
    /** La foto preparada para el servidor de Dai (o `null`); se envía mientras ML Kit lee. */
    onPhoto: (ByteArray?) -> Unit = {},
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val lifecycleOwner = LocalLifecycleOwner.current
    var hasPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                    PackageManager.PERMISSION_GRANTED,
        )
    }
    var permissionAsked by remember { mutableStateOf(false) }
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        hasPermission = it
        permissionAsked = true
    }
    LaunchedEffect(Unit) {
        if (!hasPermission) permissionLauncher.launch(Manifest.permission.CAMERA)
    }

    val imageCapture = remember {
        ImageCapture.Builder()
            // Máxima calidad: el OCR lee mejor los números pequeños de la factura.
            .setCaptureMode(ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY)
            .build()
    }
    val recognizer = remember { TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS) }
    androidx.compose.runtime.DisposableEffect(recognizer) { onDispose { recognizer.close() } }

    val reading = state.stage == DaiReceiptViewModel.Stage.Reading

    fun takePhoto() {
        if (reading) return
        onReading()
        imageCapture.takePicture(
            ContextCompat.getMainExecutor(context),
            object : ImageCapture.OnImageCapturedCallback() {
                override fun onCaptureSuccess(image: ImageProxy) {
                    val media = image.image
                    if (media == null) {
                        image.close()
                        onReadFailed()
                        return
                    }
                    // Copia de la foto para el servidor de Dai (antes de cerrar la imagen).
                    val rotation = image.imageInfo.rotationDegrees
                    val jpeg = if (image.format == ImageFormat.JPEG) {
                        image.planes.firstOrNull()?.buffer?.let { buffer ->
                            buffer.rewind()
                            ByteArray(buffer.remaining()).also { buffer.get(it); buffer.rewind() }
                        }
                    } else {
                        null
                    }
                    scope.launch(Dispatchers.Default) {
                        onPhoto(jpeg?.let { ReceiptPhoto.fromJpeg(it, rotation) })
                    }
                    val input = InputImage.fromMediaImage(media, image.imageInfo.rotationDegrees)
                    recognizer.process(input)
                        .addOnSuccessListener { onTextRecognized(ReceiptOcr.toText(it)) }
                        .addOnFailureListener { onReadFailed() }
                        .addOnCompleteListener { image.close() }
                }

                override fun onError(exception: ImageCaptureException) = onReadFailed()
            },
        )
    }

    // Elegir una foto de la factura desde la galería (no necesita permisos extra).
    val galleryLauncher = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        onReading()
        scope.launch(Dispatchers.IO) { onPhoto(ReceiptPhoto.fromUri(context, uri)) }
        try {
            recognizer.process(InputImage.fromFilePath(context, uri))
                .addOnSuccessListener { onTextRecognized(ReceiptOcr.toText(it)) }
                .addOnFailureListener { onReadFailed() }
        } catch (e: Exception) {
            onReadFailed()
        }
    }

    Box(Modifier.fillMaxSize().background(Color.Black)) {
        if (hasPermission) {
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { ctx ->
                    PreviewView(ctx).also { previewView ->
                        val providerFuture = ProcessCameraProvider.getInstance(ctx)
                        providerFuture.addListener({
                            val provider = providerFuture.get()
                            val preview = Preview.Builder().build()
                                .also { it.setSurfaceProvider(previewView.surfaceProvider) }
                            try {
                                provider.unbindAll()
                                provider.bindToLifecycle(
                                    lifecycleOwner,
                                    CameraSelector.DEFAULT_BACK_CAMERA,
                                    preview,
                                    imageCapture,
                                )
                            } catch (e: Exception) {
                                onReadFailed()
                            }
                        }, ContextCompat.getMainExecutor(ctx))
                    }
                },
            )
        }

        Column(
            Modifier.fillMaxSize().systemBarsPadding().padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier.clip(CircleShape).background(Color.Black.copy(alpha = 0.4f)),
            ) {
                Icon(Icons.AutoMirrored.Outlined.ArrowBack, stringResource(R.string.dai_receipt_back), tint = Color.White)
            }

            when {
                !hasPermission -> PermissionPanel(
                    asked = permissionAsked,
                    onAllow = { permissionLauncher.launch(Manifest.permission.CAMERA) },
                    onManual = onManual,
                )
                reading -> Column(
                    Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    CircularProgressIndicator(color = Color.White)
                    Text(stringResource(R.string.dai_receipt_reading), color = Color.White, fontSize = 15.sp)
                }
                else -> Column(
                    Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    if (state.readFailed) {
                        Text(
                            stringResource(R.string.dai_receipt_read_failed),
                            color = Color.White,
                            textAlign = TextAlign.Center,
                            modifier = Modifier
                                .clip(androidx.compose.foundation.shape.RoundedCornerShape(12.dp))
                                .background(Color.Black.copy(alpha = 0.6f))
                                .padding(12.dp),
                        )
                    } else {
                        Text(
                            stringResource(R.string.dai_receipt_hint),
                            color = Color.White,
                            textAlign = TextAlign.Center,
                        )
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = {
                                galleryLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                            },
                            modifier = Modifier.size(48.dp).clip(CircleShape).background(Color.Black.copy(alpha = 0.4f)),
                        ) {
                            Icon(Icons.Outlined.PhotoLibrary, stringResource(R.string.dai_receipt_gallery), tint = Color.White)
                        }
                        Spacer(Modifier.width(28.dp))
                        Box(
                            Modifier
                                .size(72.dp)
                                .clip(CircleShape)
                                .background(Color.White)
                                .clickable(onClickLabel = stringResource(R.string.dai_receipt_shutter)) { takePhoto() }
                                .padding(6.dp)
                                .clip(CircleShape)
                                .background(PocketColors.Coral),
                        )
                        // Ocupa el mismo espacio que la galería para que el botón quede centrado.
                        Spacer(Modifier.width(76.dp))
                    }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                        Text(
                            stringResource(R.string.dai_receipt_manual),
                            color = Color.White,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.clickable { onManual() }.padding(8.dp),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PermissionPanel(asked: Boolean, onAllow: () -> Unit, onManual: () -> Unit) {
    Column(
        Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            stringResource(if (asked) R.string.dai_receipt_perm_denied else R.string.dai_receipt_perm_needed),
            color = Color.White,
            textAlign = TextAlign.Center,
        )
        PocketPrimaryButton(text = stringResource(R.string.dai_receipt_perm_allow), onClick = onAllow)
        PocketSecondaryButton(text = stringResource(R.string.dai_receipt_manual), onClick = onManual)
    }
}