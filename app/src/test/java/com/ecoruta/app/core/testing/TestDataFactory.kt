package com.ecoruta.app.core.testing

import com.ecoruta.app.features.auth.domain.model.AuthSession
import com.ecoruta.app.features.auth.domain.model.BusinessRegistration
import com.ecoruta.app.features.auth.domain.model.CitizenRegistration
import com.ecoruta.app.features.auth.domain.model.RecyclerRegistration
import com.ecoruta.app.features.auth.domain.model.UserAccount

// Fábrica centralizada de modelos y datos simulados para pruebas
// Genera instancias de prueba de usuarios, reportes y cupones evitando duplicación
// Por ahora solo cubre autenticación porque los modelos de reportes y cupones aún no existen en el dominio
object TestDataFactory {

    const val DEFAULT_EMAIL = "ciudadano@ecoruta.test"
    const val DEFAULT_PASSWORD = "Clave1234"
    const val DEFAULT_USER_ID = "user-1"

    // Crea una cuenta confirmada con valores por defecto que cada prueba puede cambiar
    fun userAccount(
        id: String = DEFAULT_USER_ID,
        email: String = DEFAULT_EMAIL,
        fullName: String? = "Ana Gómez",
        isEmailConfirmed: Boolean = true,
        mustChangePassword: Boolean = false
    ) = UserAccount(
        id = id,
        email = email,
        fullName = fullName,
        isEmailConfirmed = isEmailConfirmed,
        mustChangePassword = mustChangePassword
    )

    // Crea una cuenta con clave temporal para probar el cambio obligatorio en el primer ingreso
    fun firstLoginAccount(
        id: String = "operator-1",
        email: String = "chofer@ecoruta.test"
    ) = userAccount(id = id, email = email, fullName = "Carlos Ruiz", mustChangePassword = true)

    // Crea una cuenta que llega desde Google, donde el nombre puede venir vacío
    fun googleAccount(
        id: String = "google-1",
        email: String = "google@ecoruta.test"
    ) = userAccount(id = id, email = email, fullName = null)

    // Envuelve una cuenta en una sesión activa
    fun authenticatedSession(user: UserAccount = userAccount()) = AuthSession.Authenticated(user)

    // El ciudadano puede registrarse sin cédula, por eso el documento va vacío por defecto
    fun citizenRegistration(
        email: String = DEFAULT_EMAIL,
        password: String = DEFAULT_PASSWORD,
        fullName: String = "Ana Gómez",
        documentNumber: String = "",
        phone: String = "3001234567",
        neighborhood: String = "Centro"
    ) = CitizenRegistration(
        email = email,
        password = password,
        fullName = fullName,
        documentNumber = documentNumber,
        phone = phone,
        neighborhood = neighborhood
    )

    // Sin asociación ni empresa el reciclador queda como independiente
    fun recyclerRegistration(
        email: String = "reciclador@ecoruta.test",
        password: String = DEFAULT_PASSWORD,
        fullName: String = "Luis Pérez",
        documentNumber: String = "1116000000",
        phone: String = "3109876543",
        associationName: String? = null,
        companyId: String? = null,
        idDocumentPdfUrl: String? = null
    ) = RecyclerRegistration(
        email = email,
        password = password,
        fullName = fullName,
        documentNumber = documentNumber,
        phone = phone,
        associationName = associationName,
        companyId = companyId,
        idDocumentPdfUrl = idDocumentPdfUrl
    )

    fun businessRegistration(
        email: String = "comercio@ecoruta.test",
        password: String = DEFAULT_PASSWORD,
        fullName: String = "Marta Díaz",
        documentNumber: String = "1116111111",
        businessName: String = "Café del Río",
        category: String = "Alimentos",
        address: String = "Calle 26 # 30-15",
        rutPdfUrl: String? = null
    ) = BusinessRegistration(
        email = email,
        password = password,
        fullName = fullName,
        documentNumber = documentNumber,
        businessName = businessName,
        category = category,
        address = address,
        rutPdfUrl = rutPdfUrl
    )
}
