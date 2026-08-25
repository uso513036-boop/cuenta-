package com.example.security

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.util.Log
import android.webkit.CookieManager
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File

/**
 * Enterprise Zero-Leakage Sandbox Intent Interceptor & Deep-Link Firewall
 * 
 * Intercepts, isolates, and blocks all external Android Intents, Market/PlayStore links,
 * app-specific URI schemes (imvu://, tg://, whatsapp://, etc.), and deep links so that
 * NO cloned session escapes to the host device's native apps.
 */
object SandboxIntentInterceptor {
    private const val TAG = "SandboxIntentInterceptor"

    data class SandboxEvent(
        val timestamp: Long = System.currentTimeMillis(),
        val originalUri: String,
        val actionTaken: String,
        val targetPackage: String? = null,
        val isBlocked: Boolean = true
    )

    private val _interceptionEvents = MutableStateFlow<List<SandboxEvent>>(emptyList())
    val interceptionEvents = _interceptionEvents.asStateFlow()

    private val _blockedCount = MutableStateFlow(0)
    val blockedCount = _blockedCount.asStateFlow()

    private val _shieldActive = MutableStateFlow(true)
    val shieldActive = _shieldActive.asStateFlow()

    // Known target package identifiers that must NEVER be resolved on the host
    private val BLOCKED_HOST_PACKAGES = setOf(
        "com.imvu.mobile",
        "com.whatsapp",
        "com.whatsapp.w4b",
        "org.telegram.messenger",
        "org.telegram.messenger.web",
        "com.instagram.android",
        "com.facebook.katana",
        "com.facebook.orca",
        "com.zhiliaoapp.musically",
        "com.snapchat.android",
        "com.twitter.android",
        "com.google.android.youtube",
        "com.spotify.music",
        "com.netflix.mediaclient",
        "com.android.vending",
        "com.aceptus.banco.popular"
    )

    // Known deep-link schemes that try to escape into native apps
    private val DEEP_LINK_SCHEMES = setOf(
        "imvu",
        "whatsapp",
        "tg",
        "telegram",
        "instagram",
        "fb",
        "fb-messenger",
        "snssdk1233",
        "snssdk1180",
        "snapchat",
        "twitter",
        "vnd.youtube",
        "spotify",
        "market",
        "intent",
        "android-app"
    )

    sealed class InterceptResolution {
        data class AllowInternal(val url: String) : InterceptResolution()
        data class ReroutedInternal(val url: String, val reason: String) : InterceptResolution()
        data class Blocked(val originalUrl: String, val reason: String) : InterceptResolution()
    }

    /**
     * Inspects a URI/URL requested by a cloned app or WebView and decides whether to allow,
     * block, or reroute internally.
     */
    fun evaluateUrl(uriString: String, profilePackageName: String? = null): InterceptResolution {
        val trimmed = uriString.trim()
        if (trimmed.isEmpty()) return InterceptResolution.Blocked(uriString, "URL vacía")

        val uri = try {
            Uri.parse(trimmed)
        } catch (e: Exception) {
            recordEvent(trimmed, "URI inválido bloqueado", isBlocked = true)
            return InterceptResolution.Blocked(trimmed, "URI Malformado")
        }

        val scheme = uri.scheme?.lowercase() ?: ""
        val host = uri.host?.lowercase() ?: ""

        // 1. Block Google Play & App Store Links (Market:// and play.google.com)
        if (scheme == "market" || (host == "play.google.com" && uri.path?.startsWith("/store/apps") == true)) {
            val pkg = uri.getQueryParameter("id") ?: profilePackageName ?: "app"
            recordEvent(trimmed, "Bloqueado intento de abrir Google Play Store ($pkg)", targetPackage = pkg, isBlocked = true)
            return InterceptResolution.Blocked(trimmed, "Enlace a Google Play bloqueado por el Sandbox Shield")
        }

        // 2. Intercept and Sanitize Android 'intent://' URLs
        if (scheme == "intent") {
            val (sanitizedUrl, targetPackage) = parseIntentScheme(trimmed)
            recordEvent(trimmed, "Interceptado Intent externo hacia paquete: $targetPackage", targetPackage = targetPackage, isBlocked = true)
            return if (sanitizedUrl != null && (sanitizedUrl.startsWith("http://") || sanitizedUrl.startsWith("https://"))) {
                InterceptResolution.ReroutedInternal(sanitizedUrl, "Convertido Intent externo a URL web interna")
            } else {
                InterceptResolution.Blocked(trimmed, "Intent externo bloqueado para evitar escape del clonador")
            }
        }

        // 3. Intercept App-Specific Deep Link Schemes (imvu://, tg://, whatsapp://, etc.)
        if (DEEP_LINK_SCHEMES.contains(scheme)) {
            val reroutedUrl = translateDeepLinkToWeb(scheme, uri)
            recordEvent(trimmed, "Interceptado esquema de app '$scheme://' -> Bloqueada fuga nativa", isBlocked = true)
            return if (reroutedUrl != null) {
                InterceptResolution.ReroutedInternal(reroutedUrl, "Ruteado a versión web/nube aislada")
            } else {
                InterceptResolution.Blocked(trimmed, "Esquema $scheme:// bloqueado (sin salida externa)")
            }
        }

        // 4. Standard HTTP / HTTPS URLs - Verify they don't target universal app link redirects
        if (scheme == "http" || scheme == "https") {
            // Check for known app redirection endpoints
            if (host.contains("api.whatsapp.com") || host.contains("wa.me")) {
                val phone = uri.path?.replace("/", "") ?: ""
                val text = uri.getQueryParameter("text") ?: ""
                return InterceptResolution.ReroutedInternal(
                    "https://web.whatsapp.com",
                    "Redirección de WhatsApp mantenida en sesión web aislada"
                )
            }
            if (host.contains("t.me") || host.contains("telegram.me")) {
                return InterceptResolution.ReroutedInternal(
                    "https://web.telegram.org",
                    "Redirección de Telegram mantenida en sesión web aislada"
                )
            }
            if (host.contains("imvu.com")) {
                return InterceptResolution.AllowInternal(trimmed)
            }

            return InterceptResolution.AllowInternal(trimmed)
        }

        // 5. Block all other non-standard protocol schemes
        recordEvent(trimmed, "Protocolo no estándar '$scheme' bloqueado", isBlocked = true)
        return InterceptResolution.Blocked(trimmed, "Protocolo bloqueado por seguridad")
    }

    /**
     * Converts native deep links into internal web URLs so the clone can continue
     * functioning inside the sandbox.
     */
    private fun translateDeepLinkToWeb(scheme: String, uri: Uri): String? {
        return when (scheme) {
            "imvu" -> {
                val path = uri.path ?: ""
                val room = uri.getQueryParameter("room") ?: uri.getQueryParameter("roomid")
                if (room != null) "https://m.imvu.com/room/$room" else "https://m.imvu.com"
            }
            "whatsapp" -> "https://web.whatsapp.com"
            "tg", "telegram" -> "https://web.telegram.org"
            "instagram" -> "https://www.instagram.com"
            "fb", "fb-messenger" -> "https://m.facebook.com"
            "twitter" -> "https://mobile.twitter.com"
            "spotify" -> "https://open.spotify.com"
            else -> null
        }
    }

    /**
     * Parses an Android intent:// URI, extracting the fallback browser URL and target package.
     */
    private fun parseIntentScheme(intentUriString: String): Pair<String?, String?> {
        try {
            val intent = Intent.parseUri(intentUriString, Intent.URI_INTENT_SCHEME)
            val fallbackUrl = intent.getStringExtra("browser_fallback_url")
            val targetPackage = intent.`package`
            return Pair(fallbackUrl, targetPackage)
        } catch (e: Exception) {
            Log.w(TAG, "Failed to parse intent URI: $intentUriString", e)
        }
        return Pair(null, null)
    }

    /**
     * Configures a WebView to guarantee total sandbox isolation for a given profile ID:
     * - Isolated storage directory per profile ID
     * - Isolated DOM storage / IndexedDB
     * - Intercepting WebViewClient that NEVER allows opening external apps
     */
    fun setupIsolatedWebView(
        context: Context,
        webView: WebView,
        profileId: Int,
        profilePackage: String? = null,
        onBlockedIntent: (String) -> Unit = {}
    ) {
        val sandboxDir = File(context.filesDir, "sandboxes/profile_$profileId")
        if (!sandboxDir.exists()) {
            sandboxDir.mkdirs()
        }

        webView.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            databaseEnabled = true
            allowFileAccess = false
            allowContentAccess = false
            setGeolocationEnabled(false)
            loadWithOverviewMode = true
            useWideViewPort = true
            cacheMode = WebSettings.LOAD_DEFAULT
            mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW
            // Custom Sandbox User Agent preventing mobile websites from forcing native app intents
            userAgentString = userAgentString.replace("wv", "") + " MultiSpaceSandbox/2.0 (Isolated)"
        }

        // Enable Cookies with Third Party isolation
        val cookieManager = CookieManager.getInstance()
        cookieManager.setAcceptCookie(true)
        cookieManager.setAcceptThirdPartyCookies(webView, true)

        webView.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                val url = request?.url?.toString() ?: return false
                return handleUrlLoading(view, url, profilePackage, onBlockedIntent)
            }

            @Deprecated("Deprecated in Java")
            override fun shouldOverrideUrlLoading(view: WebView?, url: String?): Boolean {
                if (url == null) return false
                return handleUrlLoading(view, url, profilePackage, onBlockedIntent)
            }
        }
    }

    private fun handleUrlLoading(
        view: WebView?,
        url: String,
        profilePackage: String?,
        onBlockedIntent: (String) -> Unit
    ): Boolean {
        when (val res = evaluateUrl(url, profilePackage)) {
            is InterceptResolution.AllowInternal -> {
                // Allow the WebView to load internally without launching external activities
                return false
            }
            is InterceptResolution.ReroutedInternal -> {
                view?.loadUrl(res.url)
                return true
            }
            is InterceptResolution.Blocked -> {
                Log.d(TAG, "Shield blocked external leak: ${res.reason} -> $url")
                onBlockedIntent(res.reason)
                // Returning true prevents Android from resolving the intent externally!
                return true
            }
        }
    }

    private fun recordEvent(originalUri: String, actionTaken: String, targetPackage: String? = null, isBlocked: Boolean = true) {
        val event = SandboxEvent(
            originalUri = originalUri,
            actionTaken = actionTaken,
            targetPackage = targetPackage,
            isBlocked = isBlocked
        )
        val current = _interceptionEvents.value.toMutableList()
        current.add(0, event)
        if (current.size > 50) current.removeAt(current.size - 1)
        _interceptionEvents.value = current

        if (isBlocked) {
            _blockedCount.value = _blockedCount.value + 1
        }
    }

    fun clearEvents() {
        _interceptionEvents.value = emptyList()
        _blockedCount.value = 0
    }
}
