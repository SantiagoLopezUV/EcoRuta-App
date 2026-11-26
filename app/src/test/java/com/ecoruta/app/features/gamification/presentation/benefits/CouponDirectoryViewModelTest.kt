package com.ecoruta.app.features.gamification.presentation.benefits

// Pruebas unitarias de CouponDirectoryViewModel: catálogo, filtros y flujo de redención

import androidx.lifecycle.SavedStateHandle
import com.ecoruta.app.core.testing.FakeGamificationRepository
import com.ecoruta.app.core.testing.FakeProfileRepository
import com.ecoruta.app.core.testing.MainDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class CouponDirectoryViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var fakeGamificationRepository: FakeGamificationRepository
    private lateinit var fakeProfileRepository: FakeProfileRepository

    @Before
    fun setUp() {
        fakeGamificationRepository = FakeGamificationRepository()
        fakeProfileRepository = FakeProfileRepository()
    }

    private fun createViewModel(couponId: String? = null): CouponDirectoryViewModel {
        val handle = if (couponId != null) SavedStateHandle(mapOf("id" to couponId)) else SavedStateHandle()
        return CouponDirectoryViewModel(handle, fakeGamificationRepository, fakeProfileRepository)
    }

    // --- Catálogo ---

    @Test
    fun loadCatalog_exposesCouponsFromRepository() = runTest {
        val viewModel = createViewModel()
        advanceUntilIdle()

        val state = viewModel.catalogState.value
        assertFalse(state.isLoading)
        assertEquals(fakeGamificationRepository.catalogCoupons.size, state.coupons.size)
    }

    @Test
    fun loadCatalog_exposesBalanceFromProfile() = runTest {
        fakeProfileRepository.currentProfile = fakeProfileRepository.currentProfile.copy(ecoPoints = 1_250)
        val viewModel = createViewModel()
        advanceUntilIdle()

        assertEquals(1_250, viewModel.catalogState.value.balance)
    }

    @Test
    fun loadCatalog_onFailure_setsError() = runTest {
        fakeGamificationRepository.error = RuntimeException("Network error")
        val viewModel = createViewModel()
        advanceUntilIdle()

        assertNotNull(viewModel.catalogState.value.error)
    }

    @Test
    fun onCategorySelected_filtersCoupons() = runTest {
        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.onCategorySelected(CouponCategory.GROCERIES)

        val visible = viewModel.catalogState.value.visibleCoupons
        assertTrue(visible.all { it.category == CouponCategory.GROCERIES })
    }

    @Test
    fun onCategorySelected_null_showsAllCoupons() = runTest {
        val viewModel = createViewModel()
        advanceUntilIdle()
        viewModel.onCategorySelected(CouponCategory.FOOD)
        viewModel.onCategorySelected(null)

        val state = viewModel.catalogState.value
        assertEquals(state.coupons.size, state.visibleCoupons.size)
    }

    // --- Redención ---

    @Test
    fun redemptionMode_loadsSelectedCoupon() = runTest {
        val viewModel = createViewModel(couponId = "BEN-01")
        advanceUntilIdle()

        val state = viewModel.redemptionState.value
        assertFalse(state.isLoading)
        assertNotNull(state.coupon)
        assertEquals("BEN-01", state.coupon?.id)
    }

    @Test
    fun redemptionMode_onCouponNotFound_setsError() = runTest {
        fakeGamificationRepository.error = RuntimeException("Not found")
        val viewModel = createViewModel(couponId = "BEN-MISSING")
        advanceUntilIdle()

        assertNotNull(viewModel.redemptionState.value.error)
    }

    @Test
    fun onConfirmRedemption_advancesToConfirmStep() = runTest {
        val viewModel = createViewModel(couponId = "BEN-01")
        advanceUntilIdle()

        viewModel.onConfirmRedemption()

        assertEquals(RedemptionStep.CONFIRM, viewModel.redemptionState.value.step)
    }

    @Test
    fun onCancelRedemption_returnsToDetailStep() = runTest {
        val viewModel = createViewModel(couponId = "BEN-01")
        advanceUntilIdle()
        viewModel.onConfirmRedemption()

        viewModel.onCancelRedemption()

        assertEquals(RedemptionStep.DETAIL, viewModel.redemptionState.value.step)
    }

    @Test
    fun onRedeemConfirmed_setsRedeemedStep() = runTest {
        val viewModel = createViewModel(couponId = "BEN-01")
        advanceUntilIdle()

        viewModel.onConfirmRedemption()
        viewModel.onRedeemConfirmed()
        advanceUntilIdle()

        assertEquals(RedemptionStep.REDEEMED, viewModel.redemptionState.value.step)
        assertNotNull(viewModel.redemptionState.value.receipt)
    }

    @Test
    fun onRedeemConfirmed_onFailure_setsError() = runTest {
        fakeGamificationRepository.redeemError = RuntimeException("Redeem failed")
        val viewModel = createViewModel(couponId = "BEN-01")
        advanceUntilIdle()

        viewModel.onConfirmRedemption()
        viewModel.onRedeemConfirmed()
        advanceUntilIdle()

        assertNotNull(viewModel.redemptionState.value.error)
        assertNull(viewModel.redemptionState.value.receipt)
    }

    @Test
    fun redemptionMode_insufficientBalance_setsInsufficientStep() = runTest {
        fakeProfileRepository.currentProfile = fakeProfileRepository.currentProfile.copy(ecoPoints = 10)
        val viewModel = createViewModel(couponId = "BEN-01")
        advanceUntilIdle()

        assertEquals(RedemptionStep.INSUFFICIENT, viewModel.redemptionState.value.step)
    }
}
