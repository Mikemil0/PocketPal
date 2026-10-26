package com.pocketpal.app.ui.movements

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pocketpal.app.auth.ErrorType
import com.pocketpal.app.data.movement.Movement
import com.pocketpal.app.data.movement.MovementCategory
import com.pocketpal.app.data.movement.MovementKind
import com.pocketpal.app.data.movement.MovementRepository
import com.pocketpal.app.data.movement.MovementRules
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.IOException

/**
 * Flujo 2 · Buscar movimientos (Figma "01_Movimientos por busqueda").
 *
 * - Busca en la base mientras el usuario escribe, pero **espera [debounceMs]** desde la
 *   última tecla (debounce): escribir "mercado" hace UNA consulta, no siete.
 * - Los chips Todos / Gastos / Ingresos filtran por tipo y repiten la búsqueda al instante.
 * - Con menos de 2 letras no se consulta nada.
 */
class MovementSearchViewModel(
    private val repository: MovementRepository,
    private val debounceMs: Long = DEBOUNCE_MS,
) : ViewModel() {

    data class SearchUi(
        val query: String = "",
        /** `null` = "Todos". */
        val kind: MovementKind? = null,
        val results: List<Movement> = emptyList(),
        val categories: Map<String, MovementCategory> = emptyMap(),
        /** Hay una consulta en curso (para mostrar el indicador de carga). */
        val searching: Boolean = false,
        /** Ya se buscó con el texto actual (para decidir entre "3 resultados" y "sin resultados"). */
        val searched: Boolean = false,
        val error: ErrorType? = null,
    ) {
        val canSearch: Boolean get() = MovementRules.canSearch(query)
    }

    private val _state = MutableStateFlow(SearchUi())
    val state: StateFlow<SearchUi> = _state.asStateFlow()

    private var searchJob: Job? = null

    /** Al abrir la pantalla: limpia la búsqueda anterior y trae las categorías (íconos y nombres). */
    fun prepare() {
        searchJob?.cancel()
        _state.value = SearchUi()
        viewModelScope.launch {
            try {
                val categories = repository.categories().associateBy { it.id }
                _state.value = _state.value.copy(categories = categories)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                // Sin categorías la búsqueda igual funciona (las filas salen con el ícono "Otros").
            }
        }
    }

    /** Cada tecla: guarda el texto y programa la búsqueda (se cancela si llega otra tecla antes). */
    fun onQueryChange(query: String) {
        _state.value = _state.value.copy(query = query, error = null)
        scheduleSearch(delayFirst = true)
    }

    /** Chip Todos / Gastos / Ingresos: busca de inmediato con el nuevo filtro. */
    fun selectKind(kind: MovementKind?) {
        _state.value = _state.value.copy(kind = kind)
        scheduleSearch(delayFirst = false)
    }

    /** La "x" del buscador. */
    fun clear() {
        searchJob?.cancel()
        _state.value = _state.value.copy(query = "", results = emptyList(), searching = false, searched = false, error = null)
    }

    /** Botón "Reintentar" cuando falló la red. */
    fun retry() = scheduleSearch(delayFirst = false)

    private fun scheduleSearch(delayFirst: Boolean) {
        searchJob?.cancel()
        val current = _state.value
        if (!current.canSearch) {
            _state.value = current.copy(results = emptyList(), searching = false, searched = false)
            return
        }
        searchJob = viewModelScope.launch {
            if (delayFirst) delay(debounceMs)
            _state.value = _state.value.copy(searching = true)
            val snapshot = _state.value
            _state.value = try {
                val results = repository.search(snapshot.query, snapshot.kind)
                snapshot.copy(results = results, searching = false, searched = true, error = null)
            } catch (e: CancellationException) {
                throw e
            } catch (e: IOException) {
                snapshot.copy(searching = false, error = ErrorType.NoNetwork)
            } catch (e: Exception) {
                snapshot.copy(searching = false, error = ErrorType.Unknown)
            }
        }
    }

    companion object {
        const val DEBOUNCE_MS = 300L
    }
}
