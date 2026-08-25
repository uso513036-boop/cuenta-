package com.example.ui.screens

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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.data.local.ProfileEntity
import com.example.model.AppCatalog
import com.example.ui.components.AddCloneDialog
import com.example.ui.components.DataInspectorDialog
import com.example.ui.components.QuickSwitchBar
import com.example.ui.components.SandboxWebView
import com.example.ui.components.SecurityLockScreen
import com.example.ui.viewmodel.MultiSpaceViewModel

@Composable
fun ActiveSandboxScreen(
    profile: ProfileEntity,
    viewModel: MultiSpaceViewModel,
    modifier: Modifier = Modifier
) {
    val allProfiles by viewModel.allProfiles.collectAsState()
    var isProfileUnlocked by remember(profile.id) { mutableStateOf(!profile.isPinLocked) }
    var showAddDialog by remember { mutableStateOf(false) }
    var inspectingProfile by remember { mutableStateOf<ProfileEntity?>(null) }

    // If profile is individually locked with PIN, require entry first
    if (!isProfileUnlocked && profile.isPinLocked) {
        SecurityLockScreen(
            securityPreferences = viewModel.securityPreferences,
            targetTitle = "Bloqueo: ${profile.name}",
            onUnlocked = { isProfileUnlocked = true },
            modifier = modifier
        )
        return
    }

    Scaffold(
        modifier = modifier.fillMaxSize().testTag("active_sandbox_screen"),
        bottomBar = {
            QuickSwitchBar(
                profiles = allProfiles,
                activeProfileId = profile.id,
                onSelectProfile = { viewModel.openSandbox(it) },
                onOpenHub = { viewModel.closeSandboxToDashboard() },
                onAddNewClone = { showAddDialog = true },
                onToggleSplitView = {
                    val other = allProfiles.firstOrNull { it.id != profile.id } ?: profile
                    viewModel.openSplitSandbox(profile, other)
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .statusBarsPadding()
        ) {
            SandboxWebView(
                profile = profile,
                modifier = Modifier.fillMaxSize(),
                onTitleChanged = { /* Title update */ },
                onStatsUpdated = { cookies, bytes ->
                    viewModel.updateProfileStats(profile.id, cookies, bytes)
                },
                onCloseSandbox = { viewModel.closeSandboxToDashboard() }
            )
        }
    }

    if (showAddDialog) {
        AddCloneDialog(
            onDismiss = { showAddDialog = false },
            onConfirmAdd = {
                viewModel.addProfile(it)
                showAddDialog = false
            }
        )
    }

    inspectingProfile?.let { p ->
        DataInspectorDialog(
            profile = p,
            onDismiss = { inspectingProfile = null },
            onClearSessionData = { viewModel.clearProfileData(it) },
            onSaveNotes = { pr, notes -> viewModel.saveEncryptedNotes(pr, notes) },
            onDeleteProfile = { viewModel.deleteProfile(it) },
            onToggleFavorite = { viewModel.toggleFavorite(it) }
        )
    }
}
