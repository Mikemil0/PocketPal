package com.pocketpal.app.navigation

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pocketpal.app.R
import com.pocketpal.app.data.category.CategoryKind
import com.pocketpal.app.ui.movements.MovementDetailScreen
import com.pocketpal.app.ui.movements.MovementDetailViewModel
import com.pocketpal.app.ui.movements.MovementReceipt
import com.pocketpal.app.ui.profile.CategoriesViewModel
import com.pocketpal.app.ui.profile.CategoryFormScreen
import com.pocketpal.app.ui.profile.ProfileViewModel
import kotlinx.coroutines.launch

// Pantallas y funciones de apoyo que comparten varios grafos de navegación.

/**
 * Módulo 4 · Flujo 4: pantalla de detalle con sus dos acciones de Android.
 * Compartir abre el menú del sistema; Descargar crea el PDF en segundo plano y avisa
 * al ViewModel si quedó guardado.
 */
@Composable
internal fun MovementDetailRoute(
    vm: MovementDetailViewModel,
    currency: String,
    onBack: () -> Unit,
    onGoHome: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var downloading by remember { mutableStateOf(false) }
    val state by vm.state.collectAsStateWithLifecycle()
    val receipt by vm.receipt.collectAsStateWithLifecycle()
    val chooserTitle = stringResource(R.string.detail_share_chooser)
    MovementDetailScreen(
        state = state,
        currency = currency,
        downloading = downloading,
        receiptResult = receipt,
        onBack = onBack,
        onRetry = vm::refresh,
        onGoHome = onGoHome,
        onShare = { content -> MovementReceipt.share(context, content, chooserTitle) },
        onDownload = { content ->
            if (!downloading) {
                downloading = true
                scope.launch {
                    val saved = MovementReceipt.savePdf(context, content)
                    downloading = false
                    vm.onReceiptResult(saved)
                }
            }
        },
        onReceiptShown = vm::consumeReceipt,
    )
}

/** "Ver video": abre la búsqueda del video recomendado en YouTube (o en el navegador). */
internal fun openVideoSearch(context: Context, query: String) {
    val uri = android.net.Uri.parse("https://www.youtube.com/results?search_query=" + android.net.Uri.encode(query))
    runCatching {
        context.startActivity(android.content.Intent(android.content.Intent.ACTION_VIEW, uri))
    }
}

/** Moneda del perfil para formatear montos (RN-09); COP mientras no cargue. */
@Composable
internal fun profileCurrency(state: ProfileViewModel.UiState): String =
    (state as? ProfileViewModel.UiState.Content)?.data?.currency ?: "COP"

/** Formulario crear/editar: resuelve la fila inicial y las de su tipo (RN-02). */
@Composable
internal fun CategoryFormRoute(
    categoryId: String?,
    requestedKind: CategoryKind,
    categoriesVm: CategoriesViewModel,
    profileVm: ProfileViewModel,
    onBack: () -> Unit,
    onDone: () -> Unit,
) {
    // Carga defensiva: si se entra sin lista (deep link), el formulario la pide.
    LaunchedEffect(Unit) { categoriesVm.refresh() }
    val categoriesState by categoriesVm.state.collectAsStateWithLifecycle()
    val saving by categoriesVm.saving.collectAsStateWithLifecycle()
    val error by categoriesVm.error.collectAsStateWithLifecycle()
    val event by categoriesVm.event.collectAsStateWithLifecycle()
    val profileState by profileVm.state.collectAsStateWithLifecycle()
    val initial = categoriesVm.find(categoryId.orEmpty())
    val kindForList = initial?.kind ?: requestedKind
    val existing = (categoriesState as? CategoriesViewModel.UiState.Content)?.data?.let {
        if (kindForList == CategoryKind.Income) it.incomes else it.expenses
    } ?: emptyList()
    CategoryFormScreen(
        categoryId = categoryId,
        requestedKind = requestedKind,
        initial = initial,
        existing = existing,
        currency = profileCurrency(profileState),
        saving = saving,
        error = error,
        event = event,
        onBack = onBack,
        onSave = categoriesVm::save,
        onDelete = categoriesVm::delete,
        onErrorShown = categoriesVm::consumeError,
        onEventConsumed = categoriesVm::consumeEvent,
        onDone = onDone,
    )
}
