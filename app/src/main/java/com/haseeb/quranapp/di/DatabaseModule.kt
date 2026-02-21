package com.haseeb.quranapp.di

import android.content.Context
import androidx.room.Room
import com.haseeb.quranapp.data.local.QuranDatabase
import com.haseeb.quranapp.data.local.dao.QuranDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideQuranDatabase(@ApplicationContext context: Context): QuranDatabase {
        return Room.databaseBuilder(
            context,
            QuranDatabase::class.java,
            "quran_db"
        ).fallbackToDestructiveMigration()
         .build()
    }

    @Provides
    @Singleton
    fun provideQuranDao(database: QuranDatabase): QuranDao {
        return database.quranDao()
    }
}
