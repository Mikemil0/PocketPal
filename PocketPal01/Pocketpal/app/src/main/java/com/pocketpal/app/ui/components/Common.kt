package com.pocketpal.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pocketpal.app.R
import com.pocketpal.app.ui.theme.PocketColors

@Composable
fun AuthScaffold(
    modifier: Modifier = Modifier,
    onBack: (() -> Unit)? = null,
    background: Color = PocketColors.Cream,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier.fillMaxSize().background(background)
            .systemBarsPadding().imePadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 16.dp),
    ) {
        if (onBack != null) {
            IconButton(
                onClick = onBack,
                modifier = Modifier.size(32.dp).background(PocketColors.Ink, RoundedCornerShape(8.dp)),
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    stringResource(R.string.a11y_back),
                    tint = Color.White,
                    modifier = Modifier.size(16.dp),
                )
            }
            Spacer(Modifier.height(24.dp))
        }
        content()
    }
}

@Composable
fun ScreenHeader(title: String, subtitle: String, centered: Boolean = false) {
    val align = if (centered) TextAlign.Center else TextAlign.Start
    Text(title, fontSize = 22.sp, fontWeight = FontWeight.SemiBold, color = PocketColors.Ink,
        modifier = Modifier.fillMaxWidth(), textAlign = align)
    Spacer(Modifier.height(4.dp))
    Text(subtitle, fontSize = 13.sp, color = PocketColors.Muted,
        modifier = Modifier.fillMaxWidth(), textAlign = align)
    Spacer(Modifier.height(16.dp))
}

/** Barra superior en 3 pasos: completados teal, actual coral, pendientes gris. */
@Composable
fun StepProgressBar(current: Int, total: Int = 3, modifier: Modifier = Modifier) {
    Row(modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        repeat(total) { i ->
            Box(
                Modifier.weight(1f).height(3.dp).background(
                    when {
                        i < current - 1 -> PocketColors.Teal
                        i == current - 1 -> PocketColors.Coral
                        else -> PocketColors.Gray
                    },
                    RoundedCornerShape(2.dp),
                )
            )
        }
    }
}

@Composable
fun ErrorBanner(
    text: String,
    modifier: Modifier = Modifier,
    actionText: String? = null,
    onAction: (() -> Unit)? = null,
    icon: ImageVector = Icons.Outlined.ErrorOutline,
) {
    Row(
        modifier.fillMaxWidth()
            .background(PocketColors.ErrorBg, RoundedCornerShape(10.dp))
            .border(1.dp, PocketColors.ErrorRed, RoundedCornerShape(10.dp))
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, null, tint = PocketColors.ErrorRed, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(8.dp))
        Text(
            buildAnnotatedString {
                append(text)
                if (actionText != null) {
                    append(" ")
                    withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { append(actionText) }
                }
            },
            fontSize = 12.sp, color = PocketColors.ErrorRed,
            modifier = if (onAction != null) Modifier.clickable(onClick = onAction) else Modifier,
        )
    }
}

/** "¿No tienes cuenta? Regístrate" */
@Composable
fun BottomLink(prefix: String, action: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Row(modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
        Text(prefix, fontSize = 12.sp, color = PocketColors.Muted)
        Spacer(Modifier.width(4.dp))
        Text(action, fontSize = 12.sp, color = PocketColors.Coral, fontWeight = FontWeight.Medium,
            modifier = Modifier.clickable(onClick = onClick))
    }
}

/**
 * Pantalla de altura completa que se desplaza cuando el contenido no cabe (por ejemplo, al girar a horizontal).
 *
 * Nota: un `Spacer(Modifier.weight(1f))` NO funciona dentro de un scroll (la altura es "infinita"),
 * por eso el espacio sobrante se reparte con `Arrangement.SpaceBetween` y `heightIn(min = maxHeight)`.
 * En vertical se ve igual que antes; en horizontal, el contenido simplemente hace scroll.
 */
@Composable
fun ScrollableScreen(
    modifier: Modifier = Modifier,
    background: Color = PocketColors.Cream,
    verticalArrangement: Arrangement.Vertical = Arrangement.SpaceBetween,
    horizontalAlignment: Alignment.Horizontal = Alignment.CenterHorizontally,
    content: @Composable ColumnScope.() -> Unit,
) {
    BoxWithConstraints(modifier.fillMaxSize().background(background).systemBarsPadding()) {
        Column(
            Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .heightIn(min = maxHeight)
                .padding(24.dp),
            verticalArrangement = verticalArrangement,
            horizontalAlignment = horizontalAlignment,
            content = content,
        )
    }
}
