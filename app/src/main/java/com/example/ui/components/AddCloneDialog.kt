package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.local.ProfileEntity
import com.example.model.AppCatalog
import com.example.model.AppPreset

private val COLOR_PALETTE = listOf(
    0xFF06B6D4, // Cyan
    0xFF25D366, // WhatsApp Green
    0xFF2AABEE, // Telegram Blue
    0xFF3B82F6, // Electric Blue
    0xFF8B5CF6, // Purple
    0xFFE1306C, // Instagram Pink
    0xFFEE1D52, // TikTok Red
    0xFFF59E0B, // Amber Gold
    0xFF10B981, // Emerald
    0xFF6E5494  // GitHub Purple
)

private val CATEGORIES = listOf("Personal", "Trabajo", "Privado", "Finanzas", "Social", "Comunidad")
private val USER_AGENTS = listOf("Mobile Android", "Desktop Chrome", "Mobile iOS Safari")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddCloneDialog(
    onDismiss: () -> Unit,
    onConfirmAdd: (ProfileEntity) -> Unit
) {
    var selectedPreset by remember { mutableStateOf<AppPreset?>(AppCatalog.PRESETS.first()) }
    var profileName by remember { mutableStateOf(selectedPreset?.let { "${it.name} Clon 2" } ?: "Nueva Cuenta") }
    var targetUrl by remember { mutableStateOf(selectedPreset?.defaultUrl ?: "https://") }
    var selectedCategory by remember { mutableStateOf(selectedPreset?.category ?: "Personal") }
    var selectedColor by remember { mutableLongStateOf(selectedPreset?.defaultColor ?: 0xFF06B6D4) }
    var selectedUserAgent by remember { mutableStateOf(if (selectedPreset?.recommendedDesktopUA == true) "Desktop Chrome" else "Mobile Android") }
    var isDesktopMode by remember { mutableStateOf(selectedPreset?.recommendedDesktopUA ?: false) }
    var isIncognito by remember { mutableStateOf(false) }
    var isPinLocked by remember { mutableStateOf(false) }
    var customPin by remember { mutableStateOf("") }
    var showCustomUrlInput by remember { mutableStateOf(false) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp,
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .fillMaxHeight(0.88f)
                .testTag("add_clone_dialog")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Clonar Aplicación",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Crea un contenedor cifrado e independiente",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Cerrar",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Scrollable Form
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // App Catalog Presets
                    Text(
                        text = "1. Selecciona la aplicación a clonar",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary
                    )

                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(AppCatalog.PRESETS) { preset ->
                            val isChosen = selectedPreset?.name == preset.name
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = if (isChosen) Color(preset.defaultColor).copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceVariant,
                                border = if (isChosen) ButtonDefaults.outlinedButtonBorder else null,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(16.dp))
                                    .clickable {
                                        selectedPreset = preset
                                        profileName = "${preset.name} Cuenta 2"
                                        targetUrl = preset.defaultUrl
                                        selectedColor = preset.defaultColor
                                        selectedCategory = if (preset.category in CATEGORIES) preset.category else "Personal"
                                        isDesktopMode = preset.recommendedDesktopUA
                                        selectedUserAgent = if (preset.recommendedDesktopUA) "Desktop Chrome" else "Mobile Android"
                                        showCustomUrlInput = preset.name.contains("Cualquier")
                                    }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(24.dp)
                                            .clip(CircleShape)
                                            .background(Color(preset.defaultColor)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = AppCatalog.getIconForKey(preset.iconKey),
                                            contentDescription = preset.name,
                                            tint = Color.White,
                                            modifier = Modifier.size(14.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = preset.name,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = if (isChosen) FontWeight.Bold else FontWeight.Normal
                                    )
                                }
                            }
                        }
                    }

                    // Profile Name Input
                    OutlinedTextField(
                        value = profileName,
                        onValueChange = { profileName = it },
                        label = { Text("Nombre del Perfil Clonado") },
                        placeholder = { Text("Ej: WhatsApp Negocio, Instagram 2") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("add_clone_name_input"),
                        shape = RoundedCornerShape(14.dp)
                    )

                    // Target URL
                    OutlinedTextField(
                        value = targetUrl,
                        onValueChange = { targetUrl = it },
                        label = { Text("URL del Servicio Web / App") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("add_clone_url_input"),
                        shape = RoundedCornerShape(14.dp)
                    )

                    // Category Selector
                    Text(
                        text = "2. Espacio / Categoría",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary
                    )

                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(CATEGORIES) { cat ->
                            FilterChip(
                                selected = selectedCategory == cat,
                                onClick = { selectedCategory = cat },
                                label = { Text(cat) }
                            )
                        }
                    }

                    // Color Badge Selector
                    Text(
                        text = "3. Distintivo de Color",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary
                    )

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        COLOR_PALETTE.forEach { colorVal ->
                            val isSelected = selectedColor == colorVal
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(Color(colorVal))
                                    .clickable { selectedColor = colorVal }
                                    .then(
                                        if (isSelected) Modifier.border(2.dp, MaterialTheme.colorScheme.onSurface, CircleShape)
                                        else Modifier
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                if (isSelected) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = "Seleccionado",
                                        tint = Color.White,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }

                    // User Agent & Sandbox Features
                    Text(
                        text = "4. Aislamiento & Motor Sandbox",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary
                    )

                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Modo Escritorio (Desktop UA)",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Text(
                                        text = "Requerido para WhatsApp Web, Notion o Discord",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Switch(
                                    checked = isDesktopMode,
                                    onCheckedChange = {
                                        isDesktopMode = it
                                        selectedUserAgent = if (it) "Desktop Chrome" else "Mobile Android"
                                    }
                                )
                            }

                            Divider(modifier = Modifier.padding(vertical = 8.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Contenedor Incógnito",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Text(
                                        text = "Elimina automáticamente cookies y caché al cerrar",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Switch(
                                    checked = isIncognito,
                                    onCheckedChange = { isIncognito = it }
                                )
                            }

                            Divider(modifier = Modifier.padding(vertical = 8.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Bloqueo con PIN Individual",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Text(
                                        text = "Exige PIN para abrir este clon en particular",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Switch(
                                    checked = isPinLocked,
                                    onCheckedChange = { isPinLocked = it }
                                )
                            }

                            if (isPinLocked) {
                                Spacer(modifier = Modifier.height(8.dp))
                                OutlinedTextField(
                                    value = customPin,
                                    onValueChange = { if (it.length <= 4) customPin = it },
                                    label = { Text("PIN de 4 dígitos para este perfil") },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(12.dp)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Bottom Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancelar")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            var cleanUrl = targetUrl.trim()
                            if (!cleanUrl.startsWith("http://") && !cleanUrl.startsWith("https://")) {
                                cleanUrl = "https://$cleanUrl"
                            }
                            val finalProfile = ProfileEntity(
                                name = profileName.ifEmpty { "Mi Perfil Clon" },
                                appName = selectedPreset?.name ?: "Custom App",
                                iconKey = selectedPreset?.iconKey ?: "custom",
                                badgeColor = selectedColor,
                                targetUrl = cleanUrl,
                                spaceCategory = selectedCategory,
                                userAgentMode = selectedUserAgent,
                                desktopMode = isDesktopMode,
                                isIncognito = isIncognito,
                                isPinLocked = isPinLocked,
                                customPin = if (isPinLocked && customPin.isNotEmpty()) customPin else null
                            )
                            onConfirmAdd(finalProfile)
                        },
                        modifier = Modifier.testTag("add_clone_confirm_btn")
                    ) {
                        Icon(imageVector = Icons.Default.AddCircle, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Crear Clon Seguro")
                    }
                }
            }
        }
    }
}
