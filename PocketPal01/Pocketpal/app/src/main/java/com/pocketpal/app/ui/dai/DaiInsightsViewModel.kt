package com.pocketpal.app.ui.dai

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pocketpal.app.auth.ErrorType
import com.pocketpal.app.data.dai.DaiBrain
import com.pocketpal.app.data.dai.DaiCard
import com.pocketpal.app.data.dai.DaiReply
import com.pocketpal.app.data.dai.DaiRepository
import com.pocketpal.app.data.dai.DaiTopic
import com.pocketpal.app.data.dai.GoalShare
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.IOException
import java.time.LocalDate
import java.time.ZoneId

/**
 * Flujo 2 · Las pantallas del menú ⋮ de Dai: "Recomendaciones de Dai" (alertas),
 * "Tus metas", "Aprende con Dai" y "Todavía no tengo suficientes datos".
 *
 * Lee UNA vez los datos del usuario ([DaiRepository.snapshot]) y con [DaiBrain] calcula
 * todo lo que muestran esas pantallas. No guarda nada en el chat: si el usuario quiere
 * preguntar algo, la pregunta se manda al chat (flujo 1).
 */
class DaiInsightsViewModel(
    private val repository: DaiRepository,
    private val userName: () -> String = { "" },
    private val currency: () -> String = { "COP" },
    private val weekStart: () -> Int = { 1 },
    private val today: () -> LocalDate = { LocalDate.now() },
    private val zone: () -> ZoneId = { ZoneId.systemDefault() },
) : ViewModel() {

    data class InsightsUi(
        /** `false` = no hay gastos este mes: se muestra "Todavía no tengo suficientes datos". */
        val hasData: Boolean,
        val missing: DaiCard.MissingData,
        /** "Alerta de gasto" de los últimos días (`null` = todo normal). */
        val alert: DaiCard.Alert?,
        /** Texto de la alerta como lo diría Dai. */
        val alertText: String,
        /** "Consejo de hoy". */
        val tip: String,
        val goals: List<GoalShare>,
        /** "¡Vas muy bien! Tu meta Universidad ya va en 62 %…" */
        val goalsMessage: String,
        /** Video para "También te puede interesar" (según lo que más pesa en tus gastos). */
        val suggestedLesson: DaiCard.Lesson,
    )

    sealed interface UiState {
        data object Loading : UiState
        data class Content(val data: InsightsUi) : UiState
        data class Error(val type: ErrorType) : UiState
    }

    private val _state = MutableStateFlow<UiState>(UiState.Loading)
    val state: StateFlow<UiState> = _state.asStateFlow()

    /** Tema elegido en "Aprende con Dai" (empieza en interés compuesto, como el Figma). */
    private val _topic = MutableStateFlow(DaiTopic.CompoundInterest)
    val topic: StateFlow<DaiTopic> = _topic.asStateFlow()

    /** La lección del tema elegido. */
    fun lesson(): DaiReply = DaiBrain.lesson(_topic.value)

    fun selectTopic(topic: DaiTopic) {
        _topic.value = topic
    }

    fun load() {
        if (_state.value !is UiState.Content) _state.value = UiState.Loading
        viewModelScope.launch {
            try {
                val tz = zone()
                val money = currency()
                val data = repository.snapshot(today(), tz, weekStart(), userName())
                val missing = DaiBrain.missingData(data).card as DaiCard.MissingData
                val alertReply = DaiBrain.alerts(data, tz, money)
                _state.value = UiState.Content(
                    InsightsUi(
                        hasData = DaiBrain.hasSpendingData(data),
                        missing = missing,
                        alert = alertReply.card as? DaiCard.Alert,
                        alertText = alertReply.text,
                        tip = DaiBrain.advice(data, money).text,
                        goals = DaiBrain.goalShares(data),
                        goalsMessage = DaiBrain.goals(data, money).text,
                        suggestedLesson = suggestedLesson(data.budgets.any { it.exceeded }),
                    )
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: IOException) {
                _state.value = UiState.Error(ErrorType.NoNetwork)
            } catch (e: Exception) {
                _state.value = UiState.Error(ErrorType.Unknown)
            }
        }
    }

    /** Si ya te pasaste de algún presupuesto, el video es de gastos hormiga; si no, de ahorro. */
    private fun suggestedLesson(overBudget: Boolean): DaiCard.Lesson =
        DaiBrain.lesson(if (overBudget) DaiTopic.AntExpenses else DaiTopic.Saving).card as DaiCard.Lesson

    companion object {
        /** Lo que Dai responde en el chat cuando se toca "¿Quieres que te explique por qué…?". */
        fun explainPrompt(category: String) = "¿Por qué estoy gastando tanto en $category?"
        const val ALTERNATIVES_PROMPT = "Dame un consejo"
        const val SAVE_FASTER_PROMPT = "¿Cómo ahorro más rápido?"
        const val RECOMMENDATION_PROMPT = "¿Alguna recomendación?"
    }
}
