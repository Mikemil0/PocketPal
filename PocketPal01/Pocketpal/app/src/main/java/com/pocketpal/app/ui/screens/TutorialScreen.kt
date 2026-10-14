package com.pocketpal.app.ui.screens

import androidx.annotation.StringRes
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pocketpal.app.R
import com.pocketpal.app.ui.components.PocketPrimaryButton
import com.pocketpal.app.ui.components.ScrollableScreen
import com.pocketpal.app.ui.theme.PocketColors
import com.pocketpal.app.ui.theme.pocketColors

private data class TutPage(@param:StringRes val title: Int, @param:StringRes val body: Int)

private val pages = listOf(
    TutPage(R.string.tutorial_title_1, R.string.tutorial_body_1),
    TutPage(R.string.tutorial_title_2, R.string.tutorial_body_2),
    TutPage(R.string.tutorial_title_3, R.string.tutorial_body_3),
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
                    .background(Brush.verticalGradient(listOf(pocketColors.ChipPink, pocketColors.Cream)))
                    .border(1.dp, pocketColors.Gray, shape),
                contentAlignment = Alignment.Center,
            ) { Illustration(page) }

            Spacer(Modifier.height(32.dp))
            Text(stringResource(pages[page].title), fontSize = 22.sp, fontWeight = FontWeight.SemiBold,
                color = pocketColors.Ink, textAlign = TextAlign.Center)
            Spacer(Modifier.height(8.dp))
            Text(stringResource(pages[page].body), fontSize = 12.sp, color = pocketColors.Muted, textAlign = TextAlign.Center)
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Spacer(Modifier.height(24.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                pages.indices.forEach { i ->
                    Box(Modifier.size(24.dp, 4.dp).background(
                        if (i == page) pocketColors.Coral else pocketColors.Gray, RoundedCornerShape(2.dp)))
                }
            }
            Spacer(Modifier.height(16.dp))
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                if (page > 0) {
                    Box(
                        Modifier.size(52.dp).background(pocketColors.Muted, RoundedCornerShape(12.dp))
                            .clickable { page-- },
                        contentAlignment = Alignment.Center,
                    ) { Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.a11y_previous), tint = Color.White) }
                    Spacer(Modifier.width(12.dp))
                }
                PocketPrimaryButton(
                    if (last) stringResource(R.string.tutorial_start) else stringResource(R.string.tutorial_next),
                    { if (last) onFinish() else page++ },
                    Modifier.weight(1f),
                )
            }
            Spacer(Modifier.height(12.dp))
            if (!last) {
                Text(stringResource(R.string.tutorial_skip), fontSize = 12.sp, color = pocketColors.Muted,
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
                Modifier.fillMaxSize().background(pocketColors.Navy, RoundedCornerShape(14.dp)).padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                listOf(
                    stringResource(R.string.tutorial_food) to pocketColors.Coral,
                    stringResource(R.string.tutorial_transport) to pocketColors.Coral,
                    stringResource(R.string.tutorial_income) to pocketColors.Teal,
                ).forEach { (t, c) ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(8.dp).background(c, CircleShape))
                        Spacer(Modifier.width(8.dp))
                        Text(t, color = Color.White, fontSize = 11.sp)
                    }
                }
            }
            Box(
                Modifier.align(Alignment.BottomEnd).offset(12.dp, 12.dp).size(40.dp).background(pocketColors.Coral, CircleShape),
                contentAlignment = Alignment.Center,
            ) { Text(stringResource(R.string.tutorial_add_symbol), color = Color.White, fontSize = 22.sp) }
        }
        1 -> Column(
            Modifier.width(210.dp).background(pocketColors.Card, RoundedCornerShape(12.dp)).padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(stringResource(R.string.tutorial_budget_header), fontSize = 8.sp, color = pocketColors.Muted)
            listOf(
                stringResource(R.string.tutorial_food) to 0.75f,
                stringResource(R.string.tutorial_transport) to 0.85f,
                stringResource(R.string.tutorial_entertainment) to 0.4f,
            ).forEach { (t, p) ->
                Column {
                    Text(t, fontSize = 9.sp, color = pocketColors.Ink)
                    Spacer(Modifier.height(4.dp))
                    Box(Modifier.fillMaxWidth().height(4.dp).background(pocketColors.Gray.copy(alpha = 0.4f), RoundedCornerShape(2.dp))) {
                        Box(Modifier.fillMaxWidth(p).fillMaxHeight().background(pocketColors.Coral, RoundedCornerShape(2.dp)))
                    }
                }
            }
        }
        else -> Column(
            Modifier.width(210.dp).background(pocketColors.Card, RoundedCornerShape(12.dp)).padding(14.dp),
        ) {
            Text(stringResource(R.string.tutorial_total_month), fontSize = 9.sp, color = pocketColors.Muted)
            Text(stringResource(R.string.tutorial_total_amount), fontSize = 15.sp, fontWeight = FontWeight.Bold, color = pocketColors.Ink)
            Spacer(Modifier.height(10.dp))
            Row(Modifier.fillMaxWidth().height(90.dp), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.Bottom) {
                listOf(pocketColors.Coral to 50, pocketColors.Muted to 28, pocketColors.Teal to 85).forEach { (c, h) ->
                    Box(Modifier.size(28.dp, h.dp).background(c, RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp)))
                }
            }
        }
    }
}
