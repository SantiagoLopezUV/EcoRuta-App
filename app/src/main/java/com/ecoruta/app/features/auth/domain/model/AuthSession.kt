package com.ecoruta.app.features.auth.domain.model

// Modelo de dominio para representar la sesión activa
// Modela estados: Authenticated (usuario y rol activo) o Unauthenticated (visitante)

//Clases e interfaces selladas
//Las clases e interfaces selladas permiten controlar la herencia de las jerarquías de clases.
// Todas las subclases directas de una clase sellada se conocen en tiempo de compilación.
// No pueden aparecer otras subclases fuera del módulo y del paquete en los que se define la clase sellada.
// La misma lógica se aplica a las interfaces selladas y sus implementaciones: una vez que se compila un módulo con una interfaz sellada, no se pueden crear nuevas implementaciones.

//Clase sellada para modelar los 3 estados posibles de la aplicación

sealed interface AuthSession {
    // data object para los estados que no transportan datos
    // esto genera automaticamente implementaciones limpias de toString() equals() y hashCode()

    //Estado inicial mientras se recupera la sesión guardada en el dispositvo
    data object Loading : AuthSession
    //  Sin sesión activa, la aplicación opera en modo visitante para hacer consultas
    data object Unauthenticated : AuthSession

    //data class genera automaticamente la comparacion equals()
    //Se usa porque el estado lleva datos
    //si el usuario no ha cambiado, compose no redibuja sin necesidad
    // Sesión activa con los datos del usuario autenticado
    data class Authenticated(val user: UserAccount) : AuthSession
}