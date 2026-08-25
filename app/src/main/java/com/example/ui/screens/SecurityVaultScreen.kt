package com.example.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.viewmodel.MultiSpaceViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SecurityVaultScreen(
    viewModel: MultiSpaceViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val secPrefs = viewModel.securityPreferences

    var isPinEnabled by remember { mutableStateOf(secPrefs.isPinEnabled) }
    var isCamouflageEnabled by remember { mutableStateOf(secPrefs.isCamouflageEnabled) }
    var isBiometricsEnabled by remember { mutableStateOf(secPrefs.isBiometricsEnabled) }
    var isWipeOnExit by remember { mutableStateOf(secPrefs.isWipeOnExitEnabled) }
    var isScreenshotProtected by remember { mutableStateOf(secPrefs.isScreenshotProtectionEnabled) }

    var masterPinInput by remember { mutableStateOf("") }
    var decoyPinInput by remember { mutableStateOf("") }
    var showPinDialog by remember { mutableStateOf(false) }
    var showDecoyPinDialog by remember { mutableStateOf(false) }

    Scaffold(
        modifier = modifier.fillMaxSize().testTag("security_vault_screen"),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Bóveda & Seguridad",
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { viewModel.closeSandboxToDashboard() }) {
                        Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Atrás")
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.lockVault() }) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = "Bloquear Ahora",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Security Badge Banner
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                shape = RoundedCornerShape(20.dp)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Shield,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(36.dp)
                    )
                    Spacer(modifier = Modifier.width(14.dp))
                    Column {
                        Text(
                            text = "Contenedor Blindado AES-256 GCM",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text(
                            text = "Aislamiento criptográfico total de tokens, cookies y almacenamiento local entre cuentas.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                        )
                    }
                }
            }

            // PIN & Password Section
            Text(
                text = "Control de Acceso",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )

            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                shape = RoundedCornerShape(18.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "PIN Maestro de la Bóveda",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = if (secPrefs.hasPinConfigured()) "PIN configurado y activo" else "Sin PIN (acceso directo)",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Button(
                            onClick = { showPinDialog = true },
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(if (secPrefs.hasPinConfigured()) "Cambiar" else "Configurar")
                        }
                    }

                    Divider(modifier = Modifier.padding(vertical = 12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Desbloqueo Biométrico",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "Permitir huella dactilar para acceder rápidamente",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = isBiometricsEnabled,
                            onCheckedChange = {
                                isBiometricsEnabled = it
                                secPrefs.isBiometricsEnabled = it
                            }
                        )
                    }
                }
            }

            // Camouflage & Stealth Section
            Text(
                text = "Modo Camuflaje & Privacidad Extrema",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )

            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                shape = RoundedCornerShape(18.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Disfraz de Calculadora",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "Muestra una calculadora funcional al abrir. Introduce tu PIN y '=' para desbloquear.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = isCamouflageEnabled,
                            onCheckedChange = {
                                isCamouflageEnabled = it
                                secPrefs.isCamouflageEnabled = it
                                Toast.makeText(
                                    context,
                                    if (it) "Camuflaje de calculadora activado" else "Camuflaje desactivado",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        )
                    }

                    Divider(modifier = Modifier.padding(vertical = 12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "PIN Señuelo (Modo Falso)",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "Un PIN falso que abre una bóveda vacía si te obligan a desbloquear.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        OutlinedButton(
                            onClick = { showDecoyPinDialog = true },
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Configurar")
                        }
                    }

                    Divider(modifier = Modifier.padding(vertical = 12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Protección contra Capturas",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "Evita capturas de pantalla y grabaciones en apps de terceros.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = isScreenshotProtected,
                            onCheckedChange = {
                                isScreenshotProtected = it
                                secPrefs.isScreenshotProtectionEnabled = it
                            }
                        )
                    }

                    Divider(modifier = Modifier.padding(vertical = 12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Limpieza al Salir",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "Eliminar historiales temporales y cachés al cerrar la app.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = isWipeOnExit,
                            onCheckedChange = {
                                isWipeOnExit = it
                                secPrefs.isWipeOnExitEnabled = it
                            }
                        )
                    }
                }
            }

            // Lock Vault Now Button
            Button(
                onClick = { viewModel.lockVault() },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp)
            ) {
                Icon(imageVector = Icons.Default.Lock, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Bloquear Bóveda Ahora")
            }
        }
    }

    // Set Master PIN Dialog
    if (showPinDialog) {
        AlertDialog(
            onDismissRequest = { showPinDialog = false },
            title = { Text("Configurar PIN Maestro") },
            text = {
                Column {
                    Text("Ingresa un PIN numérico de 4 dígitos para proteger todas tus cuentas:")
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = masterPinInput,
                        onValueChange = { if (it.length <= 4) masterPinInput = it },
                        label = { Text("PIN (4 dígitos)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (masterPinInput.length == 4) {
                            secPrefs.setMasterPin(masterPinInput)
                            isPinEnabled = true
                            showPinDialog = false
                            Toast.makeText(context, "PIN Maestro configurado con éxito", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(context, "El PIN debe tener 4 dígitos", Toast.LENGTH_SHORT).show()
                        }
                    }
                ) {
                    Text("Guardar")
                }
            },
            dismissButton = {
                TextButton(onClick = { showPinDialog = false }) { Text("Cancelar") }
            }
        )
    }

    // Set Decoy PIN Dialog
    if (showDecoyPinDialog) {
        AlertDialog(
            onDismissRequest = { showDecoyPinDialog = false },
            title = { Text("Configurar PIN Señuelo") },
            text = {
                Column {
                    Text("Ingresa un PIN diferente al maestro. Si se introduce este PIN, la app se abrirá ocultando tus cuentas privadas:")
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = decoyPinInput,
                        onValueChange = { if (it.length <= 4) decoyPinInput = it },
                        label = { Text("PIN Señuelo (4 dígitos)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (decoyPinInput.length == 4 && decoyPinInput != masterPinInput) {
                            secPrefs.setDecoyPin(decoyPinInput)
                            showDecoyPinDialog = false
                            Toast.makeText(context, "PIN Señuelo configurado con éxito", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(context, "El PIN debe tener 4 dígitos y ser distinto al maestro", Toast.LENGTH_SHORT).show()
                        }
                    }
                ) {
                    Text("Guardar")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDecoyPinDialog = false }) { Text("Cancelar") }
            }
        )
    }
}
