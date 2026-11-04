package com.pocketpal.app.ui.dai

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Keyboard
import androidx.compose.material.icons.outlined.MicOff
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import androidx.compose.ui.window.Dialog
import androidx.core.content.ContextCompat
import com.pocketpal.app.R
import com.pocketpal.app.data.dai.DaiVoice
import com.pocketpal.app.data.dai.VoiceProblem
import com.pocketpal.app.ui.components.PocketPrimaryButton
import com.pocketpal.app.ui.theme.pocketColors
import java.util.Locale

/**
 * Flujo 4 · "Modo voz · Estoy escuchando…" (Figma "DAI VOZ" y "VOZ ERROR").
 *
 * Usa el **reconocimiento de voz de Android** ([SpeechRecognizer], el mismo motor del
 * teclado de Google) en español de Colombia. Mientras habla:
 * - las barras se mueven con el volumen de la voz (`onRmsChanged`);
 * - se ve el texto parcial (`onPartialResults`).
 * Al terminar, el texto se manda al chat con [onHeard] (como si se hubiera escrito).
 *
 * Errores: "Disculpa, ¿puedes repetirlo?", "Hemos tenido un problema de conexión" y
 * "Micrófono bloqueado" (Abrir ajustes / Escribir).
 */
@Composable
fun DaiVoiceScreen(
    onBack: () -> Unit,
    onHeard: (String) -> Unit,
    onKeyboard: () -> Unit,
) {
    val context = LocalContext.current
    var hasPermission by remember { mutableStateOf(isMicGranted(context)) }
    var listening by remember { mutableStateOf(false) }
    var partial by remember { mutableStateOf("") }
    var problem by remember { mutableStateOf<VoiceProblem?>(null) }
    var level by remember { mutableFloatStateOf(0f) }
    // Cambia para volver a empezar a escuchar ("Repita de nuevo").
    var attempt by remember { mutableStateOf(0) }

    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        hasPermission = granted
        if (!granted) problem = VoiceProblem.Permission
    }
    LaunchedEffect(Unit) {
        if (!hasPermission) permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
    }

    val recognizer = remember {
        if (SpeechRecognizer.isRecognitionAvailable(context)) SpeechRecognizer.createSpeechRecognizer(context) else null
    }
    // Al salir de la pantalla se libera el micrófono.
    DisposableEffect(recognizer) {
        onDispose { recognizer?.destroy() }
    }

    LaunchedEffect(hasPermission, attempt) {
        if (!hasPermission) return@LaunchedEffect
        if (recognizer == null) {
            problem = VoiceProblem.Unavailable
            return@LaunchedEffect
        }
        problem = null
        partial = ""
        recognizer.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) { listening = true }
            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(rmsdB: Float) { level = ((rmsdB + 2f) / 12f).coerceIn(0f, 1f) }
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() { listening = false }
            override fun onError(error: Int) {
                listening = false
                problem = DaiVoice.problemFor(error)
            }
            override fun onResults(results: Bundle?) {
                listening = false
                val text = DaiVoice.bestResult(results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION))
                if (text == null) problem = VoiceProblem.NotUnderstood else onHeard(text)
            }
            override fun onPartialResults(partialResults: Bundle?) {
                DaiVoice.bestResult(partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION))?.let { partial = it }
            }
            override fun onEvent(eventType: Int, params: Bundle?) {}
        })
        recognizer.startListening(
            Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, "es-CO")
                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            }
        )
    }

    Column(
        Modifier.fillMaxSize().background(pocketColors.Navy).systemBarsPadding(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        DaiChatHeader(onBack = onBack)
        Spacer(Modifier.height(32.dp))
        Text(stringResource(R.string.dai_voice_title), fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White)
        Text(
            stringResource(if (problem == null) R.string.dai_voice_listening else R.string.dai_voice_paused),
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color.White,
        )
        Spacer(Modifier.height(24.dp))
        DaiAvatar(if (problem == null) DaiMood.Calm else DaiMood.Confused, size = 110.dp)
        Spacer(Modifier.height(24.dp))
        Text(
            when {
                partial.isNotBlank() -> "“$partial”"
                problem == VoiceProblem.NotUnderstood -> stringResource(R.string.dai_voice_repeat)
                problem == VoiceProblem.Connection -> stringResource(R.string.dai_voice_connection)
                problem == VoiceProblem.Unavailable -> stringResource(R.string.dai_voice_unavailable)
                else -> stringResource(R.string.dai_voice_hint)
            },
            fontSize = 13.sp,
            color = if (problem != null && partial.isBlank()) pocketColors.Coral else Color.White.copy(alpha = 0.85f),
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 32.dp),
        )
        if (problem == VoiceProblem.NotUnderstood || problem == VoiceProblem.Connection) {
            Spacer(Modifier.height(12.dp))
            Text(
                stringResource(R.string.dai_voice_retry),
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = pocketColors.Coral,
                modifier = Modifier.clip(RoundedCornerShape(8.dp)).clickable { attempt++ }.padding(8.dp),
            )
        }
        Spacer(Modifier.height(28.dp))
        VoiceWave(if (listening) level else 0.1f)
        Spacer(Modifier.weight(1f))

        // ---- X · grabar/terminar · teclado ----
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 40.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.Outlined.Close, stringResource(R.string.dai_cancel), tint = Color.White)
            }
            Box(
                Modifier
                    .size(76.dp)
                    .clip(CircleShape)
                    .background(pocketColors.Coral)
                    .clickable {
                        if (listening) recognizer?.stopListening() else attempt++
                    },
                contentAlignment = Alignment.Center,
            ) {
                Box(Modifier.size(if (listening) 20.dp else 14.dp).clip(if (listening) RoundedCornerShape(4.dp) else CircleShape).background(Color.White))
            }
            IconButton(onClick = onKeyboard) {
                Icon(Icons.Outlined.Keyboard, stringResource(R.string.dai_voice_keyboard), tint = Color.White)
            }
        }
        Spacer(Modifier.height(8.dp))
        Text(
            stringResource(if (listening) R.string.dai_voice_tap_to_end else R.string.dai_voice_tap_to_talk),
            fontSize = 11.sp,
            color = Color.White.copy(alpha = 0.7f),
        )
        Spacer(Modifier.height(32.dp))
    }

    if (problem == VoiceProblem.Permission) {
        MicBlockedDialog(
            onOpenSettings = {
                problem = null
                context.startActivity(
                    Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", context.packageName, null))
                )
            },
            onKeyboard = {
                problem = null
                onKeyboard()
            },
        )
    }
}

/** Las barras del Figma: su altura sigue el volumen de la voz. */
@Composable
private fun VoiceWave(level: Float) {
    val animated by animateFloatAsState(targetValue = level, label = "voice")
    val pattern = listOf(0.35f, 0.6f, 0.9f, 0.5f, 1f, 0.7f, 0.45f, 0.95f, 0.6f, 0.8f, 0.4f, 0.55f)
    Row(horizontalArrangement = Arrangement.spacedBy(5.dp), verticalAlignment = Alignment.CenterVertically) {
        pattern.forEach { factor ->
            Box(
                Modifier
                    .width(4.dp)
                    .height((10 + 50 * factor * (0.25f + animated)).dp)
                    .clip(RoundedCornerShape(50))
                    .background(pocketColors.Coral),
            )
        }
    }
}

/** Figma "VOZ ERROR": "Micrófono bloqueado · Actívelo en ajustes del sistema". */
@Composable
private fun MicBlockedDialog(onOpenSettings: () -> Unit, onKeyboard: () -> Unit) {
    Dialog(onDismissRequest = onKeyboard) {
        Column(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .background(Color.White)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Icon(Icons.Outlined.MicOff, null, tint = pocketColors.Ink, modifier = Modifier.size(44.dp))
            Spacer(Modifier.height(10.dp))
            Text(stringResource(R.string.dai_voice_mic_blocked), fontSize = 18.sp, fontWeight = FontWeight.Bold, color = pocketColors.Ink)
            Text(stringResource(R.string.dai_voice_mic_blocked_body), fontSize = 12.sp, color = pocketColors.Muted, textAlign = TextAlign.Center)
            Spacer(Modifier.height(18.dp))
            PocketPrimaryButton(text = stringResource(R.string.dai_open_settings_voice), onClick = onOpenSettings, containerColor = pocketColors.Teal)
            Spacer(Modifier.height(10.dp))
            PocketPrimaryButton(
                text = stringResource(R.string.dai_voice_keyboard),
                onClick = onKeyboard,
                containerColor = Color.White,
                contentColor = pocketColors.Teal,
                border = BorderStroke(1.dp, pocketColors.Teal),
            )
        }
    }
}

private fun isMicGranted(context: Context): Boolean =
    ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED

/**
 * "Voz de Dai" (Configuración): lee en voz alta cada respuesta NUEVA de Dai con el
 * **TextToSpeech** de Android, en español. Si [enabled] es `false` no hace nada.
 * Las respuestas que ya estaban cuando se abrió el chat ([loaded] = historial cargado)
 * no se leen; solo las que llegan después.
 */
@Composable
fun DaiSpeaker(enabled: Boolean, loaded: Boolean, messageId: String?, text: String?) {
    val context = LocalContext.current
    var ready by remember { mutableStateOf(false) }
    val tts = remember {
        var engine: TextToSpeech? = null
        engine = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                engine?.language = Locale("es", "CO")
                ready = true
            }
        }
        engine!!
    }
    DisposableEffect(tts) {
        onDispose {
            tts.stop()
            tts.shutdown()
        }
    }
    var primed by remember { mutableStateOf(false) }
    var lastSpoken by remember { mutableStateOf<String?>(null) }
    // Al cargar el historial se "marca" la última respuesta como ya leída.
    LaunchedEffect(loaded) {
        if (loaded && !primed) {
            primed = true
            lastSpoken = messageId
        }
    }
    LaunchedEffect(messageId, enabled, ready, primed) {
        if (!primed || !enabled || !ready || messageId == null || messageId == lastSpoken || text.isNullOrBlank()) return@LaunchedEffect
        lastSpoken = messageId
        tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, messageId)
    }
}
