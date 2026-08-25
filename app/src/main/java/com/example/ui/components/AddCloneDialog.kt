package com.example.ui.components

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
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
import com.example.model.InstalledApp
import com.example.model.InstalledAppScanner
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

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

private fun drawableToBitmap(drawable: Drawable?): Bitmap? {
    if (drawable == null) return null
    if (drawable is BitmapDrawable && drawable.bitmap != null) {
        return drawable.bitmap
    }
    return try {
        val width = if (drawable.intrinsicWidth > 0) drawable.intrinsicWidth else 96
        val height = if (drawable.intrinsicHeight > 0) drawable.intrinsicHeight else 96
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        drawable.setBounds(0, 0, canvas.width, canvas.height)
        drawable.draw(canvas)
        bitmap
    } catch (e: Exception) {
        null
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddCloneDialog(
    onDismiss: () -> Unit,
    onConfirmAdd: (ProfileEntity) -> Unit
) {
    val context = LocalContext.current
    var selectedTab by remember { mutableIntStateOf(0) } // 0 = Phone Apps, 1 = Web / Popular, 2 = Custom

    // Installed apps from phone
    var installedApps by remember { mutableStateOf<List<InstalledApp>>(emptyList()) }
    var isLoadingApps by remember { mutableStateOf(true) }
    var appSearchQuery by remember { mutableStateOf("") }

    // Selected App / Profile Form State
    var selectedInstalledApp by remember { mutableStateOf<InstalledApp?>(null) }
    var selectedPreset by remember { mutableStateOf<AppPreset?>(null) }

    var profileName by remember { mutableStateOf("") }
    var appTitle by remember { mutableStateOf("") }
    var targetUrl by remember { mutableStateOf("https://") }
    var selectedCategory by remember { mutableStateOf("Personal") }
    var selectedColor by remember { mutableLongStateOf(0xFF06B6D4) }
    var selectedIconKey by remember { mutableStateOf("apps") }
    var isDesktopMode by remember { mutableStateOf(false) }
    var isIncognito by remember { mutableStateOf(false) }
    var isPinLocked by remember { mutableStateOf(false) }
    var customPin by remember { mutableStateOf("") }

    // Load installed apps asynchronously
    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            val apps = InstalledAppScanner.scanInstalledApps(context)
            installedApps = apps
            isLoadingApps = false
        }
    }

    val filteredInstalledApps = remember(installedApps, appSearchQuery) {
        if (appSearchQuery.isBlank()) {
            installedApps
        } else {
            installedApps.filter {
                it.appName.contains(appSearchQuery, ignoreCase = true) ||
                it.packageName.contains(appSearchQuery, ignoreCase = true)
            }
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp,
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .fillMaxHeight(0.90f)
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
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Clonar Aplicación",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Selecciona una app de tu teléfono para crear un clon independiente",
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

                Spacer(modifier = Modifier.height(10.dp))

                // Tabs: Phone Apps vs Catalog vs Custom
                TabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                ) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.PhoneAndroid, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Mi Teléfono (${installedApps.size})", maxLines = 1)
                            }
                        }
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Public, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Webs Populares", maxLines = 1)
                            }
                        }
                    )
                    Tab(
                        selected = selectedTab == 2,
                        onClick = {
                            selectedTab = 2
                            if (profileName.isEmpty()) profileName = "Mi App Clon"
                            if (appTitle.isEmpty()) appTitle = "App Personalizada"
                        },
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.AddLink, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Personalizado", maxLines = 1)
                            }
                        }
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Content Based on Tab
                when (selectedTab) {
                    0 -> {
                        // TAB 0: INSTALLED APPS FROM PHONE
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth()
                        ) {
                            // Search bar for installed apps
                            OutlinedTextField(
                                value = appSearchQuery,
                                onValueChange = { appSearchQuery = it },
                                placeholder = { Text("Buscar en mis aplicaciones...") },
                                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                                trailingIcon = {
                                    if (appSearchQuery.isNotEmpty()) {
                                        IconButton(onClick = { appSearchQuery = "" }) {
                                            Icon(Icons.Default.Clear, contentDescription = "Limpiar")
                                        }
                                    }
                                },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(14.dp)
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            if (isLoadingApps) {
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxWidth(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        CircularProgressIndicator(modifier = Modifier.size(36.dp))
                                        Spacer(modifier = Modifier.height(10.dp))
                                        Text(
                                            "Explorando apps instaladas en tu teléfono...",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            } else if (filteredInstalledApps.isEmpty()) {
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxWidth(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        "No se encontraron aplicaciones con ese nombre.",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            } else {
                                LazyColumn(
                                    modifier = Modifier.weight(1f),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    items(filteredInstalledApps, key = { it.packageName }) { app ->
                                        val isSelected = selectedInstalledApp?.packageName == app.packageName
                                        val appIconBitmap = remember(app.packageName) { drawableToBitmap(app.icon) }

                                        Surface(
                                            shape = RoundedCornerShape(16.dp),
                                            color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                                            border = if (isSelected) ButtonDefaults.outlinedButtonBorder else null,
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clip(RoundedCornerShape(16.dp))
                                                .clickable {
                                                    selectedInstalledApp = app
                                                    selectedPreset = null
                                                    profileName = "${app.appName} Clon 2"
                                                    appTitle = app.appName
                                                    targetUrl = app.suggestedUrl
                                                    selectedCategory = app.suggestedCategory
                                                    selectedColor = app.suggestedColor
                                                    selectedIconKey = app.suggestedIconKey
                                                    isDesktopMode = app.recommendedDesktopUA
                                                }
                                        ) {
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(12.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                if (appIconBitmap != null) {
                                                    Image(
                                                        bitmap = appIconBitmap.asImageBitmap(),
                                                        contentDescription = app.appName,
                                                        modifier = Modifier
                                                            .size(40.dp)
                                                            .clip(RoundedCornerShape(10.dp))
                                                    )
                                                } else {
                                                    Box(
                                                        modifier = Modifier
                                                            .size(40.dp)
                                                            .clip(RoundedCornerShape(10.dp))
                                                            .background(Color(app.suggestedColor)),
                                                        contentAlignment = Alignment.Center
                                                    ) {
                                                        Icon(
                                                            imageVector = AppCatalog.getIconForKey(app.suggestedIconKey),
                                                            contentDescription = app.appName,
                                                            tint = Color.White,
                                                            modifier = Modifier.size(22.dp)
                                                        )
                                                    }
                                                }

                                                Spacer(modifier = Modifier.width(12.dp))

                                                Column(modifier = Modifier.weight(1f)) {
                                                    Text(
                                                        text = app.appName,
                                                        style = MaterialTheme.typography.bodyLarge,
                                                        fontWeight = FontWeight.Bold,
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis
                                                    )
                                                    Text(
                                                        text = app.packageName,
                                                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis
                                                    )
                                                }

                                                if (isSelected) {
                                                    Icon(
                                                        imageVector = Icons.Default.CheckCircle,
                                                        contentDescription = "Seleccionada",
                                                        tint = MaterialTheme.colorScheme.primary,
                                                        modifier = Modifier.size(24.dp)
                                                    )
                                                } else {
                                                    OutlinedButton(
                                                        onClick = {
                                                            selectedInstalledApp = app
                                                            selectedPreset = null
                                                            profileName = "${app.appName} Clon 2"
                                                            appTitle = app.appName
                                                            targetUrl = app.suggestedUrl
                                                            selectedCategory = app.suggestedCategory
                                                            selectedColor = app.suggestedColor
                                                            selectedIconKey = app.suggestedIconKey
                                                            isDesktopMode = app.recommendedDesktopUA
                                                        },
                                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                                        modifier = Modifier.height(34.dp)
                                                    ) {
                                                        Text("Elegir", fontSize = 12.sp)
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    1 -> {
                        // TAB 1: POPULAR WEB SERVICES
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .verticalScroll(rememberScrollState()),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "Elige una plataforma popular:",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            AppCatalog.PRESETS.forEach { preset ->
                                val isSelected = selectedPreset?.name == preset.name
                                Surface(
                                    shape = RoundedCornerShape(16.dp),
                                    color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                                    border = if (isSelected) ButtonDefaults.outlinedButtonBorder else null,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(16.dp))
                                        .clickable {
                                            selectedPreset = preset
                                            selectedInstalledApp = null
                                            profileName = "${preset.name} Clon 2"
                                            appTitle = preset.name
                                            targetUrl = preset.defaultUrl
                                            selectedCategory = if (preset.category in CATEGORIES) preset.category else "Personal"
                                            selectedColor = preset.defaultColor
                                            selectedIconKey = preset.iconKey
                                            isDesktopMode = preset.recommendedDesktopUA
                                        }
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(38.dp)
                                                .clip(RoundedCornerShape(10.dp))
                                                .background(Color(preset.defaultColor)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = AppCatalog.getIconForKey(preset.iconKey),
                                                contentDescription = preset.name,
                                                tint = Color.White,
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }

                                        Spacer(modifier = Modifier.width(12.dp))

                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = preset.name,
                                                style = MaterialTheme.typography.bodyLarge,
                                                fontWeight = FontWeight.Bold
                                            )
                                            Text(
                                                text = preset.description,
                                                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                maxLines = 1
                                            )
                                        }

                                        if (isSelected) {
                                            Icon(
                                                imageVector = Icons.Default.CheckCircle,
                                                contentDescription = "Seleccionada",
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(22.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    2 -> {
                        // TAB 2: MANUAL / CUSTOM APP CONFIGURATION
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .verticalScroll(rememberScrollState()),
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            OutlinedTextField(
                                value = appTitle,
                                onValueChange = {
                                    appTitle = it
                                    if (profileName.isEmpty() || profileName.startsWith("Mi App")) {
                                        profileName = "$it Clon"
                                    }
                                },
                                label = { Text("Nombre de la Aplicación") },
                                placeholder = { Text("Ej: WhatsApp, Mercado Libre, Banco") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(14.dp)
                            )

                            OutlinedTextField(
                                value = targetUrl,
                                onValueChange = { targetUrl = it },
                                label = { Text("URL o Dirección Web") },
                                placeholder = { Text("https://...") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(14.dp)
                            )
                        }
                    }
                }

                // Configuration Section if an app is selected
                if (selectedInstalledApp != null || selectedPreset != null || selectedTab == 2) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Divider()
                    Spacer(modifier = Modifier.height(10.dp))

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "⚙️ Configurar Clon Seleccionado: ${appTitle.ifEmpty { "Nueva App" }}",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )

                        OutlinedTextField(
                            value = profileName,
                            onValueChange = { profileName = it },
                            label = { Text("Nombre del Perfil Clonado") },
                            placeholder = { Text("Ej: WhatsApp Trabajo, Instagram 2") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth().testTag("add_clone_name_input"),
                            shape = RoundedCornerShape(14.dp)
                        )

                        // Category Chips
                        Text(
                            text = "Categoría del Espacio:",
                            style = MaterialTheme.typography.labelMedium
                        )
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            items(CATEGORIES) { cat ->
                                FilterChip(
                                    selected = selectedCategory == cat,
                                    onClick = { selectedCategory = cat },
                                    label = { Text(cat, fontSize = 12.sp) }
                                )
                            }
                        }

                        // Color selection
                        Text(
                            text = "Distintivo de Color:",
                            style = MaterialTheme.typography.labelMedium
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            COLOR_PALETTE.forEach { colorVal ->
                                val isSelected = selectedColor == colorVal
                                Box(
                                    modifier = Modifier
                                        .size(30.dp)
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
                                        Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
                                    }
                                }
                            }
                        }

                        // Desktop Mode & PIN Switches
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text("Modo Escritorio (Desktop UA)", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                                        Text("Recomendado para WhatsApp Web o Discord", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                    Switch(checked = isDesktopMode, onCheckedChange = { isDesktopMode = it })
                                }

                                Divider(modifier = Modifier.padding(vertical = 6.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text("Bloqueo con PIN Individual", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                                        Text("Protege este clon con clave de acceso", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                    Switch(checked = isPinLocked, onCheckedChange = { isPinLocked = it })
                                }

                                if (isPinLocked) {
                                    Spacer(modifier = Modifier.height(6.dp))
                                    OutlinedTextField(
                                        value = customPin,
                                        onValueChange = { if (it.length <= 4) customPin = it },
                                        label = { Text("PIN de 4 dígitos") },
                                        singleLine = true,
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(10.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

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

                    val canCreate = (selectedInstalledApp != null || selectedPreset != null || selectedTab == 2) && profileName.isNotBlank()

                    Button(
                        onClick = {
                            var cleanUrl = targetUrl.trim()
                            if (cleanUrl.isEmpty() || cleanUrl == "https://") {
                                cleanUrl = "https://www.google.com/search?q=${appTitle.replace(" ", "+")}"
                            } else if (!cleanUrl.startsWith("http://") && !cleanUrl.startsWith("https://")) {
                                cleanUrl = "https://$cleanUrl"
                            }

                            val finalProfile = ProfileEntity(
                                name = profileName.ifEmpty { "${appTitle.ifEmpty { "App" }} Clon" },
                                appName = appTitle.ifEmpty { "App" },
                                iconKey = selectedIconKey,
                                badgeColor = selectedColor,
                                targetUrl = cleanUrl,
                                spaceCategory = selectedCategory,
                                userAgentMode = if (isDesktopMode) "Desktop Chrome" else "Mobile Android",
                                desktopMode = isDesktopMode,
                                isIncognito = isIncognito,
                                isPinLocked = isPinLocked,
                                customPin = if (isPinLocked && customPin.isNotEmpty()) customPin else null
                            )
                            onConfirmAdd(finalProfile)
                        },
                        enabled = canCreate,
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
