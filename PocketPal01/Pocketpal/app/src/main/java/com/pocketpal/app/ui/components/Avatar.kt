package com.pocketpal.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.pocketpal.app.R
import com.pocketpal.app.ui.theme.PocketColors

/**
 * Avatar circular (RF-A1 / RF-B2): la foto si existe; si no, un círculo con la
 * inicial del nombre en mayúscula (o la del correo si no hay nombre).
 *
 * Con [onEditClick] muestra la insignia de lápiz oscura de la pág. 52 del Figma
 * (§8.2) y ese área se vuelve pulsable.
 */
@Composable
fun Avatar(
    name: String,
    email: String,
    avatarUrl: String?,
    size: Dp,
    modifier: Modifier = Modifier,
    contentDescription: String? = null,
    onEditClick: (() -> Unit)? = null,
) {
    val initial = remember(name, email) { initialOf(name, email) }
    val badgeSize = size * 0.28f

    Box(modifier.size(size)) {
        if (avatarUrl != null) {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(avatarUrl)
                    .crossfade(true)
                    .build(),
                contentDescription = contentDescription,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .clip(CircleShape)
                    .background(PocketColors.AvatarPink),
            )
        } else {
            Box(
                Modifier
                    .fillMaxSize()
                    .clip(CircleShape)
                    .background(PocketColors.AvatarPink),
                contentAlignment = Alignment.Center,
            ) {
                if (initial != null) {
                    Text(
                        initial,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = (size.value * 0.38f).sp,
                    )
                } else {
                    Icon(
                        Icons.Outlined.Person,
                        contentDescription,
                        tint = Color.White,
                        modifier = Modifier.size(size * 0.55f),
                    )
                }
            }
        }

        if (onEditClick != null) {
            Box(
                Modifier
                    .align(Alignment.BottomEnd)
                    .size(badgeSize.coerceAtLeast(18.dp))
                    .clip(CircleShape)
                    .background(PocketColors.Ink)
                    .border(2.dp, PocketColors.Cream, CircleShape)
                    .clickable(onClick = onEditClick),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Outlined.Edit,
                    stringResource(R.string.a11y_change_photo),
                    tint = Color.White,
                    modifier = Modifier.size(badgeSize * 0.55f),
                )
            }
        }
    }
}

/** Inicial del nombre (si no, la del correo); `null` si no hay ninguno. */
internal fun initialOf(name: String, email: String): String? {
    val source = name.trim().ifBlank { email.trim().substringBefore("@") }
    return source.firstOrNull()?.uppercaseChar()?.toString()
}
