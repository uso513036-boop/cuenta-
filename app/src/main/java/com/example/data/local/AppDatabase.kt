package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(entities = [ProfileEntity::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun profileDao(): ProfileDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context, scope: CoroutineScope): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "multispace_database"
                )
                .addCallback(DatabaseCallback(scope))
                .build()
                INSTANCE = instance
                instance
            }
        }

        private class DatabaseCallback(
            private val scope: CoroutineScope
        ) : RoomDatabase.Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                INSTANCE?.let { database ->
                    scope.launch(Dispatchers.IO) {
                        populateInitialProfiles(database.profileDao())
                    }
                }
            }

            private suspend fun populateInitialProfiles(dao: ProfileDao) {
                val initialList = listOf(
                    ProfileEntity(
                        name = "WhatsApp Personal",
                        appName = "WhatsApp",
                        iconKey = "whatsapp",
                        badgeColor = 0xFF25D366,
                        targetUrl = "https://web.whatsapp.com",
                        spaceCategory = "Personal",
                        userAgentMode = "Desktop Chrome",
                        desktopMode = true,
                        isFavorite = true
                    ),
                    ProfileEntity(
                        name = "WhatsApp Trabajo",
                        appName = "WhatsApp",
                        iconKey = "whatsapp",
                        badgeColor = 0xFF128C7E,
                        targetUrl = "https://web.whatsapp.com",
                        spaceCategory = "Trabajo",
                        userAgentMode = "Desktop Chrome",
                        desktopMode = true,
                        isFavorite = true
                    ),
                    ProfileEntity(
                        name = "Telegram VIP / Alts",
                        appName = "Telegram",
                        iconKey = "telegram",
                        badgeColor = 0xFF2AABEE,
                        targetUrl = "https://web.telegram.org/k/",
                        spaceCategory = "Privado",
                        userAgentMode = "Mobile Android",
                        isFavorite = false
                    ),
                    ProfileEntity(
                        name = "Instagram Secundario",
                        appName = "Instagram",
                        iconKey = "instagram",
                        badgeColor = 0xFFE1306C,
                        targetUrl = "https://www.instagram.com/",
                        spaceCategory = "Social",
                        userAgentMode = "Mobile Android",
                        isFavorite = false
                    ),
                    ProfileEntity(
                        name = "X / Twitter Privado",
                        appName = "X (Twitter)",
                        iconKey = "twitter",
                        badgeColor = 0xFF1DA1F2,
                        targetUrl = "https://x.com/",
                        spaceCategory = "Privado",
                        userAgentMode = "Mobile Android",
                        isFavorite = false
                    ),
                    ProfileEntity(
                        name = "Discord Dev & Gaming",
                        appName = "Discord",
                        iconKey = "discord",
                        badgeColor = 0xFF5865F2,
                        targetUrl = "https://discord.com/app",
                        spaceCategory = "Comunidad",
                        userAgentMode = "Desktop Chrome",
                        desktopMode = true,
                        isFavorite = false
                    )
                )

                for (profile in initialList) {
                    dao.insertProfile(profile)
                }
            }
        }
    }
}
