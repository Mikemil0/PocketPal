package com.pocketpal.app.ui.goals

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pocketpal.app.R
import com.pocketpal.app.ui.MoneyFormat
import com.pocketpal.app.ui.components.CategoryColors
import com.pocketpal.app.ui.components.CategoryIcon
import com.pocketpal.app.ui.components.PocketOutlinedTextField
import com.pocketpal.app.ui.theme.pocketColors
import java.time.LocalDate
import java.util.Locale

/*
 * Piezas visuales compartidas por todas las pantallas de Metas (base del Módulo 3).
 * Las metas reutilizan los mismos 10 íconos y 5 colores de las categorías del
 * Módulo 2 (`CategoryIcon` y `CategoryColors`), como en el Figma.
 */

/** Idioma activo de la app (para formatear fechas como "15 dic 2026"). */
@Composable
fun currentLocale(): Locale = LocalConfiguration.current.locales[0] ?: Locale.getDefault()

/** Ícono de la meta en una ficha redondeada teñida con su color. */
@Composable
fun GoalIconBadge(
    iconId: String,
    colorHex: String,
    modifier: Modifier = Modifier,
    size: Dp = 44.dp,
) {
    Box(
        modifier
            .size(size)
            .clip(RoundedCornerShape(12.dp))
            .background(CategoryColors.parse(colorHex).copy(alpha = 0.25f)),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            CategoryIcon.fromId(iconId).icon,
            stringResource(R.string.a11y_goal_icon),
            tint = pocketColors.Coral,
            modifier = Modifier.size(size / 2),
        )
    }
}

/** Barra de progreso redondeada (fondo rosado y avance del color indicado). */
@Composable
fun GoalProgressBar(
    progress: Float,
    modifier: Modifier = Modifier,
    color: Color = pocketColors.Coral,
    height: Dp = 8.dp,
) {
    Box(
        modifier
            .fillMaxWidth()
            .height(height)
            .clip(RoundedCornerShape(50))
            .background(pocketColors.ChipPink),
    ) {
        Box(
            Modifier
                .fillMaxWidth(progress.coerceIn(0f, 1f))
                .fillMaxHeight()
                .clip(RoundedCornerShape(50))
                .background(color),
        )
    }
}

/** Fila superior: botón atrás rosado y, si se pasa [onEdit], el lápiz para editar. */
@Composable
fun GoalTopBar(onBack: () -> Unit, onEdit: (() -> Unit)? = null) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        IconButton(
            onClick = onBack,
            modifier = Modifier.size(32.dp).background(pocketColors.ChipPink, RoundedCornerShape(8.dp)),
        ) {
            Icon(
                Icons.AutoMirrored.Filled.ArrowBack,
                stringResource(R.string.a11y_back),
                tint = pocketColors.Coral,
                modifier = Modifier.size(16.dp),
            )
        }
        Spacer(Modifier.weight(1f))
        if (onEdit != null) {
            IconButton(
                onClick = onEdit,
                modifier = Modifier.size(32.dp).background(pocketColors.ChipPink, RoundedCornerShape(8.dp)),
            ) {
                Icon(
                    Icons.Outlined.Edit,
                    stringResource(R.string.a11y_edit_goal),
                    tint = pocketColors.Coral,
                    modifier = Modifier.size(16.dp),
                )
            }
        }
    }
}

/**
 * Campo de monto: solo dígitos, mostrados con símbolo y puntos de miles ($300.000).
 * [digits] es el valor "crudo" (300000) que guarda la pantalla.
 */
@Composable
fun MoneyField(
    digits: String,
    onDigitsChange: (String) -> Unit,
    currency: String,
    modifier: Modifier = Modifier,
    placeholder: String? = "$0",
    enabled: Boolean = true,
    isError: Boolean = false,
) {
    PocketOutlinedTextField(
        value = MoneyFormat.formatInput(digits, currency),
        onValueChange = { onDigitsChange(it.filter(Char::isDigit).trimStart('0').take(MAX_AMOUNT_DIGITS)) },
        label = "",
        modifier = modifier,
        placeholder = placeholder,
        enabled = enabled,
        keyboardType = KeyboardType.Number,
        borderColor = if (isError) pocketColors.ErrorRed else pocketColors.RoseBorder,
        shape = RoundedCornerShape(16.dp),
    )
}

/**
 * Campo de fecha de solo lectura: al tocarlo abre el calendario de Material.
 * [isSelectable] decide qué días se pueden elegir (por ejemplo, solo futuros para la
 * fecha límite o solo pasados para un aporte).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GoalDateField(
    date: LocalDate?,
    onDateChange: (LocalDate) -> Unit,
    datePreference: String,
    isSelectable: (LocalDate) -> Boolean,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    var showPicker by remember { mutableStateOf(false) }
    val locale = currentLocale()

    Row(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(pocketColors.Card)
            .drawBehind {
                drawRoundRect(
                    color = if (enabled) PICKER_BORDER else PICKER_BORDER.copy(alpha = 0.4f),
                    cornerRadius = CornerRadius(16.dp.toPx()),
                    style = Stroke(width = 1.dp.toPx()),
                )
            }
            .clickable(enabled = enabled) { showPicker = true }
            .padding(horizontal = 16.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            date?.let { GoalFormat.inputDate(it, datePreference) }
                ?: GoalFormat.placeholder(datePreference, locale),
            fontSize = 15.sp,
            color = if (date != null && enabled) pocketColors.Ink else pocketColors.Muted,
            modifier = Modifier.weight(1f),
        )
        Icon(
            Icons.Outlined.CalendarMonth,
            stringResource(R.string.a11y_pick_date),
            tint = if (enabled) pocketColors.Coral else pocketColors.Muted,
            modifier = Modifier.size(20.dp),
        )
    }

    if (showPicker) {
        val pickerState = rememberDatePickerState(
            initialSelectedDateMillis = date?.let(GoalFormat::toPickerMillis),
            selectableDates = object : SelectableDates {
                override fun isSelectableDate(utcTimeMillis: Long): Boolean =
                    isSelectable(GoalFormat.fromPickerMillis(utcTimeMillis))
            },
        )
        DatePickerDialog(
            onDismissRequest = { showPicker = false },
            confirmButton = {
                TextButton(onClick = {
                    pickerState.selectedDateMillis?.let { onDateChange(GoalFormat.fromPickerMillis(it)) }
                    showPicker = false
                }) {
                    Text(stringResource(R.string.goal_date_ok), color = pocketColors.Coral, fontWeight = FontWeight.SemiBold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showPicker = false }) {
                    Text(stringResource(R.string.common_cancel), color = pocketColors.Muted)
                }
            },
        ) {
            DatePicker(state = pickerState)
        }
    }
}

/** Botón de borde punteado ("+ Crear nueva meta"), igual al de categorías. */
@Composable
fun DashedActionButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier
            .fillMaxWidth()
            .drawBehind {
                val stroke = 2.dp.toPx()
                drawRoundRect(
                    color = DASH_COLOR,
                    topLeft = Offset(stroke / 2, stroke / 2),
                    size = Size(size.width - stroke, size.height - stroke),
                    cornerRadius = CornerRadius(12.dp.toPx()),
                    style = Stroke(width = stroke, pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f))),
                )
            }
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 14.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Outlined.Add, null, tint = pocketColors.Coral, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(8.dp))
        Text(text, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = pocketColors.Coral)
    }
}

/** Rótulo pequeño en mayúsculas de los formularios ("NOMBRE", "MONTO"…). */
@Composable
fun GoalFieldLabel(text: String, modifier: Modifier = Modifier) {
    Text(
        text,
        fontSize = 12.sp,
        fontWeight = FontWeight.SemiBold,
        color = pocketColors.Muted,
        letterSpacing = 1.sp,
        modifier = modifier.fillMaxWidth().padding(start = 4.dp, top = 16.dp, bottom = 8.dp),
    )
}

/** Montos de hasta 12 cifras (billones), suficiente para cualquier meta personal. */
private const val MAX_AMOUNT_DIGITS = 12

/** Coral del diseño para bordes punteados y del campo de fecha (igual en claro y oscuro). */
private val DASH_COLOR = Color(0xFFFF6B6B)
private val PICKER_BORDER = Color(0xFFF4AAAA)
