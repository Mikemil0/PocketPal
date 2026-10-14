package com.pocketpal.app.ui.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AssignmentLate
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.GridOn
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selectableGroup
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pocketpal.app.R
import com.pocketpal.app.ui.components.AuthScaffold
import com.pocketpal.app.ui.components.PocketPrimaryButton
import com.pocketpal.app.ui.components.SectionHeader
import com.pocketpal.app.ui.components.SettingsGroup
import com.pocketpal.app.ui.theme.PocketColors
import com.pocketpal.app.ui.theme.pocketColors
import java.util.Date

/**
 * "Exportar datos" (págs. 39 y 56). D-05 / decisión de la Fase 8: **solo interfaz
 * y estados** — rango de fechas, formato y el estado "No hay movimientos que
 * exportar" con el botón deshabilitado. La generación del archivo y el envío por
 * correo son del Módulo 3; sin movimientos el único estado alcanzable es el vacío
 * (la pantalla "Tu historial está listo", pág. 22, llega con ese módulo).
 */
@Composable
fun ExportScreen(
    state: ExportViewModel.UiState,
    email: String,
    onBack: () -> Unit,
    onSelectRange: (ExportViewModel.Range) -> Unit,
    onSelectFormat: (ExportViewModel.Format) -> Unit,
    onCustomRange: (Long, Long) -> Unit,
) {
    var showCustomDialog by remember { mutableStateOf(false) }

    AuthScaffold(
        onBack = onBack,
        backBackground = pocketColors.ChipPink,
        backTint = pocketColors.Coral,
    ) {
        Text(
            stringResource(R.string.export_title),
            fontSize = 22.sp,
            fontWeight = FontWeight.SemiBold,
            color = pocketColors.Ink,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(8.dp))
        Text(
            stringResource(R.string.export_subtitle),
            fontSize = 13.sp,
            color = pocketColors.Muted,
            textAlign = TextAlign.Center,
            lineHeight = 18.sp,
            modifier = Modifier.fillMaxWidth(),
        )

        SectionHeader(stringResource(R.string.export_range_header))
        SettingsGroup(Modifier.semantics { selectableGroup() }) {
            ExportViewModel.Range.entries.forEach { range ->
                RadioRow(
                    label = stringResource(
                        when (range) {
                            ExportViewModel.Range.THIS_MONTH -> R.string.export_range_this_month
                            ExportViewModel.Range.LAST_3_MONTHS -> R.string.export_range_last_3_months
                            ExportViewModel.Range.THIS_YEAR -> R.string.export_range_this_year
                            ExportViewModel.Range.ALL -> R.string.export_range_all
                            ExportViewModel.Range.CUSTOM -> R.string.export_range_custom
                        }
                    ),
                    selected = state.range == range,
                    onClick = {
                        onSelectRange(range)
                        if (range == ExportViewModel.Range.CUSTOM) showCustomDialog = true
                    },
                )
            }
        }

        SectionHeader(stringResource(R.string.export_format_header))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            FormatCard(
                label = stringResource(R.string.export_format_csv),
                icon = Icons.Outlined.GridOn,
                iconBackground = pocketColors.TealSoft,
                iconTint = pocketColors.Teal,
                selected = state.format == ExportViewModel.Format.CSV,
                onClick = { onSelectFormat(ExportViewModel.Format.CSV) },
                modifier = Modifier.weight(1f),
            )
            FormatCard(
                label = stringResource(R.string.export_format_pdf),
                icon = Icons.Outlined.Description,
                iconBackground = pocketColors.Rose,
                iconTint = pocketColors.Coral,
                selected = state.format == ExportViewModel.Format.PDF,
                onClick = { onSelectFormat(ExportViewModel.Format.PDF) },
                modifier = Modifier.weight(1f),
            )
        }

        Spacer(Modifier.height(16.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                Icons.Outlined.Info,
                null,
                tint = pocketColors.Gray,
                modifier = Modifier.size(22.dp),
            )
            Spacer(Modifier.width(10.dp))
            Text(
                buildAnnotatedString {
                    append(stringResource(R.string.export_email_note_prefix))
                    withStyle(SpanStyle(fontWeight = FontWeight.Bold, color = pocketColors.Ink)) {
                        append(email)
                    }
                },
                fontSize = 12.sp,
                color = pocketColors.Muted,
                lineHeight = 16.sp,
            )
        }

        if (!state.hasMovements) {
            Spacer(Modifier.height(20.dp))
            EmptyMovementsCard()
        }

        Spacer(Modifier.height(28.dp))
        PocketPrimaryButton(
            text = stringResource(R.string.export_button),
            onClick = {}, // sin movimientos: siempre deshabilitado (RF-B9 / D-05)
            enabled = state.canExport,
            icon = Icons.Outlined.Download,
        )
        Spacer(Modifier.height(24.dp))
    }

    if (showCustomDialog) {
        CustomRangeDialog(
            initialStart = state.customStart,
            initialEnd = state.customEnd,
            onDismiss = { showCustomDialog = false },
            onApply = { start, end ->
                onCustomRange(start, end)
                showCustomDialog = false
            },
        )
    }
}

/** Fila de selección única: etiqueta a la izquierda y radio coral a la derecha (pág. 39). */
@Composable
private fun RadioRow(label: String, selected: Boolean, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(role = Role.RadioButton, onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            label,
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold,
            color = pocketColors.Ink,
            modifier = Modifier.weight(1f),
        )
        Box(
            Modifier
                .size(22.dp)
                .border(2.dp, pocketColors.Coral, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            if (selected) {
                Box(Modifier.size(10.dp).background(pocketColors.Coral, CircleShape))
            }
        }
    }
}

/** Tarjeta de formato (pág. 39): ícono en chip de color y etiqueta; la elegida lleva borde coral. */
@Composable
private fun FormatCard(
    label: String,
    icon: ImageVector,
    iconBackground: Color,
    iconTint: Color,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier
            .height(116.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(pocketColors.Card)
            .border(
                if (selected) 2.dp else 1.dp,
                if (selected) pocketColors.Coral else pocketColors.RoseBorder,
                RoundedCornerShape(16.dp),
            )
            .clickable(role = Role.RadioButton, onClick = onClick)
            .padding(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Box(
            Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(iconBackground),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, null, tint = iconTint, modifier = Modifier.size(24.dp))
        }
        Spacer(Modifier.height(10.dp))
        Text(label, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = pocketColors.Ink)
    }
}

/** Estado vacío (pág. 56): "No hay movimientos que exportar". */
@Composable
private fun EmptyMovementsCard() {
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(pocketColors.Card)
            .border(1.dp, pocketColors.RoseBorder, RoundedCornerShape(16.dp))
            .padding(horizontal = 24.dp, vertical = 28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            Modifier
                .size(64.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(pocketColors.Gray),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                Icons.Outlined.AssignmentLate,
                null,
                tint = pocketColors.Muted,
                modifier = Modifier.size(34.dp),
            )
        }
        Spacer(Modifier.height(14.dp))
        Text(
            stringResource(R.string.export_empty_title),
            fontSize = 18.sp,
            fontWeight = FontWeight.SemiBold,
            color = pocketColors.Ink,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            stringResource(R.string.export_empty_body),
            fontSize = 12.sp,
            color = pocketColors.Muted,
            textAlign = TextAlign.Center,
            lineHeight = 17.sp,
        )
    }
}

/**
 * Selector de rango personalizado (sin diseño: hallazgo 4 de §11.1). Dos
 * selectores de fecha del sistema (Material 3) para "Desde" y "Hasta".
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CustomRangeDialog(
    initialStart: Long?,
    initialEnd: Long?,
    onDismiss: () -> Unit,
    onApply: (Long, Long) -> Unit,
) {
    var start by remember { mutableStateOf(initialStart ?: System.currentTimeMillis()) }
    var end by remember { mutableStateOf(initialEnd ?: System.currentTimeMillis()) }
    var editing by remember { mutableStateOf<Boolean?>(null) } // true = desde, false = hasta

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = pocketColors.Card,
        title = {
            Text(
                stringResource(R.string.export_custom_title),
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = pocketColors.Ink,
            )
        },
        text = {
            Column(Modifier.fillMaxWidth()) {
                DateField(stringResource(R.string.export_custom_from), start) { editing = true }
                Spacer(Modifier.height(12.dp))
                DateField(stringResource(R.string.export_custom_to), end) { editing = false }
            }
        },
        confirmButton = {
            Text(
                stringResource(R.string.export_custom_apply),
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = pocketColors.Coral,
                modifier = Modifier
                    .clickable { onApply(start, end) }
                    .padding(8.dp),
            )
        },
        dismissButton = {
            Text(
                stringResource(R.string.common_cancel),
                fontSize = 14.sp,
                color = pocketColors.Muted,
                modifier = Modifier
                    .clickable(onClick = onDismiss)
                    .padding(8.dp),
            )
        },
    )

    editing?.let { isStart ->
        val pickerState = rememberDatePickerState(
            initialSelectedDateMillis = if (isStart) start else end,
        )
        DatePickerDialog(
            onDismissRequest = { editing = null },
            confirmButton = {
                Text(
                    stringResource(R.string.export_custom_apply),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = pocketColors.Coral,
                    modifier = Modifier
                        .clickable {
                            pickerState.selectedDateMillis?.let {
                                if (isStart) start = it else end = it
                            }
                            editing = null
                        }
                        .padding(8.dp),
                )
            },
            dismissButton = {
                Text(
                    stringResource(R.string.common_cancel),
                    fontSize = 14.sp,
                    color = pocketColors.Muted,
                    modifier = Modifier
                        .clickable { editing = null }
                        .padding(8.dp),
                )
            },
        ) {
            DatePicker(state = pickerState)
        }
    }
}

@Composable
private fun DateField(label: String, millis: Long, onClick: () -> Unit) {
    val context = LocalContext.current
    val formatted = remember(millis) {
        android.text.format.DateFormat.getDateFormat(context).format(Date(millis))
    }
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(pocketColors.Cream)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, fontSize = 13.sp, fontWeight = FontWeight.Medium, color = pocketColors.Ink)
        Spacer(Modifier.weight(1f))
        Text(formatted, fontSize = 13.sp, color = pocketColors.Muted)
    }
}
