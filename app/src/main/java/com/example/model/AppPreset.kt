package com.example.model

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector

data class AppPreset(
    val name: String,
    val packageName: String,
    val category: String,
    val defaultColor: Long,
    val iconKey: String,
    val description: String,
    val defaultUrl: String = ""
)

object AppCatalog {
    val PRESETS = listOf(
        AppPreset(
            name = "WhatsApp",
            packageName = "com.whatsapp",
            category = "Mensajería",
            defaultColor = 0xFF25D366,
            iconKey = "whatsapp",
            description = "Clon nativo de WhatsApp con número de teléfono independiente"
        ),
        AppPreset(
            name = "IMVU 3D",
            packageName = "com.imvu.mobile",
            category = "Social",
            defaultColor = 0xFF00B4D8,
            iconKey = "apps",
            description = "Clon de IMVU con avatares 3D, salas y chat virtual"
        ),
        AppPreset(
            name = "Telegram",
            packageName = "org.telegram.messenger",
            category = "Mensajería",
            defaultColor = 0xFF2AABEE,
            iconKey = "telegram",
            description = "Cliente nativo de Telegram con inicio de sesión por SMS"
        ),
        AppPreset(
            name = "Instagram",
            packageName = "com.instagram.android",
            category = "Social",
            defaultColor = 0xFFE1306C,
            iconKey = "instagram",
            description = "Cuenta secundaria nativa de Instagram con DMs e historias"
        ),
        AppPreset(
            name = "TikTok",
            packageName = "com.zhiliaoapp.musically",
            category = "Social",
            defaultColor = 0xFFEE1D52,
            iconKey = "tiktok",
            description = "Feed y perfil alternativo en contenedor independiente"
        ),
        AppPreset(
            name = "Facebook",
            packageName = "com.facebook.katana",
            category = "Social",
            defaultColor = 0xFF1877F2,
            iconKey = "facebook",
            description = "Facebook y Messenger en espacio aislado para multi-cuenta"
        ),
        AppPreset(
            name = "Discord",
            packageName = "com.discord",
            category = "Comunidad",
            defaultColor = 0xFF5865F2,
            iconKey = "discord",
            description = "Servidores y canales con inicio de sesión separado"
        ),
        AppPreset(
            name = "X (Twitter)",
            packageName = "com.twitter.android",
            category = "Social",
            defaultColor = 0xFF1DA1F2,
            iconKey = "twitter",
            description = "Perfil de X en sandbox con almacenamiento propio"
        ),
        AppPreset(
            name = "Reddit",
            packageName = "com.reddit.frontpage",
            category = "Comunidad",
            defaultColor = 0xFFFF4500,
            iconKey = "reddit",
            description = "Comunidades y cuenta secundaria anónima"
        ),
        AppPreset(
            name = "Snapchat",
            packageName = "com.snapchat.android",
            category = "Social",
            defaultColor = 0xFFFFFC00,
            iconKey = "camera",
            description = "Snaps, cámara y mensajes en espacio virtualizado"
        ),
        AppPreset(
            name = "Clon APK Personalizado",
            packageName = "com.custom.app",
            category = "Personalizado",
            defaultColor = 0xFF06B6D4,
            iconKey = "custom",
            description = "Clona cualquier paquete APK instalado en tu teléfono"
        )
    )

    fun getIconForKey(key: String): ImageVector {
        return when (key.lowercase()) {
            "whatsapp", "chat", "message" -> Icons.Default.Chat
            "telegram", "send" -> Icons.Default.Send
            "instagram", "camera" -> Icons.Default.PhotoCamera
            "twitter", "x" -> Icons.Default.Share
            "discord", "forum" -> Icons.Default.Forum
            "tiktok", "videocam" -> Icons.Default.Videocam
            "facebook", "people" -> Icons.Default.People
            "email", "gmail", "mail" -> Icons.Default.Email
            "reddit", "bookmark" -> Icons.Default.Bookmark
            "slack", "business" -> Icons.Default.Business
            "notion", "note", "edit" -> Icons.Default.EditNote
            "github", "code" -> Icons.Default.Code
            "linkedin", "work" -> Icons.Default.Work
            "crypto", "account_balance_wallet", "wallet" -> Icons.Default.AccountBalanceWallet
            "spotify", "music" -> Icons.Default.MusicNote
            "lock", "shield" -> Icons.Default.Security
            else -> Icons.Default.Apps
        }
    }
}
