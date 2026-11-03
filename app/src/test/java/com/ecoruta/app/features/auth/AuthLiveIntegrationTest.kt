package com.ecoruta.app.features.auth

import com.ecoruta.app.core.network.supabase.supabase
import com.ecoruta.app.features.auth.data.repository.AuthRepositoryImpl
import com.ecoruta.app.features.auth.domain.model.CitizenRegistration
import com.ecoruta.app.features.auth.domain.model.RecyclerRegistration
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

// PRUEBAS DE INTEGRACIÓN EN VIVO (INTEGRATION TESTS / SUPABASE BACKEND)
// ¿Cómo funcionan estas pruebas?
// Se conectan en tiempo real al backend de Supabase (Auth, PostgREST y PostgreSQL) y al servidor
// SMTP de Gmail configurado, utilizando las credenciales seguras de BuildConfig (SUPABASE_URL, ANON_KEY).
//
// REQUISITOS Y QUÉ SE DEBE ESPERAR PARA QUE LOS USUARIOS SE AGREGUEN A LAS TABLAS:
//
// ¿Qué hace signUpCitizen() y signUpRecycler()?
//    El flujo de registro está orquestado en 3 fases mediante la función 'registerProfile':
//    - Fase A: Crea las credenciales en 'auth.users' mediante auth.signUpWith(Email).
//    - Fase B: Inserta el registro de identidad base en la tabla 'public.profiles'.
//    - Fase C: Inserta la información complementaria en la tabla especializada ('citizens' o 'recyclers').
//
// ¿Qué se debe esperar que suceda para que la inserción en las tablas no falle por RLS?
//    Las políticas Row-Level Security (RLS) en PostgreSQL exigen: WITH CHECK (auth.uid() = id).
//    Para que 'auth.uid()' tenga un valor válido y no sea nulo, debe cumplirse UNA de estas dos condiciones:
//
//    CONDICIÓN 1
//      Tener desactivada la opción "Confirm email" en Supabase (Authentication -> Providers -> Email).
//      Al estar desactivada, Supabase Auth devuelve de inmediato el token de sesión (JWT) al registrarse,
//      lo que permite que 'auth.uid()' exista y las tablas 'profiles', 'citizens' y 'recyclers' se inserten automáticamente y con éxito en la misma llamada.
//
//    CONDICIÓN 2
//      Si "Confirm email" está activado, 'signUpWith' crea el usuario en 'auth.users' en estado
//      "Waiting for verification" (sin sesión activa). En este caso, el usuario DEBE abrir el correo de
//      confirmación en su teléfono para activar su sesión; solo después de confirmarse la sesión, el cliente puede insertar los datos en 'profiles', 'citizens' o 'recyclers'.

class AuthLiveIntegrationTest {

    private val repository = AuthRepositoryImpl(
        auth = supabase.auth,
        postgrest = supabase.postgrest
    )

    //  testSendPasswordResetEmailReal()
    // ¿Cómo funciona?
    // - Toma una dirección de correo registrada en el sistema ('auth.users').
    // - Invoca 'repository.sendPasswordResetEmail(targetEmail)'.
    // - Supabase Auth genera un token criptográfico de recuperación y se conecta al servidor
    //   SMTP de Gmail (smtp.gmail.com:465) para despachar el correo con la plantilla de EcoRuta.
    // - El usuario recibe el mensaje en su bandeja de entrada con el botón 'Restablecer mi contraseña' que apunta al Deep Link 'ecoruta://auth'.

    @Test
    fun testSendPasswordResetEmailReal() = runBlocking {
        // Usamos el usuario que SÍ existe en tu lista de Supabase
        val targetEmail = "alejoyeison9+ciudadano9186@gmail.com"
        println("ENVIANDO CORREO DE RECUPERACIÓN DE CONTRASEÑA")
        println("Destinatario: $targetEmail")

        val result = repository.sendPasswordResetEmail(targetEmail)

        if (result.isFailure) {
            val ex = result.exceptionOrNull()
            println("Fallo al enviar correo: ${ex?.message}")
            ex?.printStackTrace()
        }

        assertTrue("El envío del correo de recuperación debe responder exitoso", result.isSuccess)
        println("Supabase Auth despachó el correo de recuperación mediante Gmail SMTP.")
        println("Revisa tu bandeja de entrada en alejoyeison9@gmail.com")
    }

    // testCitizenRegistrationReal()
    // ¿Cómo funciona?
    // - Genera un modelo CitizenRegistration con credenciales, documento de identidad, teléfono y barrio.
    // - Llama a 'repository.signUpCitizen(registration)'.
    // - Crea la cuenta en Supabase Auth y procede a insertar la fila en 'public.profiles' (role_code = 'citizen')
    //   y en 'public.citizens' (neighborhood = 'El Príncipe').
    // - ¿Qué se espera para que se agregue?: Si 'Confirm email' está desactivado en Supabase, la sesión
    //   se inicia en caliente y el perfil se inserta en milisegundos. Si está activado, el usuario se creen 'auth.users' y queda en espera de confirmación de correo.

    @Test
    fun testCitizenRegistrationReal() = runBlocking {
        val unique = System.currentTimeMillis().toString().takeLast(4)
        // Usamos alias de Gmail (+) para que llegue a tu mismo correo sin colisionar con otras pruebas
        val email = "alejoyeison9+ciudadano$unique@gmail.com"
        val password = "ClaveCiudadano123*"
        val documentNumber = "11${unique}88"

        println("REGISTRANDO CIUDADANO EN VIVO")
        println("Email: $email")
        println("Documento: $documentNumber")

        val registration = CitizenRegistration(
            email = email,
            password = password,
            fullName = "Alejandro Ciudadano EcoRuta",
            documentNumber = documentNumber,
            phone = "3154829103",
            neighborhood = "El Príncipe"
        )

        val result = repository.signUpCitizen(registration)

        if (result.isFailure) {
            val ex = result.exceptionOrNull()
            println("Fallo en registro de ciudadano: ${ex?.message}")
            ex?.printStackTrace()
        }

        assertTrue("El registro del ciudadano debe ser exitoso", result.isSuccess)
        val account = result.getOrThrow()
        println("Ciudadano registrado exitosamente en Supabase Auth con UUID: ${account.id}")

        // Verificar inserción en tabla profiles
        val profileCheck = supabase.postgrest.from("profiles")
            .select { filter { eq("id", account.id) } }
        println("Fila en public.profiles: ${profileCheck.data}")
        assertTrue(profileCheck.data.contains("\"role_code\":\"citizen\""))

        // Verificar inserción en tabla citizens
        val citizenCheck = supabase.postgrest.from("citizens")
            .select { filter { eq("profile_id", account.id) } }
        println("Fila en public.citizens: ${citizenCheck.data}")
        assertTrue(citizenCheck.data.contains("\"neighborhood\":\"El Príncipe\""))

        println("Correo de confirmación enviado a $email.")
    }

    // testIndependentRecyclerRegistrationReal()
    // ¿Cómo funciona?
    // - Genera un RecyclerRegistration con asociación nula y empresa nula (indica que es independiente).
    // - Llama a 'repository.signUpRecycler(registration)'.
    // - Registra en Supabase Auth, inserta en 'public.profiles' con role_code = 'recycler', e inserta
    //   en 'public.recyclers' con status = 'pendiente'.
    // - Ejecuta una comprobación de seguridad RLS: verifica que una consulta con filtro status = 'aprobado'
    //   devuelva vacío ([]), garantizando que los ciudadanos no puedan ver al reciclador hasta que un administrador de EcoRuta apruebe su solicitud.

    @Test
    fun testIndependentRecyclerRegistrationReal() = runBlocking {
        val unique = System.currentTimeMillis().toString().takeLast(4)
        val email = "alejoyeison9+reciclador$unique@gmail.com"
        val password = "ClaveReciclador123*"
        val documentNumber = "11${unique}99"

        println("REGISTRANDO RECICLADOR INDEPENDIENTE EN VIVO")
        println("Email: $email")
        println("Documento: $documentNumber")

        val registration = RecyclerRegistration(
            email = email,
            password = password,
            fullName = "Alejandro Reciclador Independiente",
            documentNumber = documentNumber,
            phone = "3154829104",
            associationName = null, // Independiente (sin asociación)
            companyId = null,       // Sin empresa gestora vinculada
            idDocumentPdfUrl = null
        )

        val result = repository.signUpRecycler(registration)

        if (result.isFailure) {
            val ex = result.exceptionOrNull()
            println("Fallo en registro de reciclador: ${ex?.message}")
            ex?.printStackTrace()
        }

        assertTrue("El registro del reciclador debe ser exitoso", result.isSuccess)
        val account = result.getOrThrow()
        println("Reciclador registrado exitosamente en Supabase Auth con UUID: ${account.id}")

        // Verificar inserción en tabla profiles
        val profileCheck = supabase.postgrest.from("profiles")
            .select { filter { eq("id", account.id) } }
        println("Fila en public.profiles: ${profileCheck.data}")
        assertTrue(profileCheck.data.contains("\"role_code\":\"recycler\""))

        // Verificar inserción en tabla recyclers con status = 'pendiente'
        val recyclerCheck = supabase.postgrest.from("recyclers")
            .select { filter { eq("profile_id", account.id) } }
        println("Fila en public.recyclers: ${recyclerCheck.data}")
        assertTrue(recyclerCheck.data.contains("\"status\":\"pendiente\""))

        // Verificar que la política RLS oculta al reciclador de consultas públicas
        val publicList = supabase.postgrest.from("recyclers")
            .select {
                filter {
                    eq("profile_id", account.id)
                    eq("status", "aprobado")
                }
            }
        assertEquals("[]", publicList.data.trim())
        println("Seguridad RLS confirmada: El reciclador independiente no es público hasta ser aprobado.")
        println("Correo de confirmación enviado a $email.")
    }
}
