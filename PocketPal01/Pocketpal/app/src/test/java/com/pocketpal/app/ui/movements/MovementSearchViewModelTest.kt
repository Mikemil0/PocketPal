package com.pocketpal.app.ui.movements

import com.pocketpal.app.auth.ErrorType
import com.pocketpal.app.data.movement.FakeMovementRepository
import com.pocketpal.app.data.movement.MovementKind
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.LocalDate

/** Flujo 2 · Buscar movimientos: [MovementSearchViewModel] con el repositorio en memoria. */
@OptIn(ExperimentalCoroutinesApi::class)
class MovementSearchViewModelTest {

    private val dispatcher = UnconfinedTestDispatcher()
    private val day = LocalDate.of(2026, 9, 14)
    private lateinit var repository: FakeMovementRepository

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        repository = FakeMovementRepository()
        repository.addCategory("alimentacion", "Alimentación")
        repository.add("Mercado Central", 42_800.0, day, categoryId = "alimentacion")
        repository.add("Supermercado Norte", 76_200.0, day.minusDays(8), categoryId = "alimentacion")
        repository.add("Mercado del Puerto", 18_900.0, day.minusDays(16))
        repository.add("Venta en MercadoLibre", 150_000.0, day.minusDays(2), kind = MovementKind.Income)
        repository.add("Spotify", 24_900.0, day, description = "Plan premium")
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun newVm() = MovementSearchViewModel(repository, debounceMs = 50)

    @Test
    fun `busca en titulo sin importar mayusculas y ordena del mas reciente`() = runTest(dispatcher) {
        val vm = newVm()
        vm.prepare()
        vm.onQueryChange("MERCADO")
        advanceUntilIdle()

        val ui = vm.state.value
        assertTrue(ui.searched)
        assertEquals(
            listOf("Mercado Central", "Venta en MercadoLibre", "Supermercado Norte", "Mercado del Puerto"),
            ui.results.map { it.title },
        )
        assertEquals("Alimentación", ui.categories["alimentacion"]?.name)
    }

    @Test
    fun `tambien busca en la descripcion`() = runTest(dispatcher) {
        val vm = newVm()
        vm.onQueryChange("premium")
        advanceUntilIdle()
        assertEquals(listOf("Spotify"), vm.state.value.results.map { it.title })
    }

    @Test
    fun `los chips filtran gastos e ingresos`() = runTest(dispatcher) {
        val vm = newVm()
        vm.onQueryChange("mercado")
        advanceUntilIdle()
        vm.selectKind(MovementKind.Income)
        advanceUntilIdle()
        assertEquals(listOf("Venta en MercadoLibre"), vm.state.value.results.map { it.title })

        vm.selectKind(MovementKind.Expense)
        advanceUntilIdle()
        assertEquals(3, vm.state.value.results.size)

        vm.selectKind(null)
        advanceUntilIdle()
        assertEquals(4, vm.state.value.results.size)
    }

    @Test
    fun `al escribir rapido solo se consulta una vez`() = runTest(dispatcher) {
        val vm = newVm()
        listOf("me", "mer", "merc", "merca", "mercad", "mercado").forEach(vm::onQueryChange)
        advanceUntilIdle()
        assertEquals(1, repository.searchCalls)
        assertEquals(4, vm.state.value.results.size)
    }

    @Test
    fun `con menos de dos letras no consulta`() = runTest(dispatcher) {
        val vm = newVm()
        vm.onQueryChange("m")
        advanceUntilIdle()
        assertEquals(0, repository.searchCalls)
        assertFalse(vm.state.value.searched)
        assertTrue(vm.state.value.results.isEmpty())
    }

    @Test
    fun `sin resultados queda buscado y vacio`() = runTest(dispatcher) {
        val vm = newVm()
        vm.onQueryChange("gimnasio")
        advanceUntilIdle()
        assertTrue(vm.state.value.searched)
        assertTrue(vm.state.value.results.isEmpty())
    }

    @Test
    fun `limpiar borra el texto y los resultados`() = runTest(dispatcher) {
        val vm = newVm()
        vm.onQueryChange("mercado")
        advanceUntilIdle()
        vm.clear()
        assertEquals("", vm.state.value.query)
        assertTrue(vm.state.value.results.isEmpty())
        assertFalse(vm.state.value.searched)
    }

    @Test
    fun `sin internet avisa y reintentar busca de nuevo`() = runTest(dispatcher) {
        val vm = newVm()
        repository.failNextWithNetwork = true
        vm.onQueryChange("mercado")
        advanceUntilIdle()
        assertEquals(ErrorType.NoNetwork, vm.state.value.error)
        assertFalse(vm.state.value.searching)

        vm.retry()
        advanceUntilIdle()
        assertNull(vm.state.value.error)
        assertEquals(4, vm.state.value.results.size)
    }
}
