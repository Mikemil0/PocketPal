package com.pocketpal.app.ui.profile

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.MailOutline
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
import com.pocketpal.app.R
import com.pocketpal.app.ui.components.AuthScaffold
import com.pocketpal.app.ui.components.PocketOutlinedTextField
import com.pocketpal.app.ui.components.SectionHeader
import com.pocketpal.app.ui.components.SettingsGroup
import com.pocketpal.app.ui.theme.PocketColors
import com.pocketpal.app.ui.theme.pocketColors

/** Pregunta resuelta en el idioma actual (la búsqueda filtra sobre este texto). */
private data class ResolvedFaq(val index: Int, val question: String, val answer: String)

/**
 * "Ayuda y soporte" (pág. 40): buscador, preguntas frecuentes **desplegables**
 * (acordeón: tocar la pregunta la expande o colapsa; la respuesta abierta abre el
 * detalle con Anterior/Siguiente de la pág. 57, decisión A de la Fase 8) y las
 * tarjetas de contacto.
 *
 * - "Chat en vivo": tarjeta deshabilitada con "Próximamente" (D-06).
 * - "Escríbenos": abre la app de correo hacia el correo de soporte (SUPPORT_EMAIL).
 */
@Composable
fun HelpScreen(
    supportEmail: String,
    onBack: () -> Unit,
    onOpenDetail: (Int) -> Unit,
) {
    val context = LocalContext.current
    val comingSoonText = stringResource(R.string.profile_coming_soon)
    val emailSubject = stringResource(R.string.help_email_subject)
    var query by remember { mutableStateOf("") }
    // La pág. 40 del Figma muestra la primera pregunta abierta (§2: el diseño manda).
    var expanded by remember { mutableStateOf(0) }

    val all = ArrayList<ResolvedFaq>(FaqCatalog.items.size)
    for ((i, item) in FaqCatalog.items.withIndex()) {
        all.add(ResolvedFaq(i, stringResource(item.question), stringResource(item.answer, supportEmail)))
    }
    val filtered = FaqCatalog.filter(all, query) { it.question + " " + it.answer }
    val openDetailLabel = stringResource(R.string.help_open_detail)

    AuthScaffold(
        onBack = onBack,
        backBackground = pocketColors.ChipPink,
        backTint = pocketColors.Coral,
    ) {
        Text(
            stringResource(R.string.help_title),
            fontSize = 22.sp,
            fontWeight = FontWeight.SemiBold,
            color = pocketColors.Ink,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(16.dp))

        PocketOutlinedTextField(
            value = query,
            onValueChange = { query = it },
            label = "",
            placeholder = stringResource(R.string.help_search_hint),
            leadingIcon = Icons.Outlined.Search,
            imeAction = androidx.compose.ui.text.input.ImeAction.Search,
            borderColor = pocketColors.RoseBorder,
            shape = RoundedCornerShape(28.dp),
        )

        SectionHeader(stringResource(R.string.help_section_faqs))
        if (filtered.isEmpty()) {
            Text(
                stringResource(R.string.help_no_results),
                fontSize = 13.sp,
                color = pocketColors.Muted,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
            )
        } else {
            SettingsGroup {
                filtered.forEachIndexed { i, faq ->
                    if (i > 0) {
                        Box(
                            Modifier
                                .fillMaxWidth()
                                .height(1.dp)
                                .background(pocketColors.RoseBorder.copy(alpha = 0.6f))
                        )
                    }
                    val isExpanded = expanded == faq.index
                    Column(Modifier.fillMaxWidth()) {
                        Text(
                            faq.question,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = pocketColors.Ink,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    expanded = if (isExpanded) -1 else faq.index
                                }
                                .padding(horizontal = 16.dp, vertical = 14.dp),
                        )
                        if (isExpanded) {
                            Text(
                                faq.answer,
                                fontSize = 12.sp,
                                color = pocketColors.Muted,
                                lineHeight = 17.sp,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable(onClickLabel = openDetailLabel) {
                                        onOpenDetail(faq.index)
                                    }
                                    .padding(start = 16.dp, end = 16.dp, bottom = 14.dp),
                            )
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(28.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            ContactCard(
                title = stringResource(R.string.help_chat_title),
                subtitle = stringResource(R.string.help_chat_subtitle),
                icon = Icons.Outlined.ChatBubbleOutline,
                iconBackground = pocketColors.TealSoft,
                iconTint = pocketColors.Teal,
                enabled = false,
                modifier = Modifier.weight(1f),
                onClick = { Toast.makeText(context, comingSoonText, Toast.LENGTH_SHORT).show() },
            )
            ContactCard(
                title = stringResource(R.string.help_email_title),
                subtitle = supportEmail,
                icon = Icons.Outlined.MailOutline,
                iconBackground = pocketColors.Rose,
                iconTint = pocketColors.Coral,
                enabled = true,
                modifier = Modifier.weight(1f),
                onClick = {
                    if (supportEmail.isBlank()) {
                        Toast.makeText(context, comingSoonText, Toast.LENGTH_SHORT).show()
                    } else {
                        val intent = android.content.Intent(
                            android.content.Intent.ACTION_SENDTO,
                            "mailto:$supportEmail".toUri(),
                        ).putExtra(
                            android.content.Intent.EXTRA_SUBJECT,
                            emailSubject,
                        )
                        runCatching { context.startActivity(intent) }.onFailure {
                            Toast.makeText(context, comingSoonText, Toast.LENGTH_SHORT).show()
                        }
                    }
                },
            )
        }
        Spacer(Modifier.height(24.dp))
    }
}

/**
 * Tarjeta de contacto (pág. 40). Con [enabled] = false se ve deshabilitada y
 * muestra la insignia "Próximamente" (D-06).
 */
@Composable
private fun ContactCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    iconBackground: Color,
    iconTint: Color,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier
            .clip(RoundedCornerShape(16.dp))
            .background(pocketColors.Card)
            .border(1.dp, pocketColors.RoseBorder, RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 16.dp, horizontal = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Column(
            Modifier.alpha(if (enabled) 1f else 0.55f),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(
                Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(iconBackground),
                contentAlignment = Alignment.Center,
            ) {
                Icon(icon, null, tint = iconTint, modifier = Modifier.size(22.dp))
            }
            Spacer(Modifier.height(10.dp))
            Text(title, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = pocketColors.Ink)
            Spacer(Modifier.height(2.dp))
            Text(
                subtitle,
                fontSize = 11.sp,
                color = pocketColors.Muted,
                textAlign = TextAlign.Center,
                maxLines = 2,
            )
        }
        if (!enabled) {
            Spacer(Modifier.height(8.dp))
            Box(
                Modifier
                    .clip(RoundedCornerShape(50))
                    .background(pocketColors.Rose)
                    .padding(horizontal = 10.dp, vertical = 3.dp),
            ) {
                Text(
                    stringResource(R.string.profile_coming_soon),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = pocketColors.Coral,
                )
            }
        }
    }
}
