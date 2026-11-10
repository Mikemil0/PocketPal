package com.pocketpal.app.ui.movements

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pocketpal.app.auth.ErrorType
import com.pocketpal.app.data.movement.DaiInsight
import com.pocketpal.app.data.movement.Movement
import com.pocketpal.app.data.movement.MovementCategory
import com.pocketpal.app.data.movement.MovementRepository
import com.pocketpal.app.data.movement.MovementRules
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.IOException
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId

/**
 * Flujo 4 · Detalle del movimiento (Figma "03_detalle_movimiento" y
 * "04_movimiento_recurrente").
 *
 * Carga el movimiento, su categoría, el mensaje de "Dai te ayuda" (cuánto pesa dentro
 * de su mes) y, si es recurrente, el próximo cobro. Compartir y descargar los hace la
 * pantalla con [ReceiptContent]; aquí solo se guarda si el comprobante salió bien.
 */
class MovementDetailViewModel(
    private val repository: MovementRepository,
    private val today: () -> LocalDate = { LocalDate.now() },
    private val zone: () -> ZoneId = { ZoneId.systemDefault() },
) : ViewModel() {

    data class DetailUi(
        val movement: Movement,
        val category: MovementCategory?,
        val insight: DaiInsight?,
        /** Solo para recurrentes: la próxima fecha de cobro. */
        val nextCharge: LocalDate?,
    )

    sealed interface UiState {
        data object Loading : UiState
        data class Content(val data: DetailUi) : UiState
        /** El movimiento ya no existe (por ejemplo, se borró desde otro dispositivo). */
        data object NotFound : UiState
        data class Error(val type: ErrorType) : UiState
    }

    /** Resultado de "Descargar" para avisar con un mensaje corto. */
    enum class ReceiptResult { Saved, Failed }

    private val _state = MutableStateFlow<UiState>(UiState.Loading)
    val state: StateFlow<UiState> = _state.asStateFlow()

    private val _receipt = MutableStateFlow<ReceiptResult?>(null)
    val receipt: StateFlow<ReceiptResult?> = _receipt.asStateFlow()

    private var movementId: String? = null

    private val _deleting = MutableStateFlow(false)
    /** `true` mientras se borra el movimiento. */
    val deleting: StateFlow<Boolean> = _deleting.asStateFlow()

    private val _deleted = MutableStateFlow(false)
    /** `true` = ya se borró: la pantalla vuelve atrás y lo consume. */
    val deleted: StateFlow<Boolean> = _deleted.asStateFlow()

    private val _deleteError = MutableStateFlow<ErrorType?>(null)
    /** No se pudo borrar (sin internet u otro error); la pantalla lo muestra y lo consume. */
    val deleteError: StateFlow<ErrorType?> = _deleteError.asStateFlow()

    /** Abre el movimiento [id]. Si es otro distinto al anterior, muestra "cargando". */
    fun load(id: String) {
        if (id != movementId) {
            movementId = id
            _state.value = UiState.Loading
        }
        refresh()
    }

    fun refresh() {
        val id = movementId ?: return
        viewModelScope.launch {
            _state.value = try {
                val movement = repository.get(id)
                if (movement == null) UiState.NotFound else UiState.Content(build(movement))
            } catch (e: CancellationException) {
                throw e
            } catch (e: IOException) {
                UiState.Error(ErrorType.NoNetwork)
            } catch (e: Exception) {
                UiState.Error(ErrorType.Unknown)
            }
        }
    }

    /** La pantalla avisa si el PDF se guardó o no; el mensaje se muestra una vez. */
    fun onReceiptResult(saved: Boolean) {
        _receipt.value = if (saved) ReceiptResult.Saved else ReceiptResult.Failed
    }

    fun consumeReceipt() { _receipt.value = null }

    /** "Eliminar movimiento" (después de confirmar en el diálogo). */
    fun delete() {
        val id = movementId ?: return
        if (_deleting.value) return
        _deleting.value = true
        viewModelScope.launch {
            try {
                repository.delete(id)
                movementId = null
                _deleted.value = true
            } catch (e: CancellationException) {
                throw e
            } catch (e: IOException) {
                _deleteError.value = ErrorType.NoNetwork
            } catch (e: Exception) {
                _deleteError.value = ErrorType.Unknown
            } finally {
                _deleting.value = false
            }
        }
    }

    fun consumeDeleted() { _deleted.value = false }

    fun consumeDeleteError() { _deleteError.value = null }

    private suspend fun build(movement: Movement): DetailUi {
        val zone = zone()
        val category = movement.categoryId?.let { id -> repository.categories().firstOrNull { it.id == id } }
        // "Dai te ayuda" compara con el mes en que ocurrió el movimiento (no con el mes actual).
        val month = YearMonth.from(movement.localDate(zone) ?: today())
        val (from, to) = MovementRules.toInstants(MovementRules.monthRange(month), zone)
        val monthMovements = repository.list(from, to)
        return DetailUi(
            movement = movement,
            category = category,
            insight = MovementRules.insight(movement, category, monthMovements),
            nextCharge = MovementRules.nextCharge(movement, today(), zone),
        )
    }
}
