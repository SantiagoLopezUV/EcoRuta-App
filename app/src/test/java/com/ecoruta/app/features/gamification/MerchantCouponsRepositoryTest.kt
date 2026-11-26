package com.ecoruta.app.features.gamification

// Pruebas unitarias para las operaciones de cupones de comercios aliados (ECO-1072)
// Valida creación, actualización, desactivación lógica y consulta de cupones propios

import com.ecoruta.app.core.testing.FakeGamificationRepository
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class MerchantCouponsRepositoryTest {

    private lateinit var repository: FakeGamificationRepository

    @Before
    fun setUp() {
        repository = FakeGamificationRepository()
    }

    // Valida que el comercio pueda crear un cupón con sus datos y quede activo por defecto
    @Test
    fun testCreateCouponSuccess() = runTest {
        val result = repository.createCoupon(
            title = "15 % de descuento en cafetería",
            pointsCost = 350,
            quantityAvailable = 20,
            validUntilMillis = 1_795_996_800_000L,
            category = "food",
            description = "Aplica en bebidas seleccionadas",
        )

        assertTrue(result.isSuccess)
        val coupon = result.getOrNull()
        assertNotNull(coupon)
        assertEquals("15 % de descuento en cafetería", coupon?.title)
        assertEquals(350, coupon?.pointsCost)
        assertEquals(20, coupon?.availableQuantity)
        assertEquals("food", coupon?.category)
        assertEquals("Aplica en bebidas seleccionadas", coupon?.subtitle)
        assertTrue(coupon?.isActive == true)
    }

    // Valida que la creación falle si el título está en blanco
    @Test
    fun testCreateCouponWithBlankTitleFails() = runTest {
        val result = repository.createCoupon(
            title = "   ",
            pointsCost = 350,
            quantityAvailable = 20,
        )

        assertTrue(result.isFailure)
    }

    // Valida que la creación falle si los puntos son cero o negativos
    @Test
    fun testCreateCouponWithZeroPointsFails() = runTest {
        val result = repository.createCoupon(
            title = "Cupón inválido",
            pointsCost = 0,
            quantityAvailable = 10,
        )

        assertTrue(result.isFailure)
    }

    // Valida que el comercio pueda actualizar los datos de un cupón existente
    @Test
    fun testUpdateCouponSuccess() = runTest {
        val created = repository.createCoupon(
            title = "10 % de descuento",
            pointsCost = 200,
            quantityAvailable = 15,
        ).getOrThrow()

        val updatedResult = repository.updateCoupon(
            couponId = created.id,
            title = "20 % de descuento recargado",
            pointsCost = 300,
            quantityAvailable = 25,
            validUntilMillis = created.validUntilMillis,
            category = "retail",
            description = "Nueva descripción",
            isActive = true,
        )

        assertTrue(updatedResult.isSuccess)
        val updated = updatedResult.getOrNull()
        assertNotNull(updated)
        assertEquals("20 % de descuento recargado", updated?.title)
        assertEquals(300, updated?.pointsCost)
        assertEquals(25, updated?.availableQuantity)
        assertEquals("retail", updated?.category)
    }

    // Valida que la desactivación de un cupón aplique baja lógica sin eliminarlo
    @Test
    fun testDeactivateCouponSuccess() = runTest {
        val created = repository.createCoupon(
            title = "Cupón temporal",
            pointsCost = 150,
            quantityAvailable = 5,
        ).getOrThrow()

        val deactivateResult = repository.deactivateCoupon(created.id)
        assertTrue(deactivateResult.isSuccess)

        val merchantCoupons = repository.getMerchantCoupons().getOrThrow()
        val deactivated = merchantCoupons.find { it.id == created.id }
        assertNotNull(deactivated)
        assertFalse(deactivated?.isActive ?: true)
    }

    // Valida que se consulten todos los cupones propios creados por el comercio
    @Test
    fun testGetMerchantCouponsReturnsAllCreated() = runTest {
        repository.createCoupon(title = "Cupón A", pointsCost = 100, quantityAvailable = 10)
        repository.createCoupon(title = "Cupón B", pointsCost = 200, quantityAvailable = 5)

        val list = repository.getMerchantCoupons().getOrThrow()
        assertEquals(2, list.size)
        assertTrue(list.any { it.title == "Cupón A" })
        assertTrue(list.any { it.title == "Cupón B" })
    }

    // Valida que los errores de red o servidor se propaguen como Result.failure
    @Test
    fun testSimulatedErrorReturnsFailure() = runTest {
        repository.error = RuntimeException("Fallo de conexión simulado")

        val createResult = repository.createCoupon("Título", 100, 5)
        assertTrue(createResult.isFailure)

        val listResult = repository.getMerchantCoupons()
        assertTrue(listResult.isFailure)
    }
}
