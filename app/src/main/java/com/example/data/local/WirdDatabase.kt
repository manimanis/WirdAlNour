package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.local.dao.AyahBookmarkDao
import com.example.data.local.dao.PlanConfigDao
import com.example.data.local.dao.SurahAyahDao
import com.example.data.local.dao.SurahPlanDao
import com.example.data.local.entity.AyahBookmarkEntity
import com.example.data.local.entity.PlanConfigEntity
import com.example.data.local.entity.SurahAyahEntity
import com.example.data.local.entity.SurahPlanEntity

@Database(
    entities = [
        SurahPlanEntity::class,
        PlanConfigEntity::class,
        SurahAyahEntity::class,
        AyahBookmarkEntity::class
    ],
    version = 5,
    exportSchema = false
)
abstract class WirdDatabase : RoomDatabase() {
    abstract fun surahPlanDao(): SurahPlanDao
    abstract fun planConfigDao(): PlanConfigDao
    abstract fun surahAyahDao(): SurahAyahDao
    abstract fun ayahBookmarkDao(): AyahBookmarkDao

    companion object {
        @Volatile
        private var instance: WirdDatabase? = null

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `surah_ayahs` (
                        `surahNumber` INTEGER NOT NULL,
                        `ayahNumber` INTEGER NOT NULL,
                        `arabicText` TEXT NOT NULL,
                        PRIMARY KEY(`surahNumber`, `ayahNumber`)
                    )
                    """.trimIndent()
                )
            }
        }

        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "ALTER TABLE `plan_config` ADD COLUMN `readerFontSizeSp` REAL NOT NULL DEFAULT 26.0"
                )
            }
        }

        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `ayah_bookmarks` (
                        `surahNumber` INTEGER NOT NULL,
                        `ayahNumber` INTEGER NOT NULL,
                        `surahArabicName` TEXT NOT NULL,
                        `ayahPreviewText` TEXT NOT NULL,
                        `createdAtMillis` INTEGER NOT NULL,
                        PRIMARY KEY(`surahNumber`, `ayahNumber`)
                    )
                    """.trimIndent()
                )
            }
        }

        private val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "ALTER TABLE `plan_config` ADD COLUMN `readerFontKey` TEXT NOT NULL DEFAULT 'amiri_quran'"
                )
            }
        }

        fun getInstance(context: Context): WirdDatabase {
            return instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    WirdDatabase::class.java,
                    "wird_al_noor.db"
                )
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5)
                    .build()
                    .also { instance = it }
            }
        }
    }
}
