package com.pocketpal.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pocketpal.app.R
import com.pocketpal.app.ui.AppLocale
import com.pocketpal.app.ui.theme.PocketColors

/**
 * Botón globo con el código del idioma efectivo (ES/EN…) de la esquina superior derecha
 * del Welcome. Desvío del Figma no diseñado: ver §14.
 */
@Composable
fun LanguageSelectorButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        onClick = onClick,
        modifier = modifier.minimumInteractiveComponentSize(),
        shape = RoundedCornerShape(24.dp),
        color = Color.Transparent,
    ) {
        Row(
            Modifier
                .background(PocketColors.NavyLight, RoundedCornerShape(24.dp))
                .height(40.dp)
                .padding(horizontal = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                Icons.Outlined.Language,
                stringResource(R.string.language_change),
                tint = PocketColors.Coral,
                modifier = Modifier.size(18.dp),
            )
            Spacer(Modifier.width(6.dp))
            Text(
                AppLocale.currentLanguageCode(),
                color = Color.White,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}

/**
 * Hoja inferior con las opciones de idioma del Welcome: Automático (idioma del sistema),
 * Español y English (cada idioma en su propia lengua). Portugués y francés llegan en la Fase 5.
 * Persiste la elección vía [AppLocale] (autoStoreLocales en API ≤ 32; el sistema en 13+).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LanguageBottomSheet(onDismiss: () -> Unit) {
    val sheetState = rememberModalBottomSheetState()
    val saved = AppLocale.savedTag()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = PocketColors.Cream,
    ) {
        LanguageOption(
            title = stringResource(R.string.language_option_system),
            subtitle = stringResource(R.string.language_option_system_subtitle),
            selected = saved == null,
            onClick = { AppLocale.setLanguage(null); onDismiss() },
        )
        LanguageOption(
            title = stringResource(R.string.lang_name_es),
            selected = saved == "es",
            onClick = { AppLocale.setLanguage("es"); onDismiss() },
        )
        LanguageOption(
            title = stringResource(R.string.lang_name_en),
            selected = saved == "en",
            onClick = { AppLocale.setLanguage("en"); onDismiss() },
        )
        Spacer(Modifier.height(12.dp))
    }
}

@Composable
private fun LanguageOption(
    title: String,
    selected: Boolean,
    onClick: () -> Unit,
    subtitle: String? = null,
) {
    Row(
        Modifier.fillMaxWidth()
            .clickable(onClick = onClick)
            .heightIn(min = 56.dp)
            .padding(horizontal = 24.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                title,
                fontSize = 15.sp,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
                color = PocketColors.Ink,
            )
            if (subtitle != null) {
                Spacer(Modifier.height(2.dp))
                Text(subtitle, fontSize = 12.sp, color = PocketColors.Muted)
            }
        }
        if (selected) {
            Icon(Icons.Outlined.Check, null, tint = PocketColors.Coral, modifier = Modifier.size(22.dp))
        }
    }
}
