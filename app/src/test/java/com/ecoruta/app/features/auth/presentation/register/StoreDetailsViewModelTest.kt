package com.ecoruta.app.features.auth.presentation.register

import com.ecoruta.app.R
import com.ecoruta.app.core.testing.MainDispatcherRule
import com.ecoruta.app.features.profile.presentation.StoreCategory
import com.ecoruta.app.features.profile.presentation.StoreIdType
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

// Pruebas de los campos, la validación y el estado del paso de datos del comercio
@OptIn(ExperimentalCoroutinesApi::class)
class StoreDetailsViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var viewModel: StoreDetailsViewModel

    @Before
    fun setUp() {
        viewModel = StoreDetailsViewModel()
    }

    // Llena el formulario con datos válidos y la política aceptada para que cada prueba cambie solo lo que revisa
    private fun fillValidForm() {
        viewModel.onNameChange("Papelería El Lápiz")
        viewModel.onIdTypeChange(StoreIdType.NIT)
        viewModel.onIdNumberChange("900.123.456-7")
        viewModel.onCategoryChange(StoreCategory.STATIONERY_BOOKSTORE)
        viewModel.onAddressChange("Calle 27 # 30-15")
        viewModel.onPhoneChange("602 234 5678")
        viewModel.onRutPicked("rut_papeleria.pdf")
        viewModel.onDataPolicyChange(true)
    }

    @Test
    fun initialState_hasEmptyFieldsAndNoErrors() {
        val state = viewModel.uiState.value
        assertEquals(StoreDetailsUiState(), state)
        assertEquals(StoreIdType.NIT, state.idType)
        assertFalse(state.acceptedDataPolicy)
        assertFalse(state.hasErrors)
    }

    @Test
    fun editingFields_updatesStateValues() {
        fillValidForm()

        val state = viewModel.uiState.value
        assertEquals("Papelería El Lápiz", state.name)
        assertEquals(StoreIdType.NIT, state.idType)
        assertEquals("900.123.456-7", state.idNumber)
        assertEquals(StoreCategory.STATIONERY_BOOKSTORE, state.category)
        assertEquals("Calle 27 # 30-15", state.address)
        assertEquals("602 234 5678", state.phone)
        assertTrue(state.acceptedDataPolicy)
    }

    @Test
    fun submitWithoutDataPolicy_doesNotValidate() {
        viewModel.onSubmit()

        assertFalse(viewModel.uiState.value.hasErrors)
    }

    @Test
    fun emptyFormWithPolicy_showsErrorsOnEveryField() {
        viewModel.onDataPolicyChange(true)
        viewModel.onSubmit()

        val state = viewModel.uiState.value
        assertEquals(R.string.store_application_error_name, state.nameErrorRes)
        assertEquals(R.string.store_registration_error_id, state.idErrorRes)
        assertEquals(R.string.store_application_error_category, state.categoryErrorRes)
        assertEquals(R.string.store_registration_error_address, state.addressErrorRes)
        assertEquals(R.string.store_registration_error_phone, state.phoneErrorRes)
        assertEquals(R.string.store_application_error_rut, state.rutErrorRes)
    }

    @Test
    fun pickingRut_storesNameAndClearsError() {
        viewModel.onDataPolicyChange(true)
        viewModel.onSubmit()

        viewModel.onRutPicked("rut.pdf")

        assertEquals("rut.pdf", viewModel.uiState.value.rutFileName)
        assertNull(viewModel.uiState.value.rutErrorRes)
    }

    @Test
    fun validFormWithoutRut_showsRutErrorAndDoesNotEmit() = runTest {
        val events = mutableListOf<StoreDetailsEvent>()
        val collector = launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.events.toList(events) }

        fillValidForm()
        viewModel.onRemovePdf()
        viewModel.onSubmit()
        advanceUntilIdle()

        assertNull(viewModel.uiState.value.rutFileName)
        assertEquals(R.string.store_application_error_rut, viewModel.uiState.value.rutErrorRes)
        assertTrue(events.isEmpty())
        collector.cancel()
    }

    @Test
    fun nitWithoutVerificationDigit_showsNitError() {
        fillValidForm()
        viewModel.onIdNumberChange("900123456")
        viewModel.onSubmit()

        assertEquals(R.string.store_application_error_nit, viewModel.uiState.value.idErrorRes)
    }

    @Test
    fun nitWithoutDotsOrDash_isAccepted() {
        fillValidForm()
        viewModel.onIdNumberChange("9001234567")
        viewModel.onSubmit()

        assertNull(viewModel.uiState.value.idErrorRes)
    }

    @Test
    fun nationalIdTooShort_showsCedulaError() {
        fillValidForm()
        viewModel.onIdTypeChange(StoreIdType.NATIONAL_ID)
        viewModel.onIdNumberChange("12345")
        viewModel.onSubmit()

        assertEquals(R.string.store_application_error_cedula, viewModel.uiState.value.idErrorRes)
    }

    @Test
    fun validNationalId_isAccepted() {
        fillValidForm()
        viewModel.onIdTypeChange(StoreIdType.NATIONAL_ID)
        viewModel.onIdNumberChange("1116234567")
        viewModel.onSubmit()

        assertNull(viewModel.uiState.value.idErrorRes)
    }

    @Test
    fun phoneWithNineDigits_showsPhoneError() {
        fillValidForm()
        viewModel.onPhoneChange("300123456")
        viewModel.onSubmit()

        assertEquals(R.string.store_application_error_phone, viewModel.uiState.value.phoneErrorRes)
    }

    @Test
    fun blankNameAndAddress_showRequiredErrors() {
        fillValidForm()
        viewModel.onNameChange(" ")
        viewModel.onAddressChange(" ")
        viewModel.onSubmit()

        val state = viewModel.uiState.value
        assertEquals(R.string.store_application_error_name, state.nameErrorRes)
        assertEquals(R.string.store_registration_error_address, state.addressErrorRes)
    }

    @Test
    fun editingFieldsAfterErrors_clearsEachError() {
        viewModel.onDataPolicyChange(true)
        viewModel.onSubmit()

        viewModel.onNameChange("P")
        viewModel.onIdNumberChange("9")
        viewModel.onCategoryChange(StoreCategory.OTHER)
        viewModel.onAddressChange("C")
        viewModel.onPhoneChange("3")
        viewModel.onRutPicked("rut.pdf")

        assertFalse(viewModel.uiState.value.hasErrors)
    }

    @Test
    fun changingIdType_clearsIdError() {
        viewModel.onDataPolicyChange(true)
        viewModel.onSubmit()
        viewModel.onIdTypeChange(StoreIdType.NATIONAL_ID)

        assertNull(viewModel.uiState.value.idErrorRes)
    }

    @Test
    fun retry_clearsOfflineNotice() {
        viewModel.onRetry()

        assertFalse(viewModel.uiState.value.isOffline)
    }

    @Test
    fun validForm_hasNoErrorsAndEmitsFormValid() = runTest {
        val events = mutableListOf<StoreDetailsEvent>()
        val collector = launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.events.toList(events) }

        fillValidForm()
        viewModel.onSubmit()
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.hasErrors)
        assertEquals(listOf(StoreDetailsEvent.FormValid), events)
        collector.cancel()
    }

    @Test
    fun invalidForm_doesNotEmitFormValid() = runTest {
        val events = mutableListOf<StoreDetailsEvent>()
        val collector = launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.events.toList(events) }

        viewModel.onDataPolicyChange(true)
        viewModel.onSubmit()
        advanceUntilIdle()

        assertTrue(events.isEmpty())
        collector.cancel()
    }
}
