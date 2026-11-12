package com.ecoruta.app.features.auth.presentation.register

// Traduce la falla del registro de ciudadano en el texto que se le muestra al usuario
// Distingue falta de conexión, datos ya registrados y cualquier otro error

import androidx.annotation.StringRes
import com.ecoruta.app.R
import io.github.jan.supabase.auth.exception.AuthErrorCode
import io.github.jan.supabase.auth.exception.AuthRestException
import io.github.jan.supabase.exceptions.HttpRequestException
import io.ktor.client.plugins.HttpRequestTimeoutException
import java.io.InterruptedIOException
import java.net.ConnectException
import java.net.NoRouteToHostException
import java.net.UnknownHostException

@StringRes
fun registrationErrorMessage(error: Throwable): Int {
    // Recorre la causa original porque el cliente suele envolver la falla de red en otra excepción
    val chain = generateSequence(error) { it.cause }.take(MAX_CAUSE_DEPTH).toList()

    if (chain.any { it.isNetworkFailure() }) return R.string.registration_error_no_connection

    // Auth avisa con un código propio cuando el correo ya tiene una cuenta
    if (chain.any { it is AuthRestException && it.errorCode in EMAIL_TAKEN_CODES }) {
        return R.string.registration_error_email_registered
    }

    val text = chain.joinToString(" ") { it.message.orEmpty() }.lowercase()
    if (DUPLICATE_HINTS.none { it in text }) return R.string.login_error_generic

    // Si el servidor dice qué dato está repetido se usa el mensaje específico de ese dato
    return when {
        DOCUMENT_HINTS.any { it in text } -> R.string.registration_error_document_registered
        EMAIL_HINTS.any { it in text } -> R.string.registration_error_email_registered
        else -> R.string.registration_error_already_registered
    }
}

// El cliente de Supabase cambia la falla de red por su propia excepción sin guardar la causa original
// Ktor deja pasar su excepción de tiempo agotado sin envolverla
private fun Throwable.isNetworkFailure(): Boolean =
    this is HttpRequestException ||
        this is HttpRequestTimeoutException ||
        this is UnknownHostException ||
        this is ConnectException ||
        this is NoRouteToHostException ||
        this is InterruptedIOException ||
        javaClass.simpleName.contains("Timeout")

private const val MAX_CAUSE_DEPTH = 10

private val EMAIL_TAKEN_CODES = setOf(AuthErrorCode.UserAlreadyExists, AuthErrorCode.EmailExists)

private val DUPLICATE_HINTS = listOf(
    "already registered",
    "already exists",
    "user_already_exists",
    "email_exists",
    "duplicate key",
    "23505",
    "ya está registrad",
)

private val DOCUMENT_HINTS = listOf("document", "cédula")

private val EMAIL_HINTS = listOf("email", "correo", "user already", "user_already_exists")
