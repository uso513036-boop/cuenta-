package com.example.model

import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable

data class InstalledApp(
    val appName: String,
    val packageName: String,
    val icon: Drawable? = null,
    val isSystemApp: Boolean = false,
    val suggestedCategory: String = "Personal",
    val suggestedColor: Long = 0xFF06B6D4,
    val suggestedIconKey: String = "apps",
    val apkSizeMb: Double = 35.0,
    val versionName: String = "1.0.0"
)

object InstalledAppScanner {
    fun scanInstalledApps(context: Context): List<InstalledApp> {
        val pm = context.packageManager
        val intent = Intent(Intent.ACTION_MAIN, null).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
        }

        val launchableActivities = try {
            pm.queryIntentActivities(intent, 0)
        } catch (e: Exception) {
            emptyList()
        }

        val results = mutableListOf<InstalledApp>()
        val seenPackages = mutableSetOf<String>()

        for (resolveInfo in launchableActivities) {
            val pkg = resolveInfo.activityInfo.packageName
            if (pkg == context.packageName) continue // Skip our own app
            if (seenPackages.contains(pkg)) continue
            seenPackages.add(pkg)

            val label = try {
                resolveInfo.loadLabel(pm).toString()
            } catch (e: Exception) {
                pkg
            }

            val iconDrawable = try {
                resolveInfo.loadIcon(pm)
            } catch (e: Exception) {
                null
            }

            val isSystem = try {
                val appInfo = pm.getApplicationInfo(pkg, 0)
                (appInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0
            } catch (e: Exception) {
                false
            }

            val (category, color, iconKey) = getPresetMetadataForPackage(pkg, label)

            val (apkSize, version) = try {
                val pkgInfo = pm.getPackageInfo(pkg, 0)
                val sourceDir = pkgInfo.applicationInfo?.sourceDir
                val file = if (sourceDir != null) java.io.File(sourceDir) else null
                val sizeMb = if (file != null && file.exists()) file.length() / (1024.0 * 1024.0) else 42.0
                Pair(if (sizeMb > 0) sizeMb else 35.0, pkgInfo.versionName ?: "1.0.0")
            } catch (e: Exception) {
                Pair(45.0, "1.0")
            }

            results.add(
                InstalledApp(
                    appName = label,
                    packageName = pkg,
                    icon = iconDrawable,
                    isSystemApp = isSystem,
                    suggestedCategory = category,
                    suggestedColor = color,
                    suggestedIconKey = iconKey,
                    apkSizeMb = apkSize,
                    versionName = version
                )
            )
        }

        // Sort non-system apps first, then alphabetically
        return results.sortedWith(
            compareBy<InstalledApp> { it.isSystemApp }
                .thenBy { it.appName.lowercase() }
        )
    }

    private fun getPresetMetadataForPackage(
        packageName: String,
        label: String
    ): PackageMetadata {
        val pkg = packageName.lowercase()
        val name = label.lowercase()

        return when {
            pkg.contains("whatsapp") || name.contains("whatsapp") -> PackageMetadata(
                category = "Mensajería",
                color = 0xFF25D366,
                iconKey = "whatsapp"
            )
            pkg.contains("telegram") || name.contains("telegram") -> PackageMetadata(
                category = "Mensajería",
                color = 0xFF2AABEE,
                iconKey = "telegram"
            )
            pkg.contains("imvu") || name.contains("imvu") -> PackageMetadata(
                category = "Social",
                color = 0xFF00B4D8,
                iconKey = "apps"
            )
            pkg.contains("instagram") || name.contains("instagram") -> PackageMetadata(
                category = "Social",
                color = 0xFFE1306C,
                iconKey = "instagram"
            )
            pkg.contains("twitter") || pkg.contains(".x.") || name.contains("twitter") || name == "x" -> PackageMetadata(
                category = "Social",
                color = 0xFF1DA1F2,
                iconKey = "twitter"
            )
            pkg.contains("facebook") || pkg.contains("katana") || name.contains("facebook") -> PackageMetadata(
                category = "Social",
                color = 0xFF1877F2,
                iconKey = "facebook"
            )
            pkg.contains("tiktok") || name.contains("tiktok") || pkg.contains("musical") -> PackageMetadata(
                category = "Social",
                color = 0xFFEE1D52,
                iconKey = "tiktok"
            )
            pkg.contains("discord") || name.contains("discord") -> PackageMetadata(
                category = "Comunidad",
                color = 0xFF5865F2,
                iconKey = "discord"
            )
            pkg.contains("spotify") || name.contains("spotify") -> PackageMetadata(
                category = "Social",
                color = 0xFF1DB954,
                iconKey = "spotify"
            )
            pkg.contains("reddit") || name.contains("reddit") -> PackageMetadata(
                category = "Comunidad",
                color = 0xFFFF4500,
                iconKey = "reddit"
            )
            pkg.contains("youtube") || name.contains("youtube") -> PackageMetadata(
                category = "Social",
                color = 0xFFFF0000,
                iconKey = "videocam"
            )
            pkg.contains("gmail") || pkg.contains("google.android.gm") || name.contains("gmail") -> PackageMetadata(
                category = "Trabajo",
                color = 0xFFEA4335,
                iconKey = "email"
            )
            else -> PackageMetadata(
                category = "Personal",
                color = 0xFF06B6D4,
                iconKey = "apps"
            )
        }
    }

    private data class PackageMetadata(
        val category: String,
        val color: Long,
        val iconKey: String
    )
}
