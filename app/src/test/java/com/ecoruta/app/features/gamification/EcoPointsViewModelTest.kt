package com.ecoruta.app.features.gamification.presentation.benefits

// Pruebas unitarias para EcoPointsViewModel (BenefitsViewModel)
// Valida carga de saldo, historial de movimientos, catálogo, canje y manejo de errores

import com.ecoruta.app.core.testing.FakeGamificationRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class EcoPointsViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private val testScope = TestScope(testDispatcher)

    private lateinit var repository: FakeGamificationRepository
    private lateinit var viewModel: BenefitsViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        repository = FakeGamificationRepository()
        viewModel = BenefitsViewModel(repository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    // Valida que el saldo de EcoPuntos se exponga correctamente en la vista de beneficios
    @Test
    fun testLoadBalanceExposesCorrectBalance() = testScope.runTest {
        advanceUntilIdle()

        val state = viewModel.benefitsState.value
        assertFalse(state.isLoading)
        assertEquals(1_250, state.balance)
        assertNull(state.error)
    }

    // Valida que el catálogo de cupones disponibles se cargue correctamente
    @Test
    fun testLoadBenefitsExposesCoupons() = testScope.runTest {
        advanceUntilIdle()

        val state = viewModel.benefitsState.value
        assertFalse(state.isLoading)
        assertTrue(state.coupons.isNotEmpty())
        assertEquals(2, state.coupons.size)

        val firstCoupon = state.coupons.first()
        assertEquals("CUP-01", firstCoupon.id)
        assertEquals("Café del Río", firstCoupon.businessName)
        assertEquals(450, firstCoupon.pointsCost)
    }

    // Valida que el historial de movimientos de EcoPuntos se cargue con los datos correctos
    @Test
    fun testLoadPointsHistoryExposesMovements() = testScope.runTest {
        advanceUntilIdle()

        val state = viewModel.pointsHistoryState.value
        assertTrue(state is PointsHistoryUiState.Loaded)
        val loaded = state as PointsHistoryUiState.Loaded
        assertEquals(1_250, loaded.balance)
        assertEquals(3, loaded.movements.size)
        assertFalse(loaded.isSample)

        // El primer movimiento es el más reciente (abono de reciclaje)
        val firstMovement = loaded.movements.first()
        assertEquals("MOV-01", firstMovement.id)
        assertTrue(firstMovement.points > 0)
    }

    // Valida el movimiento neto calculado en el historial
    @Test
    fun testPointsHistoryNetMovementCalculation() = testScope.runTest {
        advanceUntilIdle()

        val state = viewModel.pointsHistoryState.value as? PointsHistoryUiState.Loaded
        assertNotNull(state)
        // 80 (reciclaje) + (-450) (canje) + 50 (educación) = -320
        assertEquals(-320, state!!.netMovement)
    }

    // Valida el filtro de cupones por categoría
    @Test
    fun testFilterCouponsByCategory() = testScope.runTest {
        advanceUntilIdle()

        viewModel.onCategorySelected(CouponCategory.GROCERIES)
        val state = viewModel.benefitsState.value
        assertEquals(CouponCategory.GROCERIES, state.category)
        val visible = state.visibleCoupons
        assertTrue(visible.all { it.category == CouponCategory.GROCERIES })
        assertEquals(1, visible.size)
    }

    // Valida que al limpiar el filtro se muestren todos los cupones
    @Test
    fun testClearCategoryFilterShowsAllCoupons() = testScope.runTest {
        advanceUntilIdle()

        viewModel.onCategorySelected(CouponCategory.FOOD)
        viewModel.onCategorySelected(null)
        assertEquals(2, viewModel.benefitsState.value.visibleCoupons.size)
    }

    // Valida el flujo de canje exitoso debitando los puntos correctamente
    @Test
    fun testSuccessfulCouponRedemptionDebitsSaldo() = testScope.runTest {
        advanceUntilIdle()

        // Carga cupón CUP-02 (costo 300 pts, saldo inicial 1250)
        viewModel.loadCouponForRedemption("CUP-02")
        viewModel.onRedeemRequested()
        // Con saldo 1250 >= 300 debe avanzar a CONFIRM
        assertEquals(RedemptionStep.CONFIRM, viewModel.redemptionState.value.step)

        viewModel.onConfirmRedeem()
        advanceUntilIdle()

        // Debe quedar en REDEEMED y saldo reducido a 950
        val redemption = viewModel.redemptionState.value
        assertEquals(RedemptionStep.REDEEMED, redemption.step)
        assertNotNull(redemption.receipt)
        assertEquals(950, redemption.receipt?.balanceAfter)

        // El saldo en la vista de beneficios también debe actualizarse
        assertEquals(950, viewModel.benefitsState.value.balance)
    }

    // Valida que si el saldo es insuficiente, se muestre el paso INSUFFICIENT
    @Test
    fun testInsufficientBalanceForRedemption() = testScope.runTest {
        advanceUntilIdle()

        // Ajusta el saldo a un valor bajo
        repository.balance = 100
        viewModel.loadBenefits()
        advanceUntilIdle()

        viewModel.loadCouponForRedemption("CUP-01") // Cuesta 450 pts
        viewModel.onRedeemRequested()

        assertEquals(RedemptionStep.INSUFFICIENT, viewModel.redemptionState.value.step)
    }

    // Valida el manejo de error cuando el repositorio falla al cargar saldo/cupones
    @Test
    fun testLoadBenefitsErrorHandling() = testScope.runTest {
        repository.error = RuntimeException("Error simulado de red")
        viewModel.loadBenefits()
        advanceUntilIdle()

        val state = viewModel.benefitsState.value
        assertFalse(state.isLoading)
        assertNotNull(state.error)
    }

    // Valida el manejo de error en el historial de movimientos
    @Test
    fun testLoadPointsHistoryErrorHandling() = testScope.runTest {
        repository.error = RuntimeException("Error simulado de red")
        viewModel.loadPointsHistory()
        advanceUntilIdle()

        assertTrue(viewModel.pointsHistoryState.value is PointsHistoryUiState.Error)
    }

    // Valida que al volver al detalle del cupón desde la confirmación, el paso sea DETAIL
    @Test
    fun testBackToDetailResetsStep() = testScope.runTest {
        advanceUntilIdle()

        viewModel.loadCouponForRedemption("CUP-01")
        viewModel.onRedeemRequested()
        assertEquals(RedemptionStep.CONFIRM, viewModel.redemptionState.value.step)

        viewModel.onBackToDetail()
        assertEquals(RedemptionStep.DETAIL, viewModel.redemptionState.value.step)
    }
}
