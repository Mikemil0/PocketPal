package com.pocketpal.app.ui.profile

import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Mood
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
import com.pocketpal.app.R
import com.pocketpal.app.ui.components.AuthScaffold
import com.pocketpal.app.ui.components.SectionHeader
import com.pocketpal.app.ui.components.SettingsGroup
import com.pocketpal.app.ui.components.SettingsRow
import com.pocketpal.app.ui.theme.PocketColors
import com.pocketpal.app.ui.theme.pocketColors

/**
 * "Acerca de" (pág. 41): logo, nombre y versión (leída de la app, RF-B11), filas
 * legales, "Recomienda PocketPal" (hoja de compartir de Android) y los íconos de
 * redes.
 *
 * Regla de la Fase 8: **no se inventan URLs**. Toda fila o ícono cuyo destino no
 * sea real (campo vacío en `local.properties`, o sin ficha publicada en el caso de
 * la tienda) muestra el aviso "Próximamente"; si el campo tiene una URL real, se
 * abre en el navegador.
 */
@Composable
fun AboutScreen(
    version: String,
    termsUrl: String,
    privacyUrl: String,
    facebookUrl: String,
    instagramUrl: String,
    xUrl: String,
    onBack: () -> Unit,
    onOpenLicenses: () -> Unit,
) {
    val context = LocalContext.current
    val comingSoonText = stringResource(R.string.profile_coming_soon)
    val shareText = stringResource(R.string.about_share_text)
    val soon = { Toast.makeText(context, comingSoonText, Toast.LENGTH_SHORT).show() }

    fun openUrlOrSoon(url: String) {
        if (url.isBlank()) {
            soon()
            return
        }
        runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, url.toUri())) }
            .onFailure { soon() }
    }

    AuthScaffold(
        onBack = onBack,
        backBackground = pocketColors.ChipPink,
        backTint = pocketColors.Coral,
    ) {
        Text(
            stringResource(R.string.about_title),
            fontSize = 22.sp,
            fontWeight = FontWeight.SemiBold,
            color = pocketColors.Ink,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(16.dp))
        Column(
            Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Image(
                painterResource(R.drawable.logo_pocketpal),
                contentDescription = stringResource(R.string.a11y_about_logo),
                modifier = Modifier.size(88.dp),
            )
            Spacer(Modifier.height(8.dp))
            Text(
                stringResource(R.string.brand_name),
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold,
                color = pocketColors.Ink,
            )
            Spacer(Modifier.height(2.dp))
            Text(
                stringResource(R.string.about_version, version),
                fontSize = 12.sp,
                color = pocketColors.Muted,
            )
        }

        SectionHeader(stringResource(R.string.about_section_legal))
        SettingsGroup {
            SettingsRow(
                icon = Icons.Outlined.Description,
                iconBackground = pocketColors.Rose,
                iconTint = pocketColors.Coral,
                title = stringResource(R.string.about_terms),
                onClick = { openUrlOrSoon(termsUrl) },
            )
            SettingsRow(
                icon = Icons.Outlined.Lock,
                iconBackground = pocketColors.BlueSoft,
                iconTint = pocketColors.BlueIcon,
                title = stringResource(R.string.about_privacy),
                dividerAbove = true,
                onClick = { openUrlOrSoon(privacyUrl) },
            )
            SettingsRow(
                icon = Icons.Outlined.Mood,
                iconBackground = pocketColors.TealSoft,
                iconTint = pocketColors.Teal,
                title = stringResource(R.string.about_licenses),
                dividerAbove = true,
                onClick = onOpenLicenses,
            )
        }

        SectionHeader(stringResource(R.string.about_section_share))
        SettingsGroup {
            SettingsRow(
                icon = Icons.Outlined.Share,
                iconBackground = pocketColors.Rose,
                iconTint = pocketColors.Coral,
                title = stringResource(R.string.about_share_row),
                onClick = {
                    val send = Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_TEXT, shareText)
                    }
                    runCatching {
                        context.startActivity(Intent.createChooser(send, null))
                    }.onFailure { soon() }
                },
            )
            SettingsRow(
                icon = Icons.Outlined.StarBorder,
                iconBackground = pocketColors.BlueSoft,
                iconTint = pocketColors.BlueIcon,
                title = stringResource(R.string.about_rate_row),
                dividerAbove = true,
                onClick = soon, // sin ficha publicada en la tienda: aviso (§8.14)
            )
        }

        Spacer(Modifier.height(24.dp))
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally),
        ) {
            SocialButton(R.drawable.ic_social_facebook, stringResource(R.string.a11y_facebook)) {
                openUrlOrSoon(facebookUrl)
            }
            SocialButton(R.drawable.ic_social_instagram, stringResource(R.string.a11y_instagram)) {
                openUrlOrSoon(instagramUrl)
            }
            SocialButton(R.drawable.ic_social_x, stringResource(R.string.a11y_x)) {
                openUrlOrSoon(xUrl)
            }
        }

        Spacer(Modifier.height(20.dp))
        Text(
            stringResource(R.string.about_footer),
            fontSize = 11.sp,
            color = pocketColors.Muted,
            textAlign = TextAlign.Center,
            lineHeight = 15.sp,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(16.dp))
    }
}

/** Botón cuadrado de red social (pág. 41); área táctil de 48 dp (RNF-07). */
@Composable
private fun SocialButton(iconRes: Int, contentDescription: String, onClick: () -> Unit) {
    Box(
        Modifier
            .size(48.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(pocketColors.Card)
            .border(1.dp, pocketColors.Gray, RoundedCornerShape(12.dp))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Image(
            painterResource(iconRes),
            contentDescription = contentDescription,
            modifier = Modifier.size(22.dp),
            colorFilter = ColorFilter.tint(pocketColors.Ink),
        )
    }
}
