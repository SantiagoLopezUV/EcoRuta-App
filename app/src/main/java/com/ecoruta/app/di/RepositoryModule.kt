package com.ecoruta.app.di

// Módulo de inyección Dagger Hilt para vinculación de repositorios
// Asocia mediante @Binds cada interfaz abstracta con su clase concreta de datos

import com.ecoruta.app.features.auth.data.repository.AuthRepositoryImpl
import com.ecoruta.app.features.auth.domain.repository.AuthRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    // Vincula la interfaz de autenticación con la implementación de Supabase
    @Binds
    @Singleton
    abstract fun bindAuthRepository(
        impl: AuthRepositoryImpl
    ): AuthRepository
}

