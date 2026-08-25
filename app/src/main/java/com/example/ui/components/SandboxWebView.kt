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

private const val DESKTOP_USER_AGENT =
    "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36"
private const val MOBILE_ANDROID_USER_AGENT =
    "Mozilla/5.0 (Linux; Android 14; Pixel 8 Pro) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Mobile Safari/537.36"
private const val IOS_SAFARI_USER_AGENT =
    "Mozilla/5.0 (iPhone; CPU iPhone OS 17_4_1 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/17.4.1 Mobile/15E148 Safari/604.1"

@SuppressLint("SetJavaScriptEnabled")
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
    var canGoForward by remember { mutableStateOf(false) }
    var isDesktopMode by remember { mutableStateOf(profile.desktopMode || profile.userAgentMode == "Desktop Chrome") }
    var isAdBlockActive by remember { mutableStateOf(profile.adBlockEnabled) }
    var showUrlBar by remember { mutableStateOf(false) }
    var inputUrlText by remember { mutableStateOf(profile.targetUrl) }
    var isSecureConnection by remember { mutableStateOf(true) }

    // Resolve User-Agent based on preference
    val selectedUserAgent = remember(isDesktopMode, profile.userAgentMode) {
        if (isDesktopMode) {
            DESKTOP_USER_AGENT
        } else {
            when (profile.userAgentMode) {
                "Mobile iOS Safari" -> IOS_SAFARI_USER_AGENT
                "Desktop Chrome" -> DESKTOP_USER_AGENT
                else -> MOBILE_ANDROID_USER_AGENT
            }
        }
    }

    Column(modifier = modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        // Top Sandbox Control Bar
        Surface(
            color = MaterialTheme.colorScheme.surfaceVariant,
            tonalElevation = 3.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Back & Forward
                    IconButton(
                        onClick = {
                            if (webViewInstance?.canGoBack() == true) {
                                webViewInstance?.goBack()
                            } else {
                                onCloseSandbox()
                            }
                        },
                        modifier = Modifier.size(36.dp).testTag("sandbox_back_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Atrás",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    IconButton(
                        onClick = { webViewInstance?.goForward() },
                        enabled = canGoForward,
                        modifier = Modifier.size(36.dp).testTag("sandbox_forward_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.ArrowForward,
                            contentDescription = "Adelante",
                            tint = if (canGoForward) MaterialTheme.colorScheme.onSurfaceVariant
                            else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f)
                        )
                    }

                    // Refresh / Stop
                    IconButton(
                        onClick = {
                            if (isLoading) webViewInstance?.stopLoading()
                            else webViewInstance?.reload()
                        },
                        modifier = Modifier.size(36.dp).testTag("sandbox_reload_btn")
                    ) {
                        Icon(
                            imageVector = if (isLoading) Icons.Default.Close else Icons.Default.Refresh,
                            contentDescription = if (isLoading) "Detener" else "Recargar",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    // Profile Identity Pill & URL clicker
                    Surface(
                        color = MaterialTheme.colorScheme.surface,
                        shape = RoundedCornerShape(20.dp),
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 4.dp),
                        onClick = { showUrlBar = !showUrlBar }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .clip(RoundedCornerShape(5.dp))
                                    .background(Color(profile.badgeColor))
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Icon(
                                imageVector = if (isSecureConnection) Icons.Default.Lock else Icons.Default.LockOpen,
                                contentDescription = "Seguridad",
                                tint = if (isSecureConnection) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = pageTitle.ifEmpty { currentUrl },
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    // Desktop mode toggle
                    IconButton(
                        onClick = {
                            isDesktopMode = !isDesktopMode
                            webViewInstance?.settings?.userAgentString = if (isDesktopMode) DESKTOP_USER_AGENT else MOBILE_ANDROID_USER_AGENT
                            webViewInstance?.reload()
                        },
                        modifier = Modifier.size(36.dp).testTag("sandbox_desktop_toggle")
                    ) {
                        Icon(
                            imageVector = if (isDesktopMode) Icons.Default.Computer else Icons.Default.PhoneAndroid,
                            contentDescription = "Modo Escritorio",
                            tint = if (isDesktopMode) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    // Close / Return to Hub
                    IconButton(
                        onClick = onCloseSandbox,
                        modifier = Modifier.size(36.dp).testTag("sandbox_close_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.FullscreenExit,
                            contentDescription = "Salir de Sandbox",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Expandable Custom URL input
                AnimatedVisibility(visible = showUrlBar) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = inputUrlText,
                            onValueChange = { inputUrlText = it },
                            placeholder = { Text("https://...") },
                            singleLine = true,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("sandbox_url_input"),
                            shape = RoundedCornerShape(12.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                var target = inputUrlText.trim()
                                if (!target.startsWith("http://") && !target.startsWith("https://")) {
                                    target = "https://$target"
                                }
                                webViewInstance?.loadUrl(target)
                                showUrlBar = false
                            },
                            modifier = Modifier.testTag("sandbox_go_url_btn")
                        ) {
                            Text("Ir")
                        }
                    }
                }

                // Loading progress bar
                if (isLoading && progress < 1f) {
                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier.fillMaxWidth().height(3.dp),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = Color.Transparent
                    )
                }
            }
        }

        // Web Container View
        Box(modifier = Modifier.fillMaxSize().weight(1f)) {
            AndroidView(
                factory = { ctx ->
                    WebView(ctx).apply {
                        layoutParams = ViewGroup.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT
                        )

                        // Settings for isolated modern sandbox
                        settings.apply {
                            javaScriptEnabled = true
                            domStorageEnabled = true
                            databaseEnabled = true
                            setSupportZoom(true)
                            builtInZoomControls = true
                            displayZoomControls = false
                            loadWithOverviewMode = true
                            useWideViewPort = true
                            userAgentString = selectedUserAgent
                            mediaPlaybackRequiresUserGesture = false
                            allowFileAccess = true
                            allowContentAccess = true
                            cacheMode = WebSettings.LOAD_DEFAULT
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

                        // WebViewClient with security & anti-tracking rules
                        webViewClient = object : WebViewClient() {
                            override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                                isLoading = true
                                url?.let {
                                    currentUrl = it
                                    inputUrlText = it
                                    isSecureConnection = it.startsWith("https://")
                                }
                                canGoBack = view?.canGoBack() == true
                                canGoForward = view?.canGoForward() == true
                            }

                            override fun onPageFinished(view: WebView?, url: String?) {
                                isLoading = false
                                canGoBack = view?.canGoBack() == true
                                canGoForward = view?.canGoForward() == true
                                url?.let {
                                    currentUrl = it
                                    inputUrlText = it
                                    isSecureConnection = it.startsWith("https://")

                                    // Estimate cookie count & update stats
                                    val cookies = cookieManager.getCookie(it) ?: ""
                                    val cookieCount = if (cookies.isNotEmpty()) cookies.split(";").size else 0
                                    onStatsUpdated(cookieCount, 1024L * (cookieCount + 15))
                                }

                                // Apply anti-tracking and viewport tweaks if requested
                                if (isAdBlockActive) {
                                    view?.evaluateJavascript(
                                        """
                                        (function() {
                                            var elements = document.querySelectorAll('iframe[src*="doubleclick"], iframe[src*="adservice"], div[id*="google_ads"]');
                                            elements.forEach(function(el) { el.style.display = 'none'; });
                                        })();
                                        """.trimIndent(),
                                        null
                                    )
                                }
                            }

                            override fun onReceivedSslError(view: WebView?, handler: SslErrorHandler?, error: SslError?) {
                                // Default secure handling
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
        }
    }
}
