package com.haseeb.quranapp.di

import com.haseeb.quranapp.data.repository.QuranRepositoryImpl
import com.haseeb.quranapp.domain.repository.QuranRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindQuranRepository(
        impl: QuranRepositoryImpl
    ): QuranRepository
}
