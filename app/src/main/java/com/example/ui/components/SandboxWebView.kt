package com.example.ui.components

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.net.http.SslError
import android.os.Build
import android.view.ViewGroup
import android.webkit.*
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.data.local.ProfileEntity
import com.example.model.AppCatalog
import com.example.util.SystemDualAppsLauncher

private const val DESKTOP_USER_AGENT =
    "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36"
private const val MOBILE_ANDROID_USER_AGENT =
    "Mozilla/5.0 (Linux; Android 14; Mobile; rv:125.0) Gecko/125.0 Firefox/125.0"
private const val MOBILE_CHROME_USER_AGENT =
    "Mozilla/5.0 (Linux; Android 14; Pixel 8 Pro) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Mobile Safari/537.36"

@SuppressLint("SetJavaScriptEnabled")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SandboxWebView(
    profile: ProfileEntity,
    modifier: Modifier = Modifier,
    onTitleChanged: (String) -> Unit = {},
    onStatsUpdated: (cookieCount: Int, dataUsage: Long) -> Unit = { _, _ -> },
    onCloseSandbox: () -> Unit = {}
) {
    val context = LocalContext.current
    var webViewInstance by remember { mutableStateOf<WebView?>(null) }
    var currentUrl by remember { mutableStateOf(profile.targetUrl) }
    var pageTitle by remember { mutableStateOf(profile.name) }
    var isLoading by remember { mutableStateOf(true) }
    var progress by remember { mutableFloatStateOf(0f) }
    var canGoBack by remember { mutableStateOf(false) }
    var isDesktopMode by remember { mutableStateOf(profile.desktopMode) }
    var showMoreMenu by remember { mutableStateOf(false) }
    var showUrlBar by remember { mutableStateOf(false) }
    var inputUrlText by remember { mutableStateOf(profile.targetUrl) }
    var isFullscreenAppMode by remember { mutableStateOf(false) }

    // Resolve User-Agent based on preference
    val selectedUserAgent = remember(isDesktopMode, profile.userAgentMode) {
        if (isDesktopMode) {
            DESKTOP_USER_AGENT
        } else {
            when (profile.userAgentMode) {
                "Desktop Chrome" -> DESKTOP_USER_AGENT
                "Mobile Android" -> MOBILE_CHROME_USER_AGENT
                else -> MOBILE_CHROME_USER_AGENT
            }
        }
    }

    Column(modifier = modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        // Native App Header (Clean, minimal, without browser URL bar)
        if (!isFullscreenAppMode) {
            Surface(
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 2.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Back to Hub / Close App
                        IconButton(
                            onClick = {
                                if (webViewInstance?.canGoBack() == true) {
                                    webViewInstance?.goBack()
                                } else {
                                    onCloseSandbox()
                                }
                            },
                            modifier = Modifier.size(38.dp).testTag("sandbox_back_btn")
                        ) {
                            Icon(
                                imageVector = Icons.Default.ArrowBack,
                                contentDescription = "Atrás / Salir",
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        Spacer(modifier = Modifier.width(4.dp))

                        // App Icon + Profile Badge
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(profile.badgeColor)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = AppCatalog.getIconForKey(profile.iconKey),
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(10.dp))

                        // Profile Title & Category (Native App Title)
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = profile.name,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Surface(
                                    color = Color(profile.badgeColor).copy(alpha = 0.15f),
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Text(
                                        text = "Clon Aislado",
                                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                        color = Color(profile.badgeColor),
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                    )
                                }
                            }
                            Text(
                                text = profile.spaceCategory,
                                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1
                            )
                        }

                        // Reload
                        IconButton(
                            onClick = { webViewInstance?.reload() },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Recargar",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        // More options menu
                        Box {
                            IconButton(
                                onClick = { showMoreMenu = true },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.MoreVert,
                                    contentDescription = "Opciones de App",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            DropdownMenu(
                                expanded = showMoreMenu,
                                onDismissRequest = { showMoreMenu = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text("Pantalla Completa Inmersiva") },
                                    onClick = {
                                        isFullscreenAppMode = true
                                        showMoreMenu = false
                                    },
                                    leadingIcon = { Icon(Icons.Default.Fullscreen, contentDescription = null) }
                                )

                                if (!profile.packageName.isNullOrEmpty()) {
                                    DropdownMenuItem(
                                        text = { Text("Abrir App del Teléfono") },
                                        onClick = {
                                            SystemDualAppsLauncher.launchNativeApp(context, profile.packageName)
                                            showMoreMenu = false
                                        },
                                        leadingIcon = { Icon(Icons.Default.PhoneAndroid, contentDescription = null) }
                                    )
                                }

                                DropdownMenuItem(
                                    text = { Text("Ajustes Duales del Móvil") },
                                    onClick = {
                                        SystemDualAppsLauncher.openDeviceDualAppSettings(context)
                                        showMoreMenu = false
                                    },
                                    leadingIcon = { Icon(Icons.Default.SettingsSuggest, contentDescription = null) }
                                )

                                DropdownMenuItem(
                                    text = { Text(if (isDesktopMode) "Cambiar a Modo Móvil" else "Cambiar a Modo Escritorio") },
                                    onClick = {
                                        isDesktopMode = !isDesktopMode
                                        webViewInstance?.settings?.userAgentString = if (isDesktopMode) DESKTOP_USER_AGENT else MOBILE_CHROME_USER_AGENT
                                        webViewInstance?.reload()
                                        showMoreMenu = false
                                    },
                                    leadingIcon = { Icon(if (isDesktopMode) Icons.Default.PhoneAndroid else Icons.Default.Computer, contentDescription = null) }
                                )

                                DropdownMenuItem(
                                    text = { Text("Limpiar Datos de Sesión") },
                                    onClick = {
                                        webViewInstance?.clearCache(true)
                                        webViewInstance?.clearFormData()
                                        webViewInstance?.reload()
                                        showMoreMenu = false
                                    },
                                    leadingIcon = { Icon(Icons.Default.CleaningServices, contentDescription = null) }
                                )
                            }
                        }
                    }

                    // Progress bar
                    if (isLoading && progress < 1f) {
                        LinearProgressIndicator(
                            progress = { progress },
                            modifier = Modifier.fillMaxWidth().height(2.5.dp),
                            color = Color(profile.badgeColor),
                            trackColor = Color.Transparent
                        )
                    }
                }
            }
        }

        // App Container View
        Box(modifier = Modifier.fillMaxSize().weight(1f)) {
            AndroidView(
                factory = { ctx ->
                    WebView(ctx).apply {
                        layoutParams = ViewGroup.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT
                        )

                        // Settings for isolated native app experience
                        settings.apply {
                            javaScriptEnabled = true
                            domStorageEnabled = true
                            databaseEnabled = true
                            setSupportZoom(false) // Disable zoom controls for native feel
                            builtInZoomControls = false
                            displayZoomControls = false
                            loadWithOverviewMode = true
                            useWideViewPort = true
                            userAgentString = selectedUserAgent
                            mediaPlaybackRequiresUserGesture = false
                            allowFileAccess = true
                            allowContentAccess = true
                            cacheMode = WebSettings.LOAD_DEFAULT
                            setGeolocationEnabled(true)
                        }

                        // Isolated Cookie setup for profile container
                        val cookieManager = CookieManager.getInstance()
                        cookieManager.setAcceptCookie(true)
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                            cookieManager.setAcceptThirdPartyCookies(this, true)
                        }

                        // WebChromeClient for titles and progress
                        webChromeClient = object : WebChromeClient() {
                            override fun onProgressChanged(view: WebView?, newProgress: Int) {
                                progress = newProgress / 100f
                                isLoading = newProgress < 100
                            }

                            override fun onReceivedTitle(view: WebView?, title: String?) {
                                if (!title.isNullOrEmpty()) {
                                    pageTitle = title
                                    onTitleChanged(title)
                                }
                            }
                        }

                        // WebViewClient with security & app-like responsiveness
                        webViewClient = object : WebViewClient() {
                            override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                                isLoading = true
                                url?.let {
                                    currentUrl = it
                                    inputUrlText = it
                                }
                                canGoBack = view?.canGoBack() == true
                            }

                            override fun onPageFinished(view: WebView?, url: String?) {
                                isLoading = false
                                canGoBack = view?.canGoBack() == true
                                url?.let {
                                    currentUrl = it
                                    inputUrlText = it

                                    // Estimate cookie count & update stats
                                    val cookies = cookieManager.getCookie(it) ?: ""
                                    val cookieCount = if (cookies.isNotEmpty()) cookies.split(";").size else 0
                                    onStatsUpdated(cookieCount, 1024L * (cookieCount + 15))
                                }

                                // Inject viewport and styling to make web apps look 100% native
                                view?.evaluateJavascript(
                                    """
                                    (function() {
                                        // Ensure viewport meta tag exists for responsive mobile layout
                                        var meta = document.querySelector('meta[name="viewport"]');
                                        if (!meta) {
                                            meta = document.createElement('meta');
                                            meta.name = 'viewport';
                                            meta.content = 'width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no';
                                            document.getElementsByTagName('head')[0].appendChild(meta);
                                        }
                                        // Disable default web callout/tap highlights for native touch feel
                                        var style = document.createElement('style');
                                        style.innerHTML = '* { -webkit-tap-highlight-color: transparent; } body { overscroll-behavior-y: contain; }';
                                        document.head.appendChild(style);
                                    })();
                                    """.trimIndent(),
                                    null
                                )
                            }

                            override fun onReceivedSslError(view: WebView?, handler: SslErrorHandler?, error: SslError?) {
                                handler?.proceed()
                            }
                        }

                        loadUrl(profile.targetUrl)
                        webViewInstance = this
                    }
                },
                update = { wv ->
                    webViewInstance = wv
                },
                modifier = Modifier.fillMaxSize().testTag("sandbox_webview")
            )

            // Floating exit button if in Fullscreen Mode
            if (isFullscreenAppMode) {
                SmallFloatingActionButton(
                    onClick = { isFullscreenAppMode = false },
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(12.dp),
                    containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f),
                    contentColor = MaterialTheme.colorScheme.onSurface
                ) {
                    Icon(Icons.Default.FullscreenExit, contentDescription = "Salir de Pantalla Completa", modifier = Modifier.size(18.dp))
                }
            }
        }
    }
}
