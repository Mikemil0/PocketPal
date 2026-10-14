package com.pocketpal.app.ui.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.Payments
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pocketpal.app.R
import com.pocketpal.app.auth.ErrorType
import com.pocketpal.app.ui.components.AuthScaffold
import com.pocketpal.app.ui.components.ErrorBanner
import com.pocketpal.app.ui.components.SectionHeader
import com.pocketpal.app.ui.components.SettingsGroup
import com.pocketpal.app.ui.components.SettingsRow
import com.pocketpal.app.ui.messageRes
import com.pocketpal.app.ui.theme.PocketColors
import com.pocketpal.app.ui.theme.pocketColors
import kotlinx.coroutines.delay
import java.util.Locale

/**
 * "Preferencias generales" (pág. 42, RF-B8): región y formato (moneda, idioma,
 * formato de fecha), apariencia (Claro / Oscuro / Automático, D-03) e inicio de
 * semana. Cada cambio se aplica de inmediato en toda la app. La sección "DATOS"
 * del Figma está cortada y sin contenido definido (hallazgo 1): no se muestra.
 */
@Composable
fun PreferencesScreen(
    state: PreferencesViewModel.UiState,
    error: ErrorType?,
    onBack: () -> Unit,
    onOpenCurrency: () -> Unit,
    onOpenLanguage: () -> Unit,
    onOpenDateFormat: () -> Unit,
    onThemeChange: (String) -> Unit,
    onWeekStartChange: (Int) -> Unit,
    onErrorShown: () -> Unit,
) {
    var shownError by remember { mutableStateOf<ErrorType?>(null) }
    LaunchedEffect(error) {
        if (error != null) {
            shownError = error
            onErrorShown()
            delay(ERROR_SHOWN_MS)
            shownError = null
        }
    }

    AuthScaffold(
        onBack = onBack,
        backBackground = pocketColors.ChipPink,
        backTint = pocketColors.Coral,
    ) {
        Text(
            stringResource(R.string.prefs_title),
            fontSize = 22.sp,
            fontWeight = FontWeight.SemiBold,
            color = pocketColors.Ink,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(8.dp))

        if (shownError != null) {
            ErrorBanner(stringResource(shownError!!.messageRes()))
            Spacer(Modifier.height(12.dp))
        }

        SectionHeader(stringResource(R.string.prefs_section_region))
        SettingsGroup {
            SettingsRow(
                icon = Icons.Outlined.Payments,
                iconBackground = pocketColors.Rose,
                iconTint = pocketColors.Coral,
                title = stringResource(R.string.prefs_currency),
                subtitle = stringResource(R.string.prefs_currency_sub),
                value = stringResource(currencyLabelRes(state.currency)),
                onClick = onOpenCurrency,
            )
            SettingsRow(
                icon = Icons.Outlined.Language,
                iconBackground = pocketColors.TealSoft,
                iconTint = pocketColors.Teal,
                title = stringResource(R.string.prefs_language),
                subtitle = stringResource(R.string.prefs_language_sub),
                value = languageLabel(state.language),
                valueColor = pocketColors.Teal,
                dividerAbove = true,
                onClick = onOpenLanguage,
            )
            SettingsRow(
                icon = Icons.Outlined.CalendarMonth,
                iconBackground = pocketColors.BlueSoft,
                iconTint = pocketColors.BlueIcon,
                title = stringResource(R.string.prefs_date_format),
                subtitle = stringResource(R.string.prefs_date_format_sub),
                value = stringResource(dateFormatLabelRes(state.dateFormat)),
                dividerAbove = true,
                onClick = onOpenDateFormat,
            )
        }

        SectionHeader(stringResource(R.string.prefs_section_appearance))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            ThemeChip(
                label = stringResource(R.string.prefs_theme_light),
                kind = ThemeKind.LIGHT,
                selected = state.theme == "light",
                onClick = { onThemeChange("light") },
                modifier = Modifier.weight(1f),
            )
            ThemeChip(
                label = stringResource(R.string.prefs_theme_dark),
                kind = ThemeKind.DARK,
                selected = state.theme == "dark",
                onClick = { onThemeChange("dark") },
                modifier = Modifier.weight(1f),
            )
            ThemeChip(
                label = stringResource(R.string.prefs_theme_system),
                kind = ThemeKind.SYSTEM,
                selected = state.theme == "system",
                onClick = { onThemeChange("system") },
                modifier = Modifier.weight(1f),
            )
        }

        SectionHeader(stringResource(R.string.prefs_section_week))
        SettingsGroup {
            Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp)) {
                Text(
                    stringResource(R.string.prefs_week_start),
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = pocketColors.Ink,
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    stringResource(R.string.prefs_week_start_sub),
                    fontSize = 12.sp,
                    color = pocketColors.Muted,
                )
                Spacer(Modifier.height(12.dp))
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    // 0 = domingo (§6); etiquetas localizadas (RN-11).
                    val initials = listOf(
                        R.string.day_initial_sun to R.string.day_name_sun,
                        R.string.day_initial_mon to R.string.day_name_mon,
                        R.string.day_initial_tue to R.string.day_name_tue,
                        R.string.day_initial_wed to R.string.day_name_wed,
                        R.string.day_initial_thu to R.string.day_name_thu,
                        R.string.day_initial_fri to R.string.day_name_fri,
                        R.string.day_initial_sat to R.string.day_name_sat,
                    )
                    initials.forEachIndexed { day, (initial, name) ->
                        DayChip(
                            label = stringResource(initial),
                            contentDescription = stringResource(name),
                            selected = state.weekStart == day,
                            onClick = { onWeekStartChange(day) },
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }
        }
        Spacer(Modifier.height(24.dp))
    }
}

private enum class ThemeKind { LIGHT, DARK, SYSTEM }

/**
 * Ficha de apariencia (pág. 42): cuadrado con la vista previa del tema y su
 * etiqueta debajo. La elegida lleva anillo coral (patrón de la pág. 15). Los
 * colores de las vistas previas son fijos: muestran AMBOS temas sin importar el
 * tema activo.
 */
@Composable
private fun ThemeChip(
    label: String,
    kind: ThemeKind,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        val shape = RoundedCornerShape(12.dp)
        Box(
            Modifier
                .height(60.dp)
                .fillMaxWidth()
                .clip(shape)
                .background(Color(0xFF243447))
                .border(if (selected) 2.dp else 1.dp, if (selected) pocketColors.Coral else pocketColors.Gray, shape)
                .clickable(role = Role.RadioButton, onClick = onClick),
        ) {
            when (kind) {
                ThemeKind.LIGHT -> Box(Modifier.matchParentSize().background(PocketColors.Cream, shape))
                ThemeKind.DARK -> Unit // el navy ya pinta el fondo
                ThemeKind.SYSTEM -> Row(Modifier.matchParentSize()) {
                    Box(Modifier.weight(1f).fillMaxHeight().background(Color(0xFF243447)))
                    Box(Modifier.weight(1f).fillMaxHeight().background(PocketColors.Cream))
                }
            }
        }
        Spacer(Modifier.height(6.dp))
        Text(label, fontSize = 11.sp, color = pocketColors.Muted)
    }
}

/** Chip de día de la semana (pág. 42): la selección única va en azul marino. */
@Composable
private fun DayChip(
    label: String,
    contentDescription: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier
            .aspectRatio(1f)
            .clip(RoundedCornerShape(10.dp))
            .background(if (selected) PocketColors.Navy else pocketColors.Gray.copy(alpha = 0.3f))
            .clickable(role = Role.RadioButton, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            label,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            color = if (selected) Color.White else pocketColors.Muted,
            modifier = Modifier.semantics { this.contentDescription = contentDescription },
        )
    }
}

/** Etiqueta del idioma actual ("" = Automático) en la lengua que se está usando. */
@Composable
fun languageLabel(tag: String): String =
    if (tag.isBlank()) stringResource(R.string.lang_auto)
    else nativeLanguageName(tag)

/** Nombre del idioma [tag] en el idioma actual de la app (§5.2 "Formatos"). */
@Composable
fun nativeLanguageName(tag: String): String {
    val locale = LocalConfiguration.current.locales[0]
    val name = Locale.Builder().setLanguageTag(tag).build().getDisplayLanguage(locale)
    return name.replaceFirstChar { it.titlecase(locale) }
}

@Composable
fun currencyLabelRes(currency: String): Int = when (currency) {
    "USD" -> R.string.currency_usd
    "EUR" -> R.string.currency_eur
    "MXN" -> R.string.currency_mxn
    else -> R.string.currency_cop
}

@Composable
fun dateFormatLabelRes(format: String): Int = when (format) {
    "MM/DD/YYYY" -> R.string.date_option_mdy
    "YYYY-MM-DD" -> R.string.date_option_ymd
    else -> R.string.date_option_dmy
}

private const val ERROR_SHOWN_MS = 3_500L
