package com.pocketpal.app.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pocketpal.app.R
import com.pocketpal.app.ui.components.*
import com.pocketpal.app.ui.theme.PocketColors
import com.pocketpal.app.ui.theme.pocketColors

@Composable
fun WelcomeScreen(onGoogle: () -> Unit, onRegister: () -> Unit, onLogin: () -> Unit) {
    var showLanguageSheet by rememberSaveable { mutableStateOf(false) }

    Box(Modifier.fillMaxSize()) {
        ScrollableScreen(background = pocketColors.Navy) {
            // Spacer vacío arriba: con SpaceBetween deja el logo centrado en el espacio sobre los botones.
            Spacer(Modifier.height(0.dp))

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Image(
                    painter = painterResource(R.drawable.logo_pocketpal),
                    contentDescription = stringResource(R.string.brand_name),
                    modifier = Modifier.size(150.dp),
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    buildAnnotatedString {
                        withStyle(SpanStyle(color = Color.White)) { append(stringResource(R.string.brand_pocket)) }
                        withStyle(SpanStyle(color = pocketColors.Coral)) { append(stringResource(R.string.brand_pal)) }
                    },
                    fontSize = 44.sp,
                    fontWeight = FontWeight.Bold,
                )
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Spacer(Modifier.height(24.dp)) // separación mínima cuando hay que hacer scroll
                PocketGoogleButton(stringResource(R.string.welcome_google), onGoogle)
                Spacer(Modifier.height(12.dp))
                Text(stringResource(R.string.common_or), color = pocketColors.Muted, fontSize = 12.sp)
                Spacer(Modifier.height(12.dp))
                PocketPrimaryButton(stringResource(R.string.welcome_create_account), onRegister)
                Spacer(Modifier.height(10.dp))
                PocketSecondaryButton(stringResource(R.string.welcome_have_account), onLogin)
                Spacer(Modifier.height(16.dp))
                Text(
                    stringResource(R.string.welcome_terms),
                    color = pocketColors.Muted, fontSize = 11.sp, textAlign = TextAlign.Center,
                )
            }
        }

        // Selector de idioma (desvío del Figma, §14): solo cambia este botón y su hoja.
        LanguageSelectorButton(
            onClick = { showLanguageSheet = true },
            modifier = Modifier.align(Alignment.TopEnd)
                .statusBarsPadding()
                .padding(end = 16.dp, top = 8.dp),
        )
    }

    if (showLanguageSheet) {
        LanguageBottomSheet(onDismiss = { showLanguageSheet = false })
    }
}
