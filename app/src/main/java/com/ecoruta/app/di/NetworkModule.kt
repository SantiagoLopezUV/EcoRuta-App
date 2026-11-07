package com.ecoruta.app.di

import com.ecoruta.app.core.network.supabase.supabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.functions.Functions
import io.github.jan.supabase.functions.functions
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.realtime.Realtime
import io.github.jan.supabase.realtime.realtime
import io.github.jan.supabase.storage.Storage
import io.github.jan.supabase.storage.storage
import javax.inject.Singleton

// Módulo de inyección Dagger Hilt para servicios de red
// Provee cliente de Supabase configurado con credenciales seguras y motor Ktor

@Module // nos ayuda a crear los objetos de supabase
@InstallIn(SingletonComponent::class) // instala estos objetos en el nivel más alto de la app para que vivan el tiempo que la app este en ejecución
object NetworkModule {

    // provee la instancia unica con singleton del cliente SupabaseClient
    @Provides
    @Singleton
    fun provideSupabaseClient(): SupabaseClient = supabase

    // provee el módulo de autenticación de supabase para la gestion de sesiones, JWT, registro, inicio de sesión y recuperación de contraseña
    @Provides
    @Singleton
    fun provideSupabaseAuth(client: SupabaseClient) : Auth = client.auth

    // provee el módulo PostgREST para las consultas a la base de datos PostgreSQL
    @Provides
    @Singleton
    fun provideSupabasePostgrest(client: SupabaseClient) : Postgrest = client.postgrest

    // provee el módulo Storage para la subida y descarga de fotos
    @Provides
    @Singleton
    fun provideSupabaseStorage(client: SupabaseClient) : Storage = client.storage

    // provee el RealTime con los WebSockets para la telemetría de los GPS en vivo y las alertas
    @Provides
    @Singleton
    fun provideSupabaseRealTime(client: SupabaseClient) : Realtime = client.realtime

    // provee el módulo de Functions, para poder llamar a las Edge Functions
    @Provides
    @Singleton
    fun provideFunctions(client: SupabaseClient) : Functions = client.functions
}