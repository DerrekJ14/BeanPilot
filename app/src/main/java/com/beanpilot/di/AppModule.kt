package com.beanpilot.di

import android.content.Context
import androidx.room.Room
import com.beanpilot.data.local.AppDatabase
import com.beanpilot.data.local.AppDatabase.Companion.MIGRATION_1_2
import com.beanpilot.data.local.AppDatabase.Companion.MIGRATION_2_3
import com.beanpilot.data.local.AppDatabase.Companion.MIGRATION_3_4
import com.beanpilot.data.local.AppDatabase.Companion.MIGRATION_4_5
import com.beanpilot.data.local.dao.CoffeeBeanDao
import com.beanpilot.data.local.dao.ShotDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.websocket.WebSockets
import io.ktor.serialization.kotlinx.json.json
import javax.inject.Singleton
import kotlinx.serialization.json.Json
import kotlinx.serialization.ExperimentalSerializationApi

@Module
@InstallIn(SingletonComponent::class)
object AppModule {
    @Provides
    @Singleton
    @OptIn(ExperimentalSerializationApi::class)
    fun provideJson(): Json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        explicitNulls = false
    }

    @Provides
    @Singleton
    fun provideHttpClient(json: Json): HttpClient = HttpClient(CIO) {
        install(WebSockets)
        install(ContentNegotiation) {
            json(json)
        }
    }

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase =
        Room.databaseBuilder(context, AppDatabase::class.java, "beanpilot-db")
            .addMigrations(MIGRATION_1_2)
            .addMigrations(MIGRATION_2_3)
            .addMigrations(MIGRATION_3_4)
            .addMigrations(MIGRATION_4_5)
            .build()

    @Provides
    @Singleton
    fun provideCoffeeBeanDao(database: AppDatabase): CoffeeBeanDao = database.coffeeBeanDao()

    @Provides
    @Singleton
    fun provideShotDao(database: AppDatabase): ShotDao = database.shotDao()
}
