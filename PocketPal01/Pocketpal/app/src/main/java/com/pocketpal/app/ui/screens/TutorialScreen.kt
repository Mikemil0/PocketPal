package com.pocketpal.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pocketpal.app.ui.components.PocketPrimaryButton
import com.pocketpal.app.ui.components.ScrollableScreen
import com.pocketpal.app.ui.theme.PocketColors

private data class TutPage(val title: String, val body: String)

private val pages = listOf(
    TutPage("Registra cada gasto en segundos",
        "Toca el botón \"+\", elige una categoría y listo. Así de simple es llevar el control de tu dinero."),
    TutPage("Organiza tus límites",
        "Define un presupuesto para cada categoría y te avisamos antes de superarlo."),
    TutPage("Entiende en qué se va tu dinero",
        "Cada gasto que registres se convierte en gráficos claros, para que tomes mejores decisiones cada mes."),
)

@Composable
fun TutorialScreen(onFinish: () -> Unit) {
    var page by rememberSaveable { mutableIntStateOf(0) }
    val last = page == pages.lastIndex
    val shape = RoundedCornerShape(20.dp)

    ScrollableScreen {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Spacer(Modifier.height(24.dp))
            Box(
                Modifier.fillMaxWidth().height(260.dp).clip(shape)
                    .background(Brush.verticalGradient(listOf(Color(0xFFFFE3DD), Color.White)))
                    .border(1.dp, PocketColors.Gray, shape),
                contentAlignment = Alignment.Center,
            ) { Illustration(page) }

            Spacer(Modifier.height(32.dp))
            Text(pages[page].title, fontSize = 22.sp, fontWeight = FontWeight.SemiBold,
                color = PocketColors.Ink, textAlign = TextAlign.Center)
            Spacer(Modifier.height(8.dp))
            Text(pages[page].body, fontSize = 12.sp, color = PocketColors.Muted, textAlign = TextAlign.Center)
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Spacer(Modifier.height(24.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                pages.indices.forEach { i ->
                    Box(Modifier.size(24.dp, 4.dp).background(
                        if (i == page) PocketColors.Coral else PocketColors.Gray, RoundedCornerShape(2.dp)))
                }
            }
            Spacer(Modifier.height(16.dp))
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                if (page > 0) {
                    Box(
                        Modifier.size(52.dp).background(PocketColors.Muted, RoundedCornerShape(12.dp))
                            .clickable { page-- },
                        contentAlignment = Alignment.Center,
                    ) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Anterior", tint = Color.White) }
                    Spacer(Modifier.width(12.dp))
                }
                PocketPrimaryButton(
                    if (last) "Empieza a ahorrar" else "Siguiente",
                    { if (last) onFinish() else page++ },
                    Modifier.weight(1f),
                )
            }
            Spacer(Modifier.height(12.dp))
            if (!last) {
                Text("omitir", fontSize = 12.sp, color = PocketColors.Muted,
                    modifier = Modifier.clickable(onClick = onFinish))
            } else {
                Spacer(Modifier.height(16.dp))
            }
        }
    }
}

@Composable
private fun Illustration(page: Int) {
    when (page) {
        0 -> Box(Modifier.size(210.dp, 130.dp)) {
            Column(
                Modifier.fillMaxSize().background(PocketColors.Navy, RoundedCornerShape(14.dp)).padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                listOf("Comida" to PocketColors.Coral, "Transporte" to PocketColors.Coral, "Sueldo" to PocketColors.Teal)
                    .forEach { (t, c) ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(Modifier.size(8.dp).background(c, CircleShape))
                            Spacer(Modifier.width(8.dp))
                            Text(t, color = Color.White, fontSize = 11.sp)
                        }
                    }
            }
            Box(
                Modifier.align(Alignment.BottomEnd).offset(12.dp, 12.dp).size(40.dp).background(PocketColors.Coral, CircleShape),
                contentAlignment = Alignment.Center,
            ) { Text("+", color = Color.White, fontSize = 22.sp) }
        }
        1 -> Column(
            Modifier.width(210.dp).background(Color.White, RoundedCornerShape(12.dp)).padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text("PRESUPUESTO DE ESTE MES", fontSize = 8.sp, color = PocketColors.Muted)
            listOf("Comida" to 0.75f, "Transporte" to 0.85f, "Entretenimiento" to 0.4f).forEach { (t, p) ->
                Column {
                    Text(t, fontSize = 9.sp, color = PocketColors.Ink)
                    Spacer(Modifier.height(4.dp))
                    Box(Modifier.fillMaxWidth().height(4.dp).background(PocketColors.Gray.copy(alpha = 0.4f), RoundedCornerShape(2.dp))) {
                        Box(Modifier.fillMaxWidth(p).fillMaxHeight().background(PocketColors.Coral, RoundedCornerShape(2.dp)))
                    }
                }
            }
        }
        else -> Column(
            Modifier.width(210.dp).background(Color.White, RoundedCornerShape(12.dp)).padding(14.dp),
        ) {
            Text("Total del mes", fontSize = 9.sp, color = PocketColors.Muted)
            Text("\$1.120.000", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = PocketColors.Ink)
            Spacer(Modifier.height(10.dp))
            Row(Modifier.fillMaxWidth().height(90.dp), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.Bottom) {
                listOf(PocketColors.Coral to 50, PocketColors.Muted to 28, PocketColors.Teal to 85).forEach { (c, h) ->
                    Box(Modifier.size(28.dp, h.dp).background(c, RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp)))
                }
            }
        }
    }
}
