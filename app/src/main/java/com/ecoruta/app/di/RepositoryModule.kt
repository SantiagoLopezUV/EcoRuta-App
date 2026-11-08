package com.ecoruta.app.di

// Módulo de inyección Dagger Hilt para vinculación de repositorios
// Asocia mediante @Binds cada interfaz abstracta con su clase concreta de datos

import com.ecoruta.app.features.auth.data.repository.AuthRepositoryImpl
import com.ecoruta.app.features.auth.domain.repository.AuthRepository
import com.ecoruta.app.features.profile.data.repository.ProfileRepositoryImpl
import com.ecoruta.app.features.profile.domain.repository.ProfileRepository
import com.ecoruta.app.features.reports.data.repository.ReportRepositoryImpl
import com.ecoruta.app.features.reports.domain.repository.ReportRepository
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

    // Vincula la interfaz de perfil y roles con la implementación de Supabase
    @Binds
    @Singleton
    abstract fun bindProfileRepository(
        impl: ProfileRepositoryImpl
    ): ProfileRepository

    // Vincula la interfaz de reportes ciudadanos con la implementación de Supabase
    @Binds
    @Singleton
    abstract fun bindReportRepository(
        impl: ReportRepositoryImpl
    ): ReportRepository
}
