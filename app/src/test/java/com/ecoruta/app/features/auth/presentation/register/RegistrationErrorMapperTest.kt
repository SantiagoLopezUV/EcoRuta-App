package com.ecoruta.app.features.auth.presentation.register

import com.ecoruta.app.R
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

    @Test
    fun unknownFailure_returnsGenericMessage() {
        assertEquals(
            R.string.login_error_generic,
            registrationErrorMessage(IllegalStateException("Internal server error")),
        )
    }
}
