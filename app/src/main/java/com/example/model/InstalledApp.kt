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
    val suggestedUrl: String = "",
    val suggestedCategory: String = "Personal",
    val suggestedColor: Long = 0xFF06B6D4,
    val suggestedIconKey: String = "apps",
    val recommendedDesktopUA: Boolean = false
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

            val (url, category, color, iconKey, desktop) = getPresetMetadataForPackage(pkg, label)

            results.add(
                InstalledApp(
                    appName = label,
                    packageName = pkg,
                    icon = iconDrawable,
                    isSystemApp = isSystem,
                    suggestedUrl = url,
                    suggestedCategory = category,
                    suggestedColor = color,
                    suggestedIconKey = iconKey,
                    recommendedDesktopUA = desktop
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
                url = "https://web.whatsapp.com",
                category = "Mensajería",
                color = 0xFF25D366,
                iconKey = "whatsapp",
                desktopMode = true
            )
            pkg.contains("telegram") || name.contains("telegram") -> PackageMetadata(
                url = "https://web.telegram.org/k/",
                category = "Mensajería",
                color = 0xFF2AABEE,
                iconKey = "telegram",
                desktopMode = false
            )
            pkg.contains("instagram") || name.contains("instagram") -> PackageMetadata(
                url = "https://www.instagram.com/",
                category = "Social",
                color = 0xFFE1306C,
                iconKey = "instagram",
                desktopMode = false
            )
            pkg.contains("twitter") || pkg.contains(".x.") || name.contains("twitter") || name == "x" -> PackageMetadata(
                url = "https://x.com/",
                category = "Social",
                color = 0xFF1DA1F2,
                iconKey = "twitter",
                desktopMode = false
            )
            pkg.contains("facebook") || pkg.contains("katana") || name.contains("facebook") -> PackageMetadata(
                url = "https://m.facebook.com/",
                category = "Social",
                color = 0xFF1877F2,
                iconKey = "facebook",
                desktopMode = false
            )
            pkg.contains("tiktok") || name.contains("tiktok") || pkg.contains("musical") -> PackageMetadata(
                url = "https://www.tiktok.com/",
                category = "Social",
                color = 0xFFEE1D52,
                iconKey = "tiktok",
                desktopMode = false
            )
            pkg.contains("discord") || name.contains("discord") -> PackageMetadata(
                url = "https://discord.com/app",
                category = "Comunidad",
                color = 0xFF5865F2,
                iconKey = "discord",
                desktopMode = true
            )
            pkg.contains("slack") || name.contains("slack") -> PackageMetadata(
                url = "https://app.slack.com/client",
                category = "Trabajo",
                color = 0xFF4A154B,
                iconKey = "slack",
                desktopMode = true
            )
            pkg.contains("spotify") || name.contains("spotify") -> PackageMetadata(
                url = "https://open.spotify.com/",
                category = "Social",
                color = 0xFF1DB954,
                iconKey = "spotify",
                desktopMode = false
            )
            pkg.contains("reddit") || name.contains("reddit") -> PackageMetadata(
                url = "https://www.reddit.com/",
                category = "Comunidad",
                color = 0xFFFF4500,
                iconKey = "reddit",
                desktopMode = false
            )
            pkg.contains("linkedin") || name.contains("linkedin") -> PackageMetadata(
                url = "https://www.linkedin.com/",
                category = "Trabajo",
                color = 0xFF0A66C2,
                iconKey = "linkedin",
                desktopMode = false
            )
            pkg.contains("youtube") || name.contains("youtube") -> PackageMetadata(
                url = "https://m.youtube.com/",
                category = "Social",
                color = 0xFFFF0000,
                iconKey = "videocam",
                desktopMode = false
            )
            pkg.contains("netflix") || name.contains("netflix") -> PackageMetadata(
                url = "https://www.netflix.com/",
                category = "Personal",
                color = 0xFFE50914,
                iconKey = "videocam",
                desktopMode = false
            )
            pkg.contains("notion") || name.contains("notion") -> PackageMetadata(
                url = "https://www.notion.so/",
                category = "Trabajo",
                color = 0xFF2F3437,
                iconKey = "notion",
                desktopMode = true
            )
            pkg.contains("github") || name.contains("github") -> PackageMetadata(
                url = "https://github.com/login",
                category = "Trabajo",
                color = 0xFF6E5494,
                iconKey = "github",
                desktopMode = false
            )
            pkg.contains("binance") || name.contains("binance") || pkg.contains("crypto") || name.contains("crypto") -> PackageMetadata(
                url = "https://www.binance.com/",
                category = "Finanzas",
                color = 0xFFF0B90B,
                iconKey = "crypto",
                desktopMode = true
            )
            pkg.contains("gmail") || pkg.contains("google.android.gm") || name.contains("gmail") -> PackageMetadata(
                url = "https://mail.google.com/",
                category = "Trabajo",
                color = 0xFFEA4335,
                iconKey = "email",
                desktopMode = false
            )
            pkg.contains("bank") || pkg.contains("banc") || name.contains("banc") || name.contains("bank") -> PackageMetadata(
                url = "https://www.google.com/search?q=${label.replace(" ", "+")}",
                category = "Finanzas",
                color = 0xFF10B981,
                iconKey = "crypto",
                desktopMode = false
            )
            else -> PackageMetadata(
                url = "https://www.google.com/search?q=${label.replace(" ", "+")}",
                category = "Personal",
                color = 0xFF06B6D4,
                iconKey = "apps",
                desktopMode = false
            )
        }
    }

    private data class PackageMetadata(
        val url: String,
        val category: String,
        val color: Long,
        val iconKey: String,
        val desktopMode: Boolean
    )
}
