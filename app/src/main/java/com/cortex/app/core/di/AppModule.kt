package com.cortex.app.core.di

import androidx.room.Room
import com.cortex.app.core.database.CortexDatabase
import com.cortex.app.data.repository.ContentRepositoryImpl
import com.cortex.app.data.repository.ProgressRepositoryImpl
import com.cortex.app.data.repository.SchedulerRepositoryImpl
import com.cortex.app.domain.repository.ContentRepository
import com.cortex.app.domain.repository.ProgressRepository
import com.cortex.app.domain.repository.SchedulerRepository
import com.cortex.app.domain.usecase.GetLessonUseCase
import com.cortex.app.feature.home.HomeViewModel
import com.cortex.app.feature.lesson.LessonViewModel
import com.cortex.app.feature.library.LibraryViewModel
import com.cortex.app.feature.progress.ProgressViewModel
import com.cortex.app.feature.review.ReviewViewModel
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

    // Repositories
    single<ContentRepository> { ContentRepositoryImpl() }
    single<ProgressRepository> { ProgressRepositoryImpl(get(), get()) }
    single<SchedulerRepository> { SchedulerRepositoryImpl(get()) }

    // Use cases
    single { GetLessonUseCase(get()) }

    // ViewModels
    viewModelOf(::HomeViewModel)
    viewModelOf(::LessonViewModel)
    viewModelOf(::LibraryViewModel)
    viewModelOf(::ReviewViewModel)
    viewModelOf(::ProgressViewModel)
}
