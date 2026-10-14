package com.pocketpal.app.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pocketpal.app.R
import com.pocketpal.app.ui.theme.PocketColors
import com.pocketpal.app.ui.theme.pocketColors
import kotlinx.coroutines.delay

/**
 * RF-A4 / D-16: pantalla final del splash (tras el splash del sistema). Muestra el
 * logo de la billetera con "PocketPal" centrado sobre azul marino, con una animación
 * breve de entrada, **mientras se restaura la sesión guardada** (D-13).
 *
 * Después llama a [onDecided] con `true` (Home) o `false` (Welcome). El tiempo mínimo
 * en pantalla deja verse la animación aunque la sesión se resuelva al instante.
 */
@Composable
fun SplashScreen(
    resolveSession: suspend () -> Boolean,
    onDecided: (toHome: Boolean) -> Unit,
) {
    var visible by remember { mutableStateOf(false) }
    val alpha by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = tween(durationMillis = ANIMATION_MS),
        label = "splash_alpha",
    )
    val scale by animateFloatAsState(
        targetValue = if (visible) 1f else 0.92f,
        animationSpec = tween(durationMillis = ANIMATION_MS),
        label = "splash_scale",
    )

    Box(
        Modifier.fillMaxSize().background(pocketColors.Navy),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.graphicsLayer { this.alpha = alpha; scaleX = scale; scaleY = scale },
        ) {
            Image(
                painter = painterResource(R.drawable.logo_pocketpal),
                contentDescription = stringResource(R.string.brand_name),
                modifier = Modifier.size(150.dp),
            )
            Spacer(Modifier.height(8.dp))
            Text(
                buildAnnotatedString {
                    withStyle(SpanStyle(color = Color.White)) { append(stringResource(R.string.brand_pocket)) }
                    withStyle(SpanStyle(color = pocketColors.Coral)) { append(stringResource(R.string.brand_pal)) }
                },
                fontSize = 44.sp,
                fontWeight = FontWeight.Bold,
            )
        }
    }

    LaunchedEffect(Unit) {
        visible = true
        delay(MIN_VISIBLE_MS)
        onDecided(resolveSession())
    }
}

private const val ANIMATION_MS = 500
private const val MIN_VISIBLE_MS = 700L
