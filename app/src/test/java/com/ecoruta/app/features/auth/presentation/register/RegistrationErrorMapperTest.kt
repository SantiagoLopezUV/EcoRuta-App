package com.ecoruta.app.features.auth.presentation.register

import com.ecoruta.app.R
import io.github.jan.supabase.auth.exception.AuthRestException
import io.github.jan.supabase.exceptions.HttpRequestException
import io.github.jan.supabase.exceptions.RestException
import io.ktor.client.plugins.HttpRequestTimeoutException
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.url
import io.ktor.http.HttpMethod
import java.io.IOException
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import org.junit.Assert.assertEquals
import org.junit.Test

// Pruebas de la función que convierte la falla del registro en el mensaje para el usuario
class RegistrationErrorMapperTest {

    @Test
    fun unknownHost_returnsConnectionMessage() {
        assertEquals(
            R.string.registration_error_no_connection,
            registrationErrorMessage(UnknownHostException("Unable to resolve host")),
        )
    }

    @Test
    fun refusedConnection_returnsConnectionMessage() {
        assertEquals(
            R.string.registration_error_no_connection,
            registrationErrorMessage(ConnectException("Connection refused")),
        )
    }

    @Test
    fun timeout_returnsConnectionMessage() {
        assertEquals(
            R.string.registration_error_no_connection,
            registrationErrorMessage(SocketTimeoutException("timeout")),
        )
    }

    @Test
    fun networkFailureWrappedByClient_returnsConnectionMessage() {
        val wrapped = IOException("HTTP request failed", UnknownHostException("10.0.2.2"))

        assertEquals(R.string.registration_error_no_connection, registrationErrorMessage(wrapped))
    }

    @Test
    fun userAlreadyRegistered_returnsEmailMessage() {
        assertEquals(
            R.string.registration_error_email_registered,
            registrationErrorMessage(RuntimeException("User already registered")),
        )
    }

    @Test
    fun duplicateDocument_returnsDocumentMessage() {
        val error = RuntimeException(
            "duplicate key value violates unique constraint \"profiles_document_id_key\"",
        )

        assertEquals(R.string.registration_error_document_registered, registrationErrorMessage(error))
    }

    @Test
    fun duplicateWithoutField_returnsCombinedMessage() {
        assertEquals(
            R.string.registration_error_already_registered,
            registrationErrorMessage(RuntimeException("23505")),
        )
    }

    // Arma la petición que el cliente de Supabase guarda dentro de sus excepciones de red
    private fun signUpRequest() = HttpRequestBuilder().apply {
        method = HttpMethod.Post
        url("http://10.0.2.2:54321/auth/v1/signup")
    }

    @Test
    fun supabaseRequestFailure_returnsConnectionMessage() {
        val error = HttpRequestException("Failed to connect to /10.0.2.2:54321", signUpRequest())

        assertEquals(R.string.registration_error_no_connection, registrationErrorMessage(error))
    }

    @Test
    fun ktorRequestTimeout_returnsConnectionMessage() {
        val error = HttpRequestTimeoutException(signUpRequest())

        assertEquals(R.string.registration_error_no_connection, registrationErrorMessage(error))
    }

    @Test
    fun supabaseFailureNestedInAnotherError_returnsConnectionMessage() {
        val nested = IllegalStateException(
            "No se pudo crear la cuenta",
            HttpRequestException("Connection refused", signUpRequest()),
        )

        assertEquals(R.string.registration_error_no_connection, registrationErrorMessage(nested))
    }

    @Test
    fun authUserAlreadyExists_returnsEmailMessage() {
        val error = AuthRestException("user_already_exists", "User already registered", 422)

        assertEquals(R.string.registration_error_email_registered, registrationErrorMessage(error))
    }

    @Test
    fun authEmailExistsWithoutTextHint_returnsEmailMessage() {
        val error = AuthRestException("email_exists", "Conflict", 422)

        assertEquals(R.string.registration_error_email_registered, registrationErrorMessage(error))
    }

    // Así arma Postgrest el error 409 de la cédula repetida, con el mensaje y el detalle de la base
    @Test
    fun postgrestDuplicateDocument_returnsDocumentMessage() {
        val error = RestException(
            error = "duplicate key value violates unique constraint \"profiles_document_id_key\"",
            description = "Key (document_id)=(1116234567) already exists.",
            statusCode = 409,
            message = "duplicate key value violates unique constraint \"profiles_document_id_key\" " +
                "(Key (document_id)=(1116234567) already exists.)\nURL: http://10.0.2.2:54321/rest/v1/profiles",
        )

        assertEquals(R.string.registration_error_document_registered, registrationErrorMessage(error))
    }

    @Test
    fun unknownFailure_returnsGenericMessage() {
        assertEquals(
            R.string.login_error_generic,
            registrationErrorMessage(IllegalStateException("Internal server error")),
        )
    }
}
