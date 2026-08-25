package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "profiles")
data class ProfileEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val name: String,
    val appName: String,
    val iconKey: String,
    val badgeColor: Long,
    val targetUrl: String,
    val spaceCategory: String, // "Personal", "Trabajo", "Privado", "Finanzas", "Social", "Comunidad"
    val userAgentMode: String = "Mobile Android", // "Desktop Chrome", "Mobile Android", "Mobile iOS Safari", "Default"
    val isIncognito: Boolean = false,
    val isPinLocked: Boolean = false,
    val customPin: String? = null,
    val dataUsageBytes: Long = 0L,
    val cookieCount: Int = 0,
    val lastAccessedAt: Long = System.currentTimeMillis(),
    val createdAt: Long = System.currentTimeMillis(),
    val encryptedNotes: String = "",
    val isFavorite: Boolean = false,
    val desktopMode: Boolean = false,
    val adBlockEnabled: Boolean = true,
    val cookiesSnapshotJson: String = "",
    val packageName: String? = null,
    val launchMode: String = "APP_VIEW" // "APP_VIEW", "NATIVE_PACKAGE"
)
