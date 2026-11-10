package com.ecoruta.app.features.reports.presentation.create

import android.net.Uri
import com.ecoruta.app.R
import com.ecoruta.app.core.testing.FakeReportRepository
import com.ecoruta.app.core.testing.MainDispatcherRule
import com.ecoruta.app.features.reports.domain.model.AffectationType
import com.ecoruta.app.features.reports.domain.model.ReportStatus
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

// Pruebas unitarias de ReportViewModel / CreateReportViewModel: validación de fotos requeridas,
// validación de coordenadas GPS requeridas, manejo de fallas de subida y estado inicial pendiente
@OptIn(ExperimentalCoroutinesApi::class)
class ReportViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var fakeRepository: FakeReportRepository
    private lateinit var viewModel: CreateReportViewModel

    @Before
    fun setUp() {
        fakeRepository = FakeReportRepository()
        viewModel = CreateReportViewModel(fakeRepository)
    }

    @Test
    fun initialState_hasDefaultValuesAndNoErrors() {
        val state = viewModel.uiState.value
        assertTrue(state.photos.isEmpty())
        assertNull(state.type)
        assertNull(state.latitude)
        assertNull(state.longitude)
        assertNull(state.photoError)
        assertNull(state.typeError)
        assertNull(state.locationError)
        assertFalse(state.isSubmitting)
    }

    @Test
    fun onSubmit_withMissingPhoto_showsPhotoErrorAndDoesNotSubmit() = runTest {
        val events = mutableListOf<ReportEvent>()
        val collector = launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.events.toList(events) }

        viewModel.onTypeSelected(AffectationType.CONTENEDOR_LLENO)
        viewModel.onLocationUpdated(latitude = 4.0847, longitude = -76.1953, address = "Calle 25 #14-10")
        // No photos added

        viewModel.onSubmit()
        advanceUntilIdle()

        assertEquals(R.string.report_photo_required, viewModel.uiState.value.photoError)
        assertFalse(viewModel.uiState.value.isSubmitting)
        assertTrue(fakeRepository.uploadedPhotoBatches.isEmpty())
        assertTrue(events.isEmpty())
        collector.cancel()
    }

    @Test
    fun onSubmit_withMissingGps_showsLocationErrorAndDoesNotSubmit() = runTest {
        val events = mutableListOf<ReportEvent>()
        val collector = launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.events.toList(events) }

        viewModel.onTypeSelected(AffectationType.BOTADERO_CLANDESTINO)
        // Photos provided via photosBytes, but no latitude / longitude
        viewModel.onSubmit(photosBytes = listOf(byteArrayOf(1, 2, 3)))
        advanceUntilIdle()

        assertEquals(R.string.report_location_required, viewModel.uiState.value.locationError)
        assertFalse(viewModel.uiState.value.isSubmitting)
        assertTrue(fakeRepository.uploadedPhotoBatches.isEmpty())
        assertTrue(events.isEmpty())
        collector.cancel()
    }

    @Test
    fun onSubmit_onFailedUpload_emitsErrorAndResetsSubmittingState() = runTest {
        val events = mutableListOf<ReportEvent>()
        val collector = launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.events.toList(events) }

        fakeRepository.error = RuntimeException("Upload failed")

        viewModel.onTypeSelected(AffectationType.CONTENEDOR_LLENO)
        viewModel.onLocationUpdated(latitude = 4.0847, longitude = -76.1953, address = "Calle 25 #14-10")
        viewModel.onSubmit(photosBytes = listOf(byteArrayOf(1, 2, 3)))
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isSubmitting)
        val errors = events.filterIsInstance<ReportEvent.Error>()
        assertEquals(1, errors.size)
        assertEquals(R.string.report_submit_error, errors.first().messageRes)
        collector.cancel()
    }

    @Test
    fun onSubmit_onSuccess_entersAsPendiente() = runTest {
        val events = mutableListOf<ReportEvent>()
        val collector = launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.events.toList(events) }

        viewModel.onTypeSelected(AffectationType.CONTENEDOR_LLENO)
        viewModel.onLocationUpdated(latitude = 4.0847, longitude = -76.1953, address = "Calle 25 #14-10")
        viewModel.onDescriptionChange("Contenedor totalmente lleno")
        viewModel.onSubmit(photosBytes = listOf(byteArrayOf(1, 2, 3)))
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isSubmitting)
        val successes = events.filterIsInstance<ReportEvent.Success>()
        assertEquals(1, successes.size)

        val createdReport = successes.first().report
        // El reporte nuevo debe ingresar en estado PENDIENTE hasta ser validado por un operador
        assertEquals(ReportStatus.PENDIENTE, createdReport.status)
        assertEquals(AffectationType.CONTENEDOR_LLENO, createdReport.type)
        assertEquals(4.0847, createdReport.latitude, 0.0001)
        assertEquals(-76.1953, createdReport.longitude, 0.0001)

        // Verificar que en el repositorio también esté en estado PENDIENTE
        val repoReport = fakeRepository.reports.find { it.id == createdReport.id }
        assertEquals(ReportStatus.PENDIENTE, repoReport?.status)

        collector.cancel()
    }
}
