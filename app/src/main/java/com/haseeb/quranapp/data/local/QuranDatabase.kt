package com.haseeb.quranapp.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.haseeb.quranapp.data.local.dao.QuranDao
import com.haseeb.quranapp.data.local.entity.AyahEntity

import com.haseeb.quranapp.data.local.entity.SurahEntity

@Database(entities = [AyahEntity::class, SurahEntity::class], version = 2, exportSchema = false)
abstract class QuranDatabase : RoomDatabase() {
    abstract fun quranDao(): QuranDao
}
