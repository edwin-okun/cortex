package com.cortex.app.core.di

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import java.io.File
import androidx.room.Room
import com.cortex.app.core.database.CortexDatabase
import com.cortex.app.data.repository.ContentRepositoryImpl
import com.cortex.app.data.repository.ProgressRepositoryImpl
import com.cortex.app.data.repository.SchedulerRepositoryImpl
import com.cortex.app.data.store.SessionConfigStore
import com.cortex.app.domain.repository.ContentRepository
import com.cortex.app.domain.repository.ProgressRepository
import com.cortex.app.domain.repository.SchedulerRepository
import com.cortex.app.domain.usecase.BuildDailySessionUseCase
import com.cortex.app.domain.usecase.GetLessonUseCase
import com.cortex.app.feature.home.HomeViewModel
import com.cortex.app.feature.lesson.LessonViewModel
import com.cortex.app.feature.library.LibraryViewModel
import com.cortex.app.feature.progress.ProgressViewModel
import com.cortex.app.feature.review.ReviewViewModel
import com.cortex.app.feature.session.SessionViewModel
import kotlinx.datetime.Clock
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.dsl.viewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val appModule = module {
    // Database
    single {
        Room.databaseBuilder(androidContext(), CortexDatabase::class.java, "cortex.db")
            .addMigrations(CortexDatabase.MIGRATION_1_2)
            .build()
    }

    // DAOs
    single { get<CortexDatabase>().lessonProgressDao() }
    single { get<CortexDatabase>().practiceAttemptDao() }
    single { get<CortexDatabase>().reviewCardDao() }

    // DataStore
    single {
        PreferenceDataStoreFactory.create(
            produceFile = {
                File(androidContext().filesDir, "datastore/session_config.preferences_pb")
            },
        )
    }
    single<Clock> { Clock.System }

    // Repositories
    single<ContentRepository> { ContentRepositoryImpl() }
    single<ProgressRepository> { ProgressRepositoryImpl(get(), get()) }
    single<SchedulerRepository> { SchedulerRepositoryImpl(get(), get()) }

    // Stores
    single { SessionConfigStore(get()) }

    // Use cases
    single { GetLessonUseCase(get()) }
    single { BuildDailySessionUseCase(get(), get(), get(), get()) }

    // ViewModels
    viewModelOf(::HomeViewModel)
    viewModelOf(::LessonViewModel)
    viewModelOf(::LibraryViewModel)
    viewModelOf(::ReviewViewModel)
    viewModelOf(::ProgressViewModel)
    viewModelOf(::SessionViewModel)
}
