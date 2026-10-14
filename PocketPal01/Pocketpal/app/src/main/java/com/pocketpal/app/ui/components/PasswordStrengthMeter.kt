package com.pocketpal.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pocketpal.app.auth.PasswordRules
import com.pocketpal.app.ui.theme.PocketColors

/** Barras: Gris (0 reglas) -> Rojo (1) -> Amarillo (2) -> Verde (3). Cada regla: cumplida = verde, fallida = rojo. */
@Composable
fun PasswordStrengthMeter(password: String, confirm: String, modifier: Modifier = Modifier) {
    val rules = PasswordRules.of(password, confirm)
    val (color, label) = when (rules.score) {
        0 -> PocketColors.Gray to "Ingresa tu contraseña"
        1 -> PocketColors.ErrorRed to "Insegura"
        2 -> PocketColors.Amber to "Poco segura"
        else -> PocketColors.Teal to "Muy segura"
    }

    Column(modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            repeat(3) { i ->
                Box(
                    Modifier.weight(1f).height(3.dp).background(
                        if (i < rules.score) color else PocketColors.Gray,
                        RoundedCornerShape(2.dp),
                    )
                )
            }
        }
        Spacer(Modifier.height(4.dp))
        Text(label, fontSize = 11.sp, color = if (rules.score == 0) PocketColors.Muted else color)
        Spacer(Modifier.height(8.dp))
        RuleRow("Mínimo 8 caracteres", rules.minLength, touched = password.isNotEmpty())
        RuleRow("Incluye una mayúscula y un número", rules.upperAndDigit, touched = password.isNotEmpty())
        RuleRow("Las contraseñas coinciden", rules.matches, touched = confirm.isNotEmpty())
    }
}

@Composable
private fun RuleRow(text: String, ok: Boolean, touched: Boolean) {
    val tint = when {
        ok -> PocketColors.Teal
        touched -> PocketColors.ErrorRed
        else -> PocketColors.Muted
    }
    Row(Modifier.padding(vertical = 2.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(
            if (!ok && touched) Icons.Filled.Close else Icons.Filled.Check,
            null, tint = tint, modifier = Modifier.size(12.dp),
        )
        Spacer(Modifier.width(6.dp))
        Text(text, fontSize = 11.sp, color = tint)
    }
}
