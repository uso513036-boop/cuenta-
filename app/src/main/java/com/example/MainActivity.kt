package com.example

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.example.ui.components.CamouflageCalculator
import com.example.ui.components.SecurityLockScreen
import com.example.ui.components.SplitViewSandbox
import com.example.ui.screens.ActiveSandboxScreen
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.SecurityVaultScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.MultiSpaceViewModel
import com.example.ui.viewmodel.ScreenState

class MainActivity : ComponentActivity() {
    private val viewModel: MultiSpaceViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            MyApplicationTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val isVaultUnlocked by viewModel.isVaultUnlocked.collectAsState()
                    val screenState by viewModel.screenState.collectAsState()
                    val allProfiles by viewModel.allProfiles.collectAsState()
                    val secPrefs = viewModel.securityPreferences

                    // Update Screenshot Protection flag dynamically
                    LaunchedEffect(secPrefs.isScreenshotProtectionEnabled) {
                        if (secPrefs.isScreenshotProtectionEnabled) {
                            window.setFlags(
                                WindowManager.LayoutParams.FLAG_SECURE,
                                WindowManager.LayoutParams.FLAG_SECURE
                            )
                        } else {
                            window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
                        }
                    }

                    // Top-Level Lock & Camouflage Routing
                    if (!isVaultUnlocked && secPrefs.hasPinConfigured()) {
                        if (secPrefs.isCamouflageEnabled) {
                            CamouflageCalculator(
                                securityPreferences = secPrefs,
                                onUnlockVault = { isDecoy ->
                                    viewModel.unlockVault(isDecoy)
                                }
                            )
                        } else {
                            SecurityLockScreen(
                                securityPreferences = secPrefs,
                                onUnlocked = { isDecoy ->
                                    viewModel.unlockVault(isDecoy)
                                }
                            )
                        }
                    } else {
                        // Handle back navigation according to state
                        BackHandler(enabled = screenState !is ScreenState.Dashboard) {
                            when (screenState) {
                                is ScreenState.SingleSandbox,
                                is ScreenState.SplitSandbox,
                                is ScreenState.SecurityVault -> {
                                    viewModel.closeSandboxToDashboard()
                                }
                                else -> {}
                            }
                        }

                        AnimatedContent(
                            targetState = screenState,
                            transitionSpec = { fadeIn() togetherWith fadeOut() },
                            label = "screen_transition"
                        ) { state ->
                            when (state) {
                                is ScreenState.Dashboard -> {
                                    DashboardScreen(viewModel = viewModel)
                                }
                                is ScreenState.SingleSandbox -> {
                                    ActiveSandboxScreen(
                                        profile = state.profile,
                                        viewModel = viewModel
                                    )
                                }
                                is ScreenState.SplitSandbox -> {
                                    SplitViewSandbox(
                                        allProfiles = allProfiles,
                                        topProfile = state.topProfile,
                                        bottomProfile = state.bottomProfile,
                                        onSelectTopProfile = { newTop ->
                                            viewModel.openSplitSandbox(newTop, state.bottomProfile)
                                        },
                                        onSelectBottomProfile = { newBottom ->
                                            viewModel.openSplitSandbox(state.topProfile, newBottom)
                                        },
                                        onSwapProfiles = {
                                            viewModel.openSplitSandbox(state.bottomProfile, state.topProfile)
                                        },
                                        onExitSplitMode = {
                                            viewModel.closeSandboxToDashboard()
                                        }
                                    )
                                }
                                is ScreenState.SecurityVault -> {
                                    SecurityVaultScreen(viewModel = viewModel)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
