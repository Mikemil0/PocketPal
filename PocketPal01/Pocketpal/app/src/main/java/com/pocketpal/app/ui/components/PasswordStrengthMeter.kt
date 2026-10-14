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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pocketpal.app.R
import com.pocketpal.app.auth.PasswordRules
import com.pocketpal.app.ui.theme.PocketColors

/** Barras: Gris (0 reglas) -> Rojo (1) -> Amarillo (2) -> Verde (3). Cada regla: cumplida = verde, fallida = rojo. */
@Composable
fun PasswordStrengthMeter(password: String, confirm: String, modifier: Modifier = Modifier) {
    val rules = PasswordRules.of(password, confirm)
    val (color, labelRes) = when (rules.score) {
        0 -> PocketColors.Gray to R.string.strength_enter_password
        1 -> PocketColors.ErrorRed to R.string.strength_insecure
        2 -> PocketColors.Amber to R.string.strength_low
        else -> PocketColors.Teal to R.string.strength_high
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
        Text(stringResource(labelRes), fontSize = 11.sp, color = if (rules.score == 0) PocketColors.Muted else color)
        Spacer(Modifier.height(8.dp))
        RuleRow(stringResource(R.string.rule_min_8), rules.minLength, touched = password.isNotEmpty())
        RuleRow(stringResource(R.string.rule_upper_digit), rules.upperAndDigit, touched = password.isNotEmpty())
        RuleRow(stringResource(R.string.rule_match), rules.matches, touched = confirm.isNotEmpty())
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
