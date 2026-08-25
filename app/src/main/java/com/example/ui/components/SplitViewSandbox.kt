package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.ProfileEntity
import com.example.model.AppCatalog

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SplitViewSandbox(
    allProfiles: List<ProfileEntity>,
    topProfile: ProfileEntity,
    bottomProfile: ProfileEntity,
    onSelectTopProfile: (ProfileEntity) -> Unit,
    onSelectBottomProfile: (ProfileEntity) -> Unit,
    onSwapProfiles: () -> Unit,
    onExitSplitMode: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showTopPicker by remember { mutableStateOf(false) }
    var showBottomPicker by remember { mutableStateOf(false) }

    Column(modifier = modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        // Top Header
        Surface(
            color = MaterialTheme.colorScheme.surfaceVariant,
            tonalElevation = 2.dp,
            modifier = Modifier.fillMaxWidth().statusBarsPadding()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.ViewAgenda,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Modo Paralelo Multi-Cuenta",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                }

                Row {
                    IconButton(onClick = onSwapProfiles) {
                        Icon(imageVector = Icons.Default.SwapVert, contentDescription = "Intercambiar")
                    }
                    IconButton(onClick = onExitSplitMode) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Salir")
                    }
                }
            }
        }

        // Top Pane
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Top Pane Bar
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 10.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .background(Color(topProfile.badgeColor), shape = RoundedCornerShape(2.dp))
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Panel 1: ${topProfile.name}",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        TextButton(
                            onClick = { showTopPicker = true },
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Text("Cambiar", fontSize = 12.sp)
                        }
                    }
                }

                NativeAppContainer(
                    profile = topProfile,
                    modifier = Modifier.weight(1f),
                    onCloseSandbox = onExitSplitMode
                )
            }
        }

        // Divider
        Divider(
            thickness = 3.dp,
            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
        )

        // Bottom Pane
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Bottom Pane Bar
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 10.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .background(Color(bottomProfile.badgeColor), shape = RoundedCornerShape(2.dp))
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Panel 2: ${bottomProfile.name}",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        TextButton(
                            onClick = { showBottomPicker = true },
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Text("Cambiar", fontSize = 12.sp)
                        }
                    }
                }

                NativeAppContainer(
                    profile = bottomProfile,
                    modifier = Modifier.weight(1f),
                    onCloseSandbox = onExitSplitMode
                )
            }
        }
    }

    // Top Profile Picker Sheet
    if (showTopPicker) {
        AlertDialog(
            onDismissRequest = { showTopPicker = false },
            title = { Text("Seleccionar Cuenta para Panel Superior") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    allProfiles.forEach { p ->
                        Surface(
                            onClick = {
                                onSelectTopProfile(p)
                                showTopPicker = false
                            },
                            shape = RoundedCornerShape(12.dp),
                            color = if (p.id == topProfile.id) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(12.dp)
                                        .background(Color(p.badgeColor), shape = RoundedCornerShape(6.dp))
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(p.name, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showTopPicker = false }) { Text("Cerrar") }
            }
        )
    }

    // Bottom Profile Picker Sheet
    if (showBottomPicker) {
        AlertDialog(
            onDismissRequest = { showBottomPicker = false },
            title = { Text("Seleccionar Cuenta para Panel Inferior") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    allProfiles.forEach { p ->
                        Surface(
                            onClick = {
                                onSelectBottomProfile(p)
                                showBottomPicker = false
                            },
                            shape = RoundedCornerShape(12.dp),
                            color = if (p.id == bottomProfile.id) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(12.dp)
                                        .background(Color(p.badgeColor), shape = RoundedCornerShape(6.dp))
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(p.name, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showBottomPicker = false }) { Text("Cerrar") }
            }
        )
    }
}
