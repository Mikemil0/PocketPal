package com.pocketpal.app.ui.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pocketpal.app.R
import com.pocketpal.app.auth.ErrorType
import com.pocketpal.app.ui.components.*
import com.pocketpal.app.ui.messageRes
import com.pocketpal.app.ui.theme.PocketColors
import kotlinx.coroutines.delay
import java.util.Locale

/**
 * RF-B2: "Datos personales" (págs. 20 y 34). Nombre editable (RN-01), correo solo
 * lectura (D-08: la edición llega en la Fase 8b), fila Teléfono **visible sin flujo**
 * (el SMS es la Fase 7), país y moneda (D-04) y la hoja de foto.
 *
 * Los códigos guardados son neutros (RN-11: `CO`, `COP`…); las etiquetas las forma la UI.
 */
@Composable
fun EditProfileScreen(
    state: ProfileViewModel.UiState,
    error: ErrorType?,
    saving: Boolean,
    event: ProfileViewModel.Event?,
    onBack: () -> Unit,
    onSave: (fullName: String, country: String, currency: String) -> Unit,
    onAvatarBytes: (ByteArray?) -> Unit,
    onRemoveAvatar: () -> Unit,
    onErrorShown: () -> Unit,
    onEventConsumed: () -> Unit,
) {
    val content = (state as? ProfileViewModel.UiState.Content)?.data

    // El formulario se inicializa una vez con el perfil y conserva lo escrito
    // aunque la ViewModel recargue (RNF-05: no se pierde lo escrito).
    var name by rememberSaveable { mutableStateOf("") }
    var country by rememberSaveable { mutableStateOf("CO") }
    var currency by rememberSaveable { mutableStateOf("COP") }
    var initialized by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(content) {
        if (!initialized && content != null) {
            name = content.fullName
            country = content.country
            currency = content.currency
            initialized = true
        }
    }

    val nameValid = name.isNotBlank() && name.trim().length <= ProfileViewModel.MAX_NAME_LENGTH
    val changed = content != null && (
        name.trim() != content.fullName || country != content.country || currency != content.currency
        )

    var shownError by remember { mutableStateOf<ErrorType?>(null) }
    LaunchedEffect(error) {
        if (error != null) {
            shownError = error
            onErrorShown()
            delay(ERROR_SHOWN_MS)
            shownError = null
        }
    }

    // Al guardar se vuelve a Mi perfil (evento consumido una sola vez).
    LaunchedEffect(event) {
        if (event == ProfileViewModel.Event.DataSaved) {
            onEventConsumed()
            onBack()
        }
    }

    val pickers = rememberAvatarPickers(onImage = onAvatarBytes)
    var showPhotoSheet by remember { mutableStateOf(false) }

    AuthScaffold(onBack = onBack) {
        Text(
            stringResource(R.string.edit_title),
            fontSize = 22.sp,
            fontWeight = FontWeight.SemiBold,
            color = PocketColors.Ink,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(16.dp))

        if (shownError != null) {
            ErrorBanner(stringResource(shownError!!.messageRes()))
            Spacer(Modifier.height(12.dp))
        }

        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            Avatar(
                name = content?.fullName.orEmpty(),
                email = content?.email.orEmpty(),
                avatarUrl = content?.avatarUrl,
                size = 110.dp,
                contentDescription = stringResource(R.string.a11y_change_photo),
                onEditClick = { showPhotoSheet = true },
            )
            Spacer(Modifier.height(8.dp))
            Text(
                stringResource(R.string.edit_change_photo),
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = PocketColors.Coral,
                modifier = Modifier.clickable(onClick = { showPhotoSheet = true }),
            )
        }

        Spacer(Modifier.height(24.dp))

        // RN-01: obligatorio y máximo 60 caracteres; con el nombre inválido no se guarda.
        PocketOutlinedTextField(
            value = name,
            onValueChange = { name = it },
            label = stringResource(R.string.edit_field_name),
            isError = name.trim().length > ProfileViewModel.MAX_NAME_LENGTH,
            errorText = if (name.trim().length > ProfileViewModel.MAX_NAME_LENGTH)
                stringResource(R.string.error_name_too_long) else null,
            borderColor = PocketColors.RoseBorder,
        )
        Spacer(Modifier.height(16.dp))
        PocketOutlinedTextField(
            value = content?.email.orEmpty(),
            onValueChange = {},
            label = stringResource(R.string.field_email),
            enabled = false,
            borderColor = PocketColors.RoseBorder,
        )
        Spacer(Modifier.height(16.dp))

        // Teléfono: la fila existe en el Figma, pero su flujo (SMS) llega en la Fase 7.
        Text(
            stringResource(R.string.edit_field_phone),
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            color = PocketColors.Ink,
        )
        Spacer(Modifier.height(6.dp))
        Row(
            Modifier
                .fillMaxWidth()
                .height(56.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(Color.White)
                .border(1.dp, PocketColors.RoseBorder, RoundedCornerShape(12.dp))
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                stringResource(R.string.edit_phone_add),
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                color = PocketColors.Coral,
                modifier = Modifier.weight(1f),
            )
            Icon(Icons.Outlined.ChevronRight, null, tint = PocketColors.Coral, modifier = Modifier.size(22.dp))
        }

        SectionHeader(stringResource(R.string.edit_section_location))

        SelectField(
            label = stringResource(R.string.edit_field_country),
            selectedCode = country,
            options = COUNTRY_CODES.map { it to countryName(it) },
            onSelect = { country = it },
        )
        Spacer(Modifier.height(16.dp))
        SelectField(
            label = stringResource(R.string.edit_field_currency),
            selectedCode = currency,
            options = CURRENCY_CODES.map { it to currencyLabel(it) },
            onSelect = { currency = it },
        )

        Spacer(Modifier.height(32.dp))
        PocketPrimaryButton(
            text = stringResource(R.string.edit_save),
            onClick = { onSave(name, country, currency) },
            enabled = changed && nameValid && !saving,
            loading = saving,
        )
        Spacer(Modifier.height(24.dp))
    }

    if (showPhotoSheet) {
        PhotoActionSheet(
            hasPhoto = content?.avatarUrl != null,
            onTakePhoto = pickers.takePhoto,
            onPickFromGallery = pickers.pickFromGallery,
            onRemovePhoto = onRemoveAvatar,
            onDismiss = { showPhotoSheet = false },
        )
    }
}

/** Campo de selección única (pág. 34: campo redondeado con chevrón en caja rosada). */
@Composable
private fun SelectField(
    label: String,
    selectedCode: String,
    options: List<Pair<String, String>>,
    onSelect: (String) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    val selectedLabel = options.firstOrNull { it.first == selectedCode }?.second ?: selectedCode

    Column(Modifier.fillMaxWidth()) {
        Text(label, fontSize = 13.sp, fontWeight = FontWeight.Medium, color = PocketColors.Ink)
        Spacer(Modifier.height(6.dp))
        Box(Modifier.fillMaxWidth()) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.White)
                    .border(1.dp, PocketColors.RoseBorder, RoundedCornerShape(12.dp))
                    .clickable { expanded = true }
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(selectedLabel, fontSize = 15.sp, color = PocketColors.Muted, modifier = Modifier.weight(1f))
                Box(
                    Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(PocketColors.Rose),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Outlined.ExpandMore, null, tint = PocketColors.Coral, modifier = Modifier.size(20.dp))
                }
            }
            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                options.forEach { (code, optionLabel) ->
                    DropdownMenuItem(
                        text = {
                            Text(optionLabel, fontWeight = if (code == selectedCode) FontWeight.SemiBold else FontWeight.Normal)
                        },
                        onClick = { onSelect(code); expanded = false },
                    )
                }
            }
        }
    }
}

// D-04 / §14 fila 5: Colombia, Perú, México y Bolivia (los prefijos corregidos
// se usan en el flujo de teléfono, Fase 7).
private val COUNTRY_CODES = listOf("CO", "PE", "MX", "BO")

// D-04: lista simple de monedas. El valor guardado es el código (RN-11).
private val CURRENCY_CODES = listOf("COP", "USD", "EUR", "MXN")

/**
 * Nombre del país en el idioma actual (§5.2, "Formatos"). El locale se lee de
 * [LocalConfiguration] (observable) para que el texto cambie al cambiar de idioma.
 */
@Composable
private fun countryName(code: String): String =
    Locale.Builder().setRegion(code).build()
        .getDisplayCountry(LocalConfiguration.current.locales.get(0))

/** Etiqueta de moneda desde resources (RF-A3: ningún texto visible en el código). */
@Composable
private fun currencyLabel(code: String): String = stringResource(
    when (code) {
        "USD" -> R.string.currency_usd
        "EUR" -> R.string.currency_eur
        "MXN" -> R.string.currency_mxn
        else -> R.string.currency_cop
    }
)

private const val ERROR_SHOWN_MS = 3_500L
