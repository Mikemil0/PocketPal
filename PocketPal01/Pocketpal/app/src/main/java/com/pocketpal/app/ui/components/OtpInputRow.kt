package com.pocketpal.app.ui.components

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pocketpal.app.ui.theme.PocketColors

/** Un único BasicTextField oculto, dibujado como N recuadros. */
@Composable
fun OtpInputRow(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    length: Int = 6,
    isError: Boolean = false,
) {
    BasicTextField(
        value = value,
        onValueChange = { if (it.length <= length && it.all(Char::isDigit)) onValueChange(it) },
        modifier = modifier,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
        cursorBrush = SolidColor(Color.Transparent),
        decorationBox = {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                repeat(length) { i ->
                    val active = i == value.length
                    Box(
                        Modifier
                            .size(46.dp, 56.dp)
                            .border(
                                if (active || value.getOrNull(i) != null) 2.dp else 1.dp,
                                when {
                                    isError -> PocketColors.ErrorRed
                                    active -> PocketColors.Ink
                                    else -> PocketColors.Coral
                                },
                                RoundedCornerShape(10.dp),
                            ),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            value.getOrNull(i)?.toString().orEmpty(),
                            fontSize = 22.sp, fontWeight = FontWeight.SemiBold, color = PocketColors.Ink,
                        )
                    }
                }
            }
        },
    )
}
