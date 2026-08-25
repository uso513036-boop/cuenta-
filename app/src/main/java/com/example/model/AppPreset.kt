package com.example.model

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector

data class AppPreset(
    val name: String,
    val defaultUrl: String,
    val category: String,
    val defaultColor: Long,
    val iconKey: String,
    val recommendedDesktopUA: Boolean = false,
    val description: String
)

object AppCatalog {
    val PRESETS = listOf(
        AppPreset(
            name = "WhatsApp",
            defaultUrl = "https://web.whatsapp.com",
            category = "Mensajería",
            defaultColor = 0xFF25D366,
            iconKey = "whatsapp",
            recommendedDesktopUA = true,
            description = "WhatsApp Web con sesión aislada e independiente"
        ),
        AppPreset(
            name = "Telegram",
            defaultUrl = "https://web.telegram.org/k/",
            category = "Mensajería",
            defaultColor = 0xFF2AABEE,
            iconKey = "telegram",
            recommendedDesktopUA = false,
            description = "Telegram Web cliente K ultra rápido"
        ),
        AppPreset(
            name = "Instagram",
            defaultUrl = "https://www.instagram.com/",
            category = "Social",
            defaultColor = 0xFFE1306C,
            iconKey = "instagram",
            recommendedDesktopUA = false,
            description = "Feed, DMs e historias de cuenta secundaria"
        ),
        AppPreset(
            name = "X (Twitter)",
            defaultUrl = "https://x.com/",
            category = "Social",
            defaultColor = 0xFF1DA1F2,
            iconKey = "twitter",
            recommendedDesktopUA = false,
            description = "Gestiona múltiples perfiles de X sin vincular"
        ),
        AppPreset(
            name = "Discord",
            defaultUrl = "https://discord.com/app",
            category = "Comunidad",
            defaultColor = 0xFF5865F2,
            iconKey = "discord",
            recommendedDesktopUA = true,
            description = "Servidores y canales en espacio aislado"
        ),
        AppPreset(
            name = "TikTok",
            defaultUrl = "https://www.tiktok.com/",
            category = "Social",
            defaultColor = 0xFFEE1D52,
            iconKey = "tiktok",
            recommendedDesktopUA = false,
            description = "Feed de contenido y cuenta alterna privada"
        ),
        AppPreset(
            name = "Facebook",
            defaultUrl = "https://m.facebook.com/",
            category = "Social",
            defaultColor = 0xFF1877F2,
            iconKey = "facebook",
            recommendedDesktopUA = false,
            description = "Facebook y Messenger para negocios o personal"
        ),
        AppPreset(
            name = "Gmail / Google",
            defaultUrl = "https://mail.google.com/",
            category = "Trabajo",
            defaultColor = 0xFFEA4335,
            iconKey = "email",
            recommendedDesktopUA = false,
            description = "Bandeja de correo corporativa o secundaria"
        ),
        AppPreset(
            name = "Reddit",
            defaultUrl = "https://www.reddit.com/",
            category = "Comunidad",
            defaultColor = 0xFFFF4500,
            iconKey = "reddit",
            recommendedDesktopUA = false,
            description = "Comunidades y subreddits anónimos"
        ),
        AppPreset(
            name = "Slack",
            defaultUrl = "https://app.slack.com/client",
            category = "Trabajo",
            defaultColor = 0xFF4A154B,
            iconKey = "slack",
            recommendedDesktopUA = true,
            description = "Espacio de trabajo para equipos o clientes"
        ),
        AppPreset(
            name = "Notion",
            defaultUrl = "https://www.notion.so/",
            category = "Trabajo",
            defaultColor = 0xFF2F3437,
            iconKey = "notion",
            recommendedDesktopUA = true,
            description = "Notas, wikis y gestión de proyectos aislados"
        ),
        AppPreset(
            name = "GitHub",
            defaultUrl = "https://github.com/login",
            category = "Desarrollo",
            defaultColor = 0xFF6E5494,
            iconKey = "github",
            recommendedDesktopUA = false,
            description = "Cuenta de desarrollo secundaria u organizativa"
        ),
        AppPreset(
            name = "LinkedIn",
            defaultUrl = "https://www.linkedin.com/",
            category = "Trabajo",
            defaultColor = 0xFF0A66C2,
            iconKey = "linkedin",
            recommendedDesktopUA = false,
            description = "Perfil profesional o reclutador"
        ),
        AppPreset(
            name = "Binance / Crypto",
            defaultUrl = "https://www.binance.com/",
            category = "Finanzas",
            defaultColor = 0xFFF0B90B,
            iconKey = "crypto",
            recommendedDesktopUA = true,
            description = "Operaciones y billeteras en contenedor cifrado"
        ),
        AppPreset(
            name = "Spotify",
            defaultUrl = "https://open.spotify.com/",
            category = "Entretenimiento",
            defaultColor = 0xFF1DB954,
            iconKey = "spotify",
            recommendedDesktopUA = false,
            description = "Reproductor web para cuentas familiares o alts"
        ),
        AppPreset(
            name = "Cualquier Web / App",
            defaultUrl = "https://",
            category = "Personalizado",
            defaultColor = 0xFF06B6D4,
            iconKey = "custom",
            recommendedDesktopUA = false,
            description = "Clona cualquier URL o servicio web con aislamiento total"
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
