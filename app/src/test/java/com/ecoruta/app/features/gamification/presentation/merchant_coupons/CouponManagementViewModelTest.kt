package com.ecoruta.app.features.gamification.presentation.merchant_coupons

// Pruebas unitarias de CouponManagementViewModel: lista, formulario, validación y desactivación

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
class CouponManagementViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var fakeGamificationRepository: FakeGamificationRepository
    private lateinit var fakeProfileRepository: FakeProfileRepository

    @Before
    fun setUp() {
        fakeGamificationRepository = FakeGamificationRepository()
        fakeProfileRepository = FakeProfileRepository()
    }

    private fun createViewModel(couponId: String? = null): CouponManagementViewModel {
        val handle = if (couponId != null) SavedStateHandle(mapOf("id" to couponId)) else SavedStateHandle()
        return CouponManagementViewModel(handle, fakeGamificationRepository, fakeProfileRepository)
    }

    // --- Lista de cupones ---

    @Test
    fun loadCoupons_exposesCouponsFromRepository() = runTest {
        val viewModel = createViewModel()
        advanceUntilIdle()

        val state = viewModel.couponsState.value
        assertFalse(state.isLoading)
        assertEquals(fakeGamificationRepository.coupons.size, state.coupons.size)
        assertEquals("CPN-01", state.coupons.first().id)
    }

    @Test
    fun loadCoupons_onFailure_setsError() = runTest {
        fakeGamificationRepository.error = RuntimeException("Network error")
        val viewModel = createViewModel()
        advanceUntilIdle()

        assertTrue(viewModel.couponsState.value.hasError)
    }

    @Test
    fun deactivateCoupon_callsRepositoryAndRefreshes() = runTest {
        val viewModel = createViewModel()
        advanceUntilIdle()
        val coupon = viewModel.couponsState.value.coupons.first()

        viewModel.onDeactivateCoupon(coupon)
        viewModel.onConfirmDeactivate()
        advanceUntilIdle()

        assertTrue(fakeGamificationRepository.deactivatedIds.contains(coupon.id))
    }

    @Test
    fun dismissDeactivate_clearsPendingCoupon() = runTest {
        val viewModel = createViewModel()
        advanceUntilIdle()
        val coupon = viewModel.couponsState.value.coupons.first()
        viewModel.onDeactivateCoupon(coupon)
        assertNotNull(viewModel.couponsState.value.couponToDeactivate)

        viewModel.onDismissDeactivate()
        assertNull(viewModel.couponsState.value.couponToDeactivate)
    }

    // --- Formulario de creación ---

    @Test
    fun submit_withValidForm_callsCreate() = runTest {
        val viewModel = createViewModel()
        advanceUntilIdle()
        viewModel.onTitleChange("Descuento 15%")
        viewModel.onDescriptionChange("Solo en frutas y verduras")
        viewModel.onPointsChange("400")
        viewModel.onQuantityChange("10")
        viewModel.onDateChange(System.currentTimeMillis() + 86_400_000L)

        viewModel.onSubmit()
        advanceUntilIdle()

        assertEquals(1, fakeGamificationRepository.createCalls.size)
        assertFalse(viewModel.formState.value.hasError)
    }

    @Test
    fun submit_withBlankTitle_setsInvalidField() = runTest {
        val viewModel = createViewModel()
        advanceUntilIdle()
        viewModel.onDescriptionChange("Descripción válida")
        viewModel.onPointsChange("200")
        viewModel.onQuantityChange("5")
        viewModel.onDateChange(System.currentTimeMillis() + 86_400_000L)

        viewModel.onSubmit()

        assertTrue(MerchantCouponField.TITLE in viewModel.formState.value.invalidFields)
        assertEquals(0, fakeGamificationRepository.createCalls.size)
    }

    @Test
    fun submit_withNegativePoints_setsInvalidField() = runTest {
        val viewModel = createViewModel()
        advanceUntilIdle()
        viewModel.onTitleChange("Título")
        viewModel.onDescriptionChange("Descripción")
        viewModel.onPointsChange("-100")
        viewModel.onQuantityChange("5")
        viewModel.onDateChange(System.currentTimeMillis() + 86_400_000L)

        viewModel.onSubmit()

        assertTrue(MerchantCouponField.POINTS in viewModel.formState.value.invalidFields)
    }

    @Test
    fun submit_withPastDate_setsInvalidField() = runTest {
        val viewModel = createViewModel()
        advanceUntilIdle()
        viewModel.onTitleChange("Título")
        viewModel.onDescriptionChange("Descripción")
        viewModel.onPointsChange("300")
        viewModel.onQuantityChange("10")
        viewModel.onDateChange(System.currentTimeMillis() - 1_000L)

        viewModel.onSubmit()

        assertTrue(MerchantCouponField.VALID_UNTIL in viewModel.formState.value.invalidFields)
    }

    // --- Formulario de edición ---

    @Test
    fun editMode_prePopulatesFormWithCouponData() = runTest {
        val viewModel = createViewModel(couponId = "CPN-01")
        advanceUntilIdle()

        val form = viewModel.formState.value
        assertEquals("10% en toda la tienda", form.title)
        assertEquals("500", form.pointsCost)
    }

    @Test
    fun editMode_onSubmit_callsUpdate() = runTest {
        val viewModel = createViewModel(couponId = "CPN-01")
        advanceUntilIdle()
        viewModel.onTitleChange("15% en toda la tienda")
        viewModel.onDescriptionChange(viewModel.formState.value.description)
        viewModel.onPointsChange("600")
        viewModel.onQuantityChange("20")
        viewModel.onDateChange(System.currentTimeMillis() + 86_400_000L)

        viewModel.onSubmit()
        advanceUntilIdle()

        assertEquals(1, fakeGamificationRepository.updateCalls.size)
        assertEquals("CPN-01", fakeGamificationRepository.updateCalls.first()["id"])
    }

    @Test
    fun editMode_withMissingCoupon_setsMissingFlag() = runTest {
        fakeGamificationRepository.error = RuntimeException("Not found")
        val viewModel = createViewModel(couponId = "CPN-MISSING")
        advanceUntilIdle()

        assertTrue(viewModel.formState.value.isCouponMissing)
    }
}
