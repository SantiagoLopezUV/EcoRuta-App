package com.ecoruta.app.core.testing

// Regla de JUnit para pruebas unitarias de corrutinas en ViewModels
// Sustituye Dispatchers.Main por TestDispatcher para ejecución sincrónica en la JVM

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.rules.TestWatcher
import org.junit.runner.Description

@OptIn(ExperimentalCoroutinesApi::class)
class MainDispatcherRule(
    val testDispatcher: TestDispatcher = StandardTestDispatcher()
) : TestWatcher() {

    // Antes de cada prueba el hilo principal pasa a usar el dispatcher de prueba
    override fun starting(description: Description) {
        Dispatchers.setMain(testDispatcher)
    }

    // Al terminar cada prueba se devuelve el hilo principal original
    override fun finished(description: Description) {
        Dispatchers.resetMain()
    }
}
