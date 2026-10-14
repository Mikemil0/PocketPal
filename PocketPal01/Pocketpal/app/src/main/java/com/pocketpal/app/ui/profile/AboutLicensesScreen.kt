package com.pocketpal.app.ui.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pocketpal.app.R
import com.pocketpal.app.ui.components.AuthScaffold
import com.pocketpal.app.ui.components.SettingsGroup
import com.pocketpal.app.ui.theme.PocketColors
import com.pocketpal.app.ui.theme.pocketColors

/**
 * "Licencias de código abierto" (fila de la pág. 41; sin pantalla en el Figma).
 * Decisión de la Fase 8: pantalla propia que lista las librerías del proyecto en
 * vez de la librería oficial de licencias de Google (evita una dependencia nueva
 * y todo el contenido pasa por RF-A3). Los nombres propios no se traducen.
 */
@Composable
fun AboutLicensesScreen(onBack: () -> Unit) {
    val libraries = listOf(
        R.string.oss_kotlin,
        R.string.oss_compose,
        R.string.oss_navigation,
        R.string.oss_appcompat,
        R.string.oss_datastore,
        R.string.oss_splashscreen,
        R.string.oss_exif,
        R.string.oss_icons,
        R.string.oss_supabase,
        R.string.oss_ktor,
        R.string.oss_coil,
        R.string.oss_reorderable,
        R.string.oss_credentials,
    )

    AuthScaffold(
        onBack = onBack,
        backBackground = pocketColors.ChipPink,
        backTint = pocketColors.Coral,
    ) {
        Text(
            stringResource(R.string.about_licenses_title),
            fontSize = 22.sp,
            fontWeight = FontWeight.SemiBold,
            color = pocketColors.Ink,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(12.dp))
        Text(
            stringResource(R.string.about_licenses_intro),
            fontSize = 13.sp,
            color = pocketColors.Muted,
            textAlign = TextAlign.Center,
            lineHeight = 18.sp,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(16.dp))
        SettingsGroup {
            libraries.forEachIndexed { i, res ->
                if (i > 0) {
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .height(1.dp)
                            .background(pocketColors.RoseBorder.copy(alpha = 0.6f))
                    )
                }
                Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp)) {
                    Text(
                        stringResource(res),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = pocketColors.Ink,
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        stringResource(R.string.about_licenses_apache),
                        fontSize = 11.sp,
                        color = pocketColors.Muted,
                    )
                }
            }
        }
        Spacer(Modifier.height(24.dp))
    }
}
