package com.pocketpal.app.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.pocketpal.app.ui.theme.PocketColors

@Composable
fun PocketPrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    loading: Boolean = false,
    containerColor: Color = PocketColors.Coral,
    contentColor: Color = Color.White,
    border: BorderStroke? = null,
) {
    Button(
        onClick = onClick,
        enabled = enabled && !loading,
        modifier = modifier.fillMaxWidth().height(52.dp),
        shape = RoundedCornerShape(12.dp),
        border = border,
        colors = ButtonDefaults.buttonColors(
            containerColor = containerColor,
            contentColor = contentColor,
            disabledContainerColor = containerColor.copy(alpha = 0.6f),
            disabledContentColor = contentColor,
        ),
    ) {
        if (loading) CircularProgressIndicator(Modifier.size(20.dp), color = contentColor, strokeWidth = 2.dp)
        else Text(text, fontWeight = FontWeight.SemiBold)
    }
}

/** Botón blanco "Continuar con Google". */
@Composable
fun PocketGoogleButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier) =
    PocketPrimaryButton(text, onClick, modifier, containerColor = Color.White, contentColor = PocketColors.Ink)

/** Botón secundario outline oscuro "Ya tengo una cuenta". */
@Composable
fun PocketSecondaryButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier) =
    PocketPrimaryButton(
        text, onClick, modifier,
        containerColor = PocketColors.NavyLight,
        border = BorderStroke(1.dp, PocketColors.Muted),
    )
