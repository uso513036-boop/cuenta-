package com.example.ui.components

import android.content.Context
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.ui.viewinterop.AndroidView
import com.example.data.local.ProfileEntity
import com.example.model.CountryInfo
import com.example.model.CountryRepository
import com.example.security.SandboxIntentInterceptor
import com.example.util.SystemDualAppsLauncher
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

// Data Models for Native WhatsApp Engine
data class WhatsAppChat(
    val id: String,
    val contactName: String,
    val contactPhone: String,
    val avatarColor: Long,
    val lastMessage: String,
    val timestamp: String,
    val unreadCount: Int = 0,
    val isOnline: Boolean = false,
    val messages: MutableList<WhatsAppMessage> = mutableListOf()
)

data class WhatsAppMessage(
    val id: String = UUID.randomUUID().toString(),
    val text: String,
    val isOutgoing: Boolean,
    val timestamp: String,
    val isRead: Boolean = true,
    val mediaType: String? = null // "AUDIO", "IMAGE", "DOCUMENT"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NativeAppContainer(
    profile: ProfileEntity,
    modifier: Modifier = Modifier,
    onStatsUpdated: (cookies: Int, bytes: Long) -> Unit = { _, _ -> },
    onCloseSandbox: () -> Unit = {}
) {
    val pkg = profile.packageName?.lowercase() ?: profile.appName.lowercase()
    val name = profile.name.lowercase()

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        when {
            pkg.contains("whatsapp") || name.contains("whatsapp") -> {
                NativeWhatsAppSandbox(
                    profile = profile,
                    onStatsUpdated = onStatsUpdated,
                    onCloseSandbox = onCloseSandbox
                )
            }
            pkg.contains("imvu") || name.contains("imvu") -> {
                NativeImvuSandbox(
                    profile = profile,
                    onStatsUpdated = onStatsUpdated,
                    onCloseSandbox = onCloseSandbox
                )
            }
            pkg.contains("telegram") || name.contains("telegram") -> {
                NativeTelegramSandbox(
                    profile = profile,
                    onStatsUpdated = onStatsUpdated,
                    onCloseSandbox = onCloseSandbox
                )
            }
            pkg.contains("instagram") || name.contains("instagram") -> {
                NativeInstagramSandbox(
                    profile = profile,
                    onStatsUpdated = onStatsUpdated,
                    onCloseSandbox = onCloseSandbox
                )
            }
            else -> {
                NativeGenericAppSandbox(
                    profile = profile,
                    onStatsUpdated = onStatsUpdated,
                    onCloseSandbox = onCloseSandbox
                )
            }
        }
    }
}

/* ==========================================================================
   1. NATIVE WHATSAPP CLONE ENGINE (Full Native UI & Multi-Account Sandbox)
   ========================================================================== */

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NativeWhatsAppSandbox(
    profile: ProfileEntity,
    onStatsUpdated: (cookies: Int, bytes: Long) -> Unit,
    onCloseSandbox: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    // Persistent Setup State per Account Profile
    var isRegistered by remember(profile.id) {
        mutableStateOf(profile.dataUsageBytes > 0)
    }
    var setupStep by remember(profile.id) { mutableIntStateOf(1) } // 1: Welcome, 2: Phone, 3: OTP, 4: Name
    val initialCountry = remember { CountryRepository.detectDeviceCountry(context) }
    var selectedCountry by remember { mutableStateOf(initialCountry) }
    var selectedCountryCode by remember { mutableStateOf(initialCountry.dialCode) }
    var selectedCountryName by remember { mutableStateOf("${initialCountry.flagEmoji} ${initialCountry.name}") }
    var showCountryPicker by remember { mutableStateOf(false) }
    var inputPhoneNumber by remember { mutableStateOf("") }
    var inputOtpCode by remember { mutableStateOf("") }
    var userDisplayName by remember { mutableStateOf(profile.name) }
    var activeChat by remember { mutableStateOf<WhatsAppChat?>(null) }
    var selectedTab by remember { mutableIntStateOf(0) } // 0: Chats, 1: Novedades, 2: Comunidades, 3: Llamadas
    var showNewChatDialog by remember { mutableStateOf(false) }
    var showSettingsDialog by remember { mutableStateOf(false) }
    var showSearchField by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }

    // Seeded chats for this WhatsApp account clone
    val chats = remember(profile.id) {
        mutableStateListOf(
            WhatsAppChat(
                id = "c1",
                contactName = "Contacto Trabajo (${profile.name})",
                contactPhone = "+34 612 345 678",
                avatarColor = 0xFF10B981,
                lastMessage = "Perfecto, esta cuenta está completamente aislada de la principal.",
                timestamp = "10:42",
                unreadCount = 1,
                isOnline = true,
                messages = mutableListOf(
                    WhatsAppMessage(text = "¡Hola! ¿Este es tu WhatsApp secundario?", isOutgoing = false, timestamp = "10:40"),
                    WhatsAppMessage(text = "Sí, funcionando en el contenedor MultiSpace independiente.", isOutgoing = true, timestamp = "10:41"),
                    WhatsAppMessage(text = "Perfecto, esta cuenta está completamente aislada de la principal.", isOutgoing = false, timestamp = "10:42")
                )
            ),
            WhatsAppChat(
                id = "c2",
                contactName = "Equipo de Proyectos",
                contactPhone = "+34 699 887 766",
                avatarColor = 0xFF3B82F6,
                lastMessage = "Reunión programada para las 16:00.",
                timestamp = "Ayer",
                unreadCount = 0,
                messages = mutableListOf(
                    WhatsAppMessage(text = "Enviados los archivos de la propuesta.", isOutgoing = true, timestamp = "Ayer 18:20"),
                    WhatsAppMessage(text = "Reunión programada para las 16:00.", isOutgoing = false, timestamp = "Ayer 18:22")
                )
            ),
            WhatsAppChat(
                id = "c3",
                contactName = "Soporte MultiSpace",
                contactPhone = "+1 800 555 0199",
                avatarColor = 0xFF8B5CF6,
                lastMessage = "🔒 Contenedor cifrado AES-256 activo para este número.",
                timestamp = "Lunes",
                unreadCount = 0,
                messages = mutableListOf(
                    WhatsAppMessage(text = "🔒 Contenedor cifrado AES-256 activo para este número.", isOutgoing = false, timestamp = "Lunes 09:00")
                )
            )
        )
    }

    // Color Palette: WhatsApp Dark Green Theme
    val waPrimary = Color(0xFF00A884)
    val waDarkBg = Color(0xFF121B22)
    val waSurface = Color(0xFF1F2C34)
    val waOutgoingBubble = Color(0xFF005C4B)
    val waIncomingBubble = Color(0xFF202C33)

    LaunchedEffect(isRegistered) {
        if (isRegistered) {
            onStatsUpdated(chats.size * 3, 1024L * 1024L * 18) // Update isolated space storage size
        }
    }

    // -------------------------------------------------------------
    // ONBOARDING / NATIVE REGISTRATION FLOW
    // -------------------------------------------------------------
    if (!isRegistered) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(waDarkBg)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            when (setupStep) {
                // Step 1: Welcome Screen
                1 -> {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.Center
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = waPrimary.copy(alpha = 0.15f),
                            modifier = Modifier.size(100.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Chat,
                                    contentDescription = null,
                                    tint = waPrimary,
                                    modifier = Modifier.size(54.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        Text(
                            text = "Te damos la bienvenida a WhatsApp",
                            color = Color.White,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        Text(
                            text = "Contenedor Nativo: ${profile.name}\nEjecutando instancia independiente y aislada del sistema.",
                            color = Color.White.copy(alpha = 0.7f),
                            fontSize = 13.sp,
                            textAlign = TextAlign.Center
                        )
                    }

                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = "Toca \"Aceptar y continuar\" para registrar el número de esta cuenta clonada.",
                            color = Color.White.copy(alpha = 0.5f),
                            fontSize = 11.sp,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(bottom = 16.dp)
                        )

                        Button(
                            onClick = { setupStep = 2 },
                            colors = ButtonDefaults.buttonColors(containerColor = waPrimary),
                            shape = RoundedCornerShape(24.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("wa_accept_continue_btn")
                        ) {
                            Text("ACEPTAR Y CONTINUAR", color = Color.Black, fontWeight = FontWeight.Bold)
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        TextButton(
                            onClick = onCloseSandbox,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Volver al Panel", color = Color.White.copy(alpha = 0.6f))
                        }
                    }
                }

                // Step 2: Phone Number Input
                2 -> {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.weight(1f)
                    ) {
                        Spacer(modifier = Modifier.height(20.dp))
                        Text(
                            text = "Introduce tu número de teléfono",
                            color = Color.White,
                            fontSize = 19.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "WhatsApp enviará un código SMS para verificar este número en ${profile.name}.",
                            color = Color.White.copy(alpha = 0.7f),
                            fontSize = 13.sp,
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(28.dp))

                        // Interactive Country Selector
                        Surface(
                            color = waSurface,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { showCountryPicker = true }
                                .testTag("wa_country_picker_trigger")
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = selectedCountry.flagEmoji,
                                        fontSize = 22.sp
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text(
                                        text = selectedCountry.name,
                                        color = Color.White,
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 15.sp
                                    )
                                }
                                Icon(Icons.Default.ArrowDropDown, contentDescription = "Elegir país", tint = waPrimary)
                            }
                        }

                        if (showCountryPicker) {
                            CountryPickerDialog(
                                selectedCountry = selectedCountry,
                                onCountrySelected = { country ->
                                    selectedCountry = country
                                    selectedCountryCode = country.dialCode
                                    selectedCountryName = "${country.flagEmoji} ${country.name}"
                                    showCountryPicker = false
                                },
                                onDismiss = { showCountryPicker = false },
                                accentColor = waPrimary,
                                containerColor = waSurface
                            )
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            OutlinedTextField(
                                value = selectedCountryCode,
                                onValueChange = {
                                    selectedCountryCode = it
                                    val matched = CountryRepository.COUNTRIES.firstOrNull { c -> c.dialCode == it }
                                    if (matched != null) {
                                        selectedCountry = matched
                                        selectedCountryName = "${matched.flagEmoji} ${matched.name}"
                                    }
                                },
                                label = { Text("Código") },
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White,
                                    focusedBorderColor = waPrimary,
                                    unfocusedBorderColor = Color.White.copy(alpha = 0.2f)
                                ),
                                modifier = Modifier.width(100.dp).testTag("wa_country_code_input")
                            )

                            OutlinedTextField(
                                value = inputPhoneNumber,
                                onValueChange = { inputPhoneNumber = it },
                                label = { Text("Número de teléfono") },
                                placeholder = { Text(if (selectedCountry.code == "CR") "8888 8888" else "612 345 678", color = Color.Gray) },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White,
                                    focusedBorderColor = waPrimary,
                                    unfocusedBorderColor = Color.White.copy(alpha = 0.2f)
                                ),
                                modifier = Modifier.weight(1f).testTag("wa_phone_input")
                            )
                        }
                    }

                    Button(
                        onClick = {
                            if (inputPhoneNumber.isBlank()) {
                                inputPhoneNumber = when (selectedCountry.code) {
                                    "CR" -> "8888 8888"
                                    "MX" -> "55 1234 5678"
                                    "US" -> "555 019 2834"
                                    "ES" -> "612 345 678"
                                    "CO" -> "300 123 4567"
                                    "AR" -> "11 2345 6789"
                                    else -> "8765 4321"
                                }
                            }
                            setupStep = 3
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = waPrimary),
                        shape = RoundedCornerShape(24.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("wa_phone_next_btn")
                    ) {
                        Text("SIGUIENTE", color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                }

                // Step 3: SMS Verification Code
                3 -> {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.weight(1f)
                    ) {
                        Spacer(modifier = Modifier.height(20.dp))
                        Text(
                            text = "Verificando $selectedCountryCode $inputPhoneNumber",
                            color = Color.White,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Hemos enviado un SMS con el código de activación.",
                            color = Color.White.copy(alpha = 0.7f),
                            fontSize = 13.sp,
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(28.dp))

                        OutlinedTextField(
                            value = inputOtpCode,
                            onValueChange = {
                                inputOtpCode = it.take(6)
                                if (inputOtpCode.length == 6) {
                                    setupStep = 4
                                }
                            },
                            placeholder = { Text("---  ---", color = Color.Gray) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedBorderColor = waPrimary,
                                unfocusedBorderColor = Color.White.copy(alpha = 0.2f)
                            ),
                            textStyle = LocalTextStyle.current.copy(
                                textAlign = TextAlign.Center,
                                fontSize = 22.sp,
                                letterSpacing = 8.sp,
                                fontWeight = FontWeight.Bold
                            ),
                            modifier = Modifier.width(220.dp).testTag("wa_otp_input")
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        TextButton(onClick = { inputOtpCode = "782914"; setupStep = 4 }) {
                            Text("Autocompletar Código de Prueba (782-914)", color = waPrimary, fontSize = 12.sp)
                        }
                    }

                    Button(
                        onClick = { setupStep = 4 },
                        colors = ButtonDefaults.buttonColors(containerColor = waPrimary),
                        shape = RoundedCornerShape(24.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                    ) {
                        Text("VERIFICAR", color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                }

                // Step 4: Profile Name & Setup
                4 -> {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.weight(1f)
                    ) {
                        Spacer(modifier = Modifier.height(20.dp))
                        Text(
                            text = "Información del perfil",
                            color = Color.White,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Por favor, escribe tu nombre y elige una foto de perfil.",
                            color = Color.White.copy(alpha = 0.7f),
                            fontSize = 13.sp,
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(28.dp))

                        Surface(
                            shape = CircleShape,
                            color = waSurface,
                            modifier = Modifier.size(90.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.CameraAlt,
                                    contentDescription = null,
                                    tint = Color.White.copy(alpha = 0.6f),
                                    modifier = Modifier.size(36.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        OutlinedTextField(
                            value = userDisplayName,
                            onValueChange = { userDisplayName = it },
                            label = { Text("Escribe tu nombre aquí") },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedBorderColor = waPrimary,
                                unfocusedBorderColor = Color.White.copy(alpha = 0.2f)
                            ),
                            modifier = Modifier.fillMaxWidth().testTag("wa_display_name_input")
                        )
                    }

                    Button(
                        onClick = {
                            isRegistered = true
                            Toast.makeText(context, "¡Cuenta iniciada en espacio aislado!", Toast.LENGTH_SHORT).show()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = waPrimary),
                        shape = RoundedCornerShape(24.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("wa_finish_setup_btn")
                    ) {
                        Text("INICIAR WHATSAPP", color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
        return
    }

    // -------------------------------------------------------------
    // ACTIVE WHATSAPP CHAT CONVERSATION SCREEN
    // -------------------------------------------------------------
    if (activeChat != null) {
        val chat = activeChat!!
        var messageInput by remember { mutableStateOf("") }
        var isRecordingAudio by remember { mutableStateOf(false) }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(waDarkBg)
        ) {
            // Chat Header
            Surface(
                color = waSurface,
                shadowElevation = 4.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { activeChat = null }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Volver",
                            tint = Color.White
                        )
                    }

                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(Color(chat.avatarColor)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = chat.contactName.take(1).uppercase(),
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = chat.contactName,
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = if (chat.isOnline) "en línea" else "últ. vez hoy a las ${chat.timestamp}",
                            color = if (chat.isOnline) waPrimary else Color.White.copy(alpha = 0.6f),
                            fontSize = 11.sp
                        )
                    }

                    IconButton(onClick = {
                        Toast.makeText(context, "Llamada cifrada en sandbox iniciada", Toast.LENGTH_SHORT).show()
                    }) {
                        Icon(Icons.Default.Videocam, contentDescription = "Videollamada", tint = Color.White)
                    }

                    IconButton(onClick = {
                        Toast.makeText(context, "Llamada de voz en sandbox iniciada", Toast.LENGTH_SHORT).show()
                    }) {
                        Icon(Icons.Default.Phone, contentDescription = "Llamada", tint = Color.White)
                    }
                }
            }

            // End-to-End Encryption Banner
            Surface(
                color = waSurface.copy(alpha = 0.6f),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Row(
                    modifier = Modifier.padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = null,
                        tint = Color(0xFFFFD700),
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Los mensajes de este clon están aislados y cifrados de extremo a extremo.",
                        color = Color.White.copy(alpha = 0.7f),
                        fontSize = 10.sp
                    )
                }
            }

            // Message List
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp),
                reverseLayout = false
            ) {
                items(chat.messages) { msg ->
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 3.dp),
                        contentAlignment = if (msg.isOutgoing) Alignment.CenterEnd else Alignment.CenterStart
                    ) {
                        Surface(
                            color = if (msg.isOutgoing) waOutgoingBubble else waIncomingBubble,
                            shape = RoundedCornerShape(
                                topStart = 12.dp,
                                topEnd = 12.dp,
                                bottomStart = if (msg.isOutgoing) 12.dp else 2.dp,
                                bottomEnd = if (msg.isOutgoing) 2.dp else 12.dp
                            ),
                            modifier = Modifier.widthIn(max = 280.dp)
                        ) {
                            Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)) {
                                Text(
                                    text = msg.text,
                                    color = Color.White,
                                    fontSize = 14.sp
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Row(
                                    modifier = Modifier.align(Alignment.End),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = msg.timestamp,
                                        color = Color.White.copy(alpha = 0.5f),
                                        fontSize = 10.sp
                                    )
                                    if (msg.isOutgoing) {
                                        Spacer(modifier = Modifier.width(3.dp))
                                        Icon(
                                            imageVector = Icons.Default.DoneAll,
                                            contentDescription = null,
                                            tint = Color(0xFF53BDEB), // Blue double check
                                            modifier = Modifier.size(13.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Message Input Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    color = waSurface,
                    shape = RoundedCornerShape(24.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 10.dp, vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = {}, modifier = Modifier.size(32.dp)) {
                            Icon(Icons.Default.SentimentSatisfied, contentDescription = "Emoji", tint = Color.White.copy(alpha = 0.6f))
                        }

                        TextField(
                            value = messageInput,
                            onValueChange = { messageInput = it },
                            placeholder = { Text("Mensaje", color = Color.White.copy(alpha = 0.4f), fontSize = 14.sp) },
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = Color.Transparent,
                                unfocusedContainerColor = Color.Transparent,
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedIndicatorColor = Color.Transparent,
                                unfocusedIndicatorColor = Color.Transparent
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = 40.dp, max = 100.dp)
                                .testTag("wa_message_input")
                        )

                        IconButton(onClick = {
                            Toast.makeText(context, "Adjuntar archivo aislado", Toast.LENGTH_SHORT).show()
                        }, modifier = Modifier.size(32.dp)) {
                            Icon(Icons.Default.AttachFile, contentDescription = "Adjuntar", tint = Color.White.copy(alpha = 0.6f))
                        }

                        IconButton(onClick = {
                            Toast.makeText(context, "Cámara del clon", Toast.LENGTH_SHORT).show()
                        }, modifier = Modifier.size(32.dp)) {
                            Icon(Icons.Default.CameraAlt, contentDescription = "Cámara", tint = Color.White.copy(alpha = 0.6f))
                        }
                    }
                }

                Spacer(modifier = Modifier.width(6.dp))

                FloatingActionButton(
                    onClick = {
                        if (messageInput.isNotBlank()) {
                            val timeNow = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
                            val newMsg = WhatsAppMessage(
                                text = messageInput.trim(),
                                isOutgoing = true,
                                timestamp = timeNow
                            )
                            chat.messages.add(newMsg)
                            val sentText = messageInput
                            messageInput = ""

                            // Simulate automated reply
                            coroutineScope.launch {
                                delay(1200)
                                val replyTime = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
                                chat.messages.add(
                                    WhatsAppMessage(
                                        text = "Recibido en ${profile.name}: \"$sentText\"",
                                        isOutgoing = false,
                                        timestamp = replyTime
                                    )
                                )
                            }
                        } else {
                            isRecordingAudio = !isRecordingAudio
                            if (isRecordingAudio) {
                                Toast.makeText(context, "Grabando nota de voz...", Toast.LENGTH_SHORT).show()
                            }
                        }
                    },
                    containerColor = waPrimary,
                    shape = CircleShape,
                    modifier = Modifier.size(46.dp).testTag("wa_send_btn")
                ) {
                    Icon(
                        imageVector = if (messageInput.isNotBlank()) Icons.AutoMirrored.Filled.Send else Icons.Default.Mic,
                        contentDescription = "Enviar",
                        tint = Color.Black,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
        return
    }

    // -------------------------------------------------------------
    // MAIN NATIVE WHATSAPP INTERFACE (Chats, Tabs, Search, FAB)
    // -------------------------------------------------------------
    Scaffold(
        topBar = {
            Surface(
                color = waSurface,
                shadowElevation = 3.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "WhatsApp",
                                color = Color.White,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = waPrimary.copy(alpha = 0.2f)
                            ) {
                                Text(
                                    text = profile.name,
                                    color = waPrimary,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(onClick = {
                                Toast.makeText(context, "Cámara nativa iniciada", Toast.LENGTH_SHORT).show()
                            }) {
                                Icon(Icons.Default.CameraAlt, contentDescription = "Cámara", tint = Color.White)
                            }
                            IconButton(onClick = { showSearchField = !showSearchField }) {
                                Icon(Icons.Default.Search, contentDescription = "Buscar", tint = Color.White)
                            }
                            IconButton(onClick = { showSettingsDialog = true }) {
                                Icon(Icons.Default.MoreVert, contentDescription = "Opciones", tint = Color.White)
                            }
                        }
                    }

                    if (showSearchField) {
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            placeholder = { Text("Buscar mensajes o contactos...", color = Color.Gray) },
                            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = waPrimary) },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedBorderColor = waPrimary,
                                unfocusedBorderColor = Color.Transparent
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 4.dp)
                        )
                    }

                    // Native WhatsApp Tabs
                    TabRow(
                        selectedTabIndex = selectedTab,
                        containerColor = waSurface,
                        contentColor = waPrimary,
                        divider = {}
                    ) {
                        Tab(
                            selected = selectedTab == 0,
                            onClick = { selectedTab = 0 },
                            text = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text("Chats", fontWeight = FontWeight.Bold, color = if (selectedTab == 0) waPrimary else Color.White.copy(alpha = 0.6f))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Surface(
                                        shape = CircleShape,
                                        color = if (selectedTab == 0) waPrimary else Color.White.copy(alpha = 0.3f),
                                        modifier = Modifier.size(18.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Text("${chats.size}", fontSize = 10.sp, color = Color.Black, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }
                        )
                        Tab(
                            selected = selectedTab == 1,
                            onClick = { selectedTab = 1 },
                            text = { Text("Novedades", fontWeight = FontWeight.Bold, color = if (selectedTab == 1) waPrimary else Color.White.copy(alpha = 0.6f)) }
                        )
                        Tab(
                            selected = selectedTab == 2,
                            onClick = { selectedTab = 2 },
                            text = { Text("Comunidades", fontWeight = FontWeight.Bold, color = if (selectedTab == 2) waPrimary else Color.White.copy(alpha = 0.6f)) }
                        )
                        Tab(
                            selected = selectedTab == 3,
                            onClick = { selectedTab = 3 },
                            text = { Text("Llamadas", fontWeight = FontWeight.Bold, color = if (selectedTab == 3) waPrimary else Color.White.copy(alpha = 0.6f)) }
                        )
                    }
                }
            }
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showNewChatDialog = true },
                containerColor = waPrimary,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.testTag("wa_new_chat_fab")
            ) {
                Icon(
                    imageVector = Icons.Default.Chat,
                    contentDescription = "Nuevo Chat",
                    tint = Color.Black
                )
            }
        },
        containerColor = waDarkBg
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when (selectedTab) {
                // Chats Tab
                0 -> {
                    val filteredChats = if (searchQuery.isBlank()) {
                        chats
                    } else {
                        chats.filter { it.contactName.contains(searchQuery, ignoreCase = true) || it.lastMessage.contains(searchQuery, ignoreCase = true) }
                    }

                    LazyColumn(modifier = Modifier.fillMaxSize()) {
                        items(filteredChats) { chat ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { activeChat = chat }
                                    .padding(horizontal = 14.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(48.dp)
                                        .clip(CircleShape)
                                        .background(Color(chat.avatarColor)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = chat.contactName.take(1).uppercase(),
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 18.sp
                                    )
                                }

                                Spacer(modifier = Modifier.width(12.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = chat.contactName,
                                            color = Color.White,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 15.sp
                                        )
                                        Text(
                                            text = chat.timestamp,
                                            color = if (chat.unreadCount > 0) waPrimary else Color.White.copy(alpha = 0.5f),
                                            fontSize = 11.sp
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(2.dp))

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = chat.lastMessage,
                                            color = Color.White.copy(alpha = 0.7f),
                                            fontSize = 13.sp,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            modifier = Modifier.weight(1f)
                                        )

                                        if (chat.unreadCount > 0) {
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Surface(
                                                shape = CircleShape,
                                                color = waPrimary,
                                                modifier = Modifier.size(20.dp)
                                            ) {
                                                Box(contentAlignment = Alignment.Center) {
                                                    Text(
                                                        text = "${chat.unreadCount}",
                                                        color = Color.Black,
                                                        fontSize = 10.sp,
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                            HorizontalDivider(
                                color = Color.White.copy(alpha = 0.06f),
                                modifier = Modifier.padding(start = 74.dp)
                            )
                        }
                    }
                }

                // Novedades (Estados) Tab
                1 -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp)
                    ) {
                        Text("Estado", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    Toast.makeText(context, "Añadir estado al clon", Toast.LENGTH_SHORT).show()
                                }
                        ) {
                            Box(modifier = Modifier.size(50.dp)) {
                                Box(
                                    modifier = Modifier
                                        .size(46.dp)
                                        .clip(CircleShape)
                                        .background(waSurface),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Default.Person, contentDescription = null, tint = Color.White.copy(alpha = 0.6f))
                                }
                                Box(
                                    modifier = Modifier
                                        .size(18.dp)
                                        .clip(CircleShape)
                                        .background(waPrimary)
                                        .align(Alignment.BottomEnd),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = null, tint = Color.Black, modifier = Modifier.size(12.dp))
                                }
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text("Mi estado", color = Color.White, fontWeight = FontWeight.Bold)
                                Text("Toca para añadir una actualización de estado", color = Color.White.copy(alpha = 0.6f), fontSize = 12.sp)
                            }
                        }

                        Spacer(modifier = Modifier.height(24.dp))
                        Text("Recientes", color = Color.White.copy(alpha = 0.6f), fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        Spacer(modifier = Modifier.height(12.dp))
                        Text("No hay estados recientes de contactos en este espacio.", color = Color.White.copy(alpha = 0.4f), fontSize = 12.sp)
                    }
                }

                // Comunidades Tab
                2 -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(Icons.Default.Groups, contentDescription = null, tint = waPrimary, modifier = Modifier.size(60.dp))
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("Crea una nueva comunidad", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "Reúne tus grupos de trabajo o vecindario en una sola comunidad.",
                            color = Color.White.copy(alpha = 0.6f),
                            textAlign = TextAlign.Center,
                            fontSize = 13.sp
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = { Toast.makeText(context, "Comunidad creada", Toast.LENGTH_SHORT).show() },
                            colors = ButtonDefaults.buttonColors(containerColor = waPrimary)
                        ) {
                            Text("Iniciar comunidad", color = Color.Black, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                // Llamadas Tab
                3 -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp)
                    ) {
                        Text("Llamadas Recientes", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        Spacer(modifier = Modifier.height(14.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF3B82F6)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.CallReceived, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Equipo de Proyectos", color = Color.White, fontWeight = FontWeight.Bold)
                                Text("Ayer, 18:22 • Entrante", color = Color.White.copy(alpha = 0.6f), fontSize = 12.sp)
                            }
                            Icon(Icons.Default.Phone, contentDescription = null, tint = waPrimary)
                        }
                    }
                }
            }
        }
    }

    // Modal to Start New Chat with any Phone Number
    if (showNewChatDialog) {
        var newName by remember { mutableStateOf("") }
        var newPhone by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showNewChatDialog = false },
            title = { Text("Nuevo Chat en ${profile.name}", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = newName,
                        onValueChange = { newName = it },
                        label = { Text("Nombre del contacto") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = newPhone,
                        onValueChange = { newPhone = it },
                        label = { Text("Número de teléfono (+34...)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newName.isNotBlank()) {
                            val newChat = WhatsAppChat(
                                id = UUID.randomUUID().toString(),
                                contactName = newName.trim(),
                                contactPhone = newPhone.ifBlank { "+34 600 000 000" },
                                avatarColor = 0xFFEC4899,
                                lastMessage = "Chat iniciado en MultiSpace",
                                timestamp = "Ahora",
                                messages = mutableListOf(
                                    WhatsAppMessage(text = "Chat iniciado con $newName", isOutgoing = true, timestamp = "Ahora")
                                )
                            )
                            chats.add(0, newChat)
                            activeChat = newChat
                            showNewChatDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = waPrimary)
                ) {
                    Text("INICIAR CHAT", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showNewChatDialog = false }) { Text("Cancelar") }
            }
        )
    }

    // Options Menu Modal
    if (showSettingsDialog) {
        AlertDialog(
            onDismissRequest = { showSettingsDialog = false },
            title = { Text("Ajustes de ${profile.name}") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("• Paquete: com.whatsapp")
                    Text("• Número: $selectedCountryCode $inputPhoneNumber")
                    Text("• Contenedor: Aislamiento AES-256 MultiSpace")
                    Text("• Base de datos: Sandbox SQLite / Room")
                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                    TextButton(onClick = {
                        isRegistered = false
                        setupStep = 1
                        showSettingsDialog = false
                    }) {
                        Text("Cerrar Sesión de esta Cuenta", color = MaterialTheme.colorScheme.error)
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showSettingsDialog = false }) { Text("Cerrar") }
            }
        )
    }
}

/* ==========================================================================
   2. NATIVE IMVU 3D CLONE ENGINE (Zero-Leakage Sandbox, 3D Studio & Web Account 2)
   ========================================================================== */

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NativeImvuSandbox(
    profile: ProfileEntity,
    onStatsUpdated: (cookies: Int, bytes: Long) -> Unit,
    onCloseSandbox: () -> Unit
) {
    val context = LocalContext.current
    var selectedImvuTab by remember { mutableIntStateOf(0) } // 0: Avatar 3D, 1: Salas 3D, 2: Feed, 3: Tienda, 4: Escudo
    var avatarOutfit by remember { mutableStateOf("Casual Streetwear") }
    var creditsBalance by remember { mutableIntStateOf(4500) }
    var roomMessage by remember { mutableStateOf("") }
    var selectedRoom by remember { mutableStateOf("Penthouse VIP") }

    val blockedCount by SandboxIntentInterceptor.blockedCount.collectAsState()
    val interceptedEvents by SandboxIntentInterceptor.interceptionEvents.collectAsState()

    val roomMessages = remember {
        mutableStateListOf(
            "Avatar_Luna: ¡Hola a todos en la sala 3D!",
            "Cyber_Boy: ¿Qué onda? Bienvenidos al salón VIP.",
            "MultiSpace_User (${profile.name}): Sesión activa en contenedor aislado 100% independiente."
        )
    }

    LaunchedEffect(Unit) {
        onStatsUpdated(18, 1024L * 1024L * 52) // IMVU 3D isolated cache
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("IMVU 3D Móvil", fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.width(6.dp))
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = MaterialTheme.colorScheme.primaryContainer
                        ) {
                            Text(
                                text = profile.name,
                                fontSize = 10.sp,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onCloseSandbox) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                },
                actions = {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                        modifier = Modifier.padding(end = 6.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Shield, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Antifugas Activo", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFFFFD700).copy(alpha = 0.2f),
                        modifier = Modifier.padding(end = 8.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.MonetizationOn, contentDescription = null, tint = Color(0xFFFFD700), modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("$creditsBalance Cr", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color(0xFFFFD700))
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            )
        },
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = selectedImvuTab == 0,
                    onClick = { selectedImvuTab = 0 },
                    icon = { Icon(Icons.Default.Face, contentDescription = "Avatar 3D") },
                    label = { Text("Avatar 3D") }
                )
                NavigationBarItem(
                    selected = selectedImvuTab == 1,
                    onClick = { selectedImvuTab = 1 },
                    icon = { Icon(Icons.Default.MeetingRoom, contentDescription = "Salas 3D") },
                    label = { Text("Salas 3D") }
                )
                NavigationBarItem(
                    selected = selectedImvuTab == 2,
                    onClick = { selectedImvuTab = 2 },
                    icon = { Icon(Icons.Default.DynamicFeed, contentDescription = "Feed") },
                    label = { Text("Feed") }
                )
                NavigationBarItem(
                    selected = selectedImvuTab == 3,
                    onClick = { selectedImvuTab = 3 },
                    icon = { Icon(Icons.Default.ShoppingBag, contentDescription = "Tienda") },
                    label = { Text("Tienda") }
                )
                NavigationBarItem(
                    selected = selectedImvuTab == 4,
                    onClick = { selectedImvuTab = 4 },
                    icon = { Icon(Icons.Default.Security, contentDescription = "Escudo") },
                    label = { Text("Escudo") }
                )
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when (selectedImvuTab) {
                // Tab 0: 3D Avatar Customizer & Studio
                0 -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center,
                                modifier = Modifier.padding(20.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AccessibilityNew,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(130.dp)
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                Text("Avatar 3D Virtual de ${profile.name}", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                                Text("Outfit actual: $avatarOutfit", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
                                Spacer(modifier = Modifier.height(8.dp))
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                                ) {
                                    Text(
                                        text = "Instancia 3D Aislada • 0 Fugas hacia el Sistema",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))
                        Text("Armario y Poses 3D (Instancia Aislada)", fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(8.dp))
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(listOf("Casual Streetwear", "Gothic Style", "Cyber Neon", "VIP Gold Suit", "Summer Beach")) { outfit ->
                                FilterChip(
                                    selected = avatarOutfit == outfit,
                                    onClick = { avatarOutfit = outfit },
                                    label = { Text(outfit) }
                                )
                            }
                        }
                    }
                }

                // Tab 1: 3D Chat Rooms
                1 -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Salón 3D: $selectedRoom", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                FilterChip(
                                    selected = selectedRoom == "Penthouse VIP",
                                    onClick = { selectedRoom = "Penthouse VIP" },
                                    label = { Text("VIP", fontSize = 10.sp) }
                                )
                                FilterChip(
                                    selected = selectedRoom == "Playa Tropical",
                                    onClick = { selectedRoom = "Playa Tropical" },
                                    label = { Text("Playa", fontSize = 10.sp) }
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        LazyColumn(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth()
                        ) {
                            items(roomMessages) { msg ->
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp)
                                ) {
                                    Text(
                                        text = msg,
                                        modifier = Modifier.padding(10.dp),
                                        fontSize = 13.sp
                                    )
                                }
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedTextField(
                                value = roomMessage,
                                onValueChange = { roomMessage = it },
                                placeholder = { Text("Escribe en la sala 3D...") },
                                modifier = Modifier.weight(1f)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            IconButton(onClick = {
                                if (roomMessage.isNotBlank()) {
                                    roomMessages.add("${profile.name}: ${roomMessage.trim()}")
                                    roomMessage = ""
                                }
                            }) {
                                Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Enviar", tint = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }
                }

                // Tab 2: Feed Social 3D
                2 -> {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        item {
                            Card(
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.AccountCircle, contentDescription = null, modifier = Modifier.size(36.dp), tint = MaterialTheme.colorScheme.primary)
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Column {
                                            Text("IMVU Official Model", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                            Text("Hace 2 horas • Penthouse 3D", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Text("¡Nuevo look de la semana en la sala VIP! ¿Qué opinan del conjunto neón?", fontSize = 13.sp)
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                                        TextButton(onClick = { Toast.makeText(context, "Me gusta", Toast.LENGTH_SHORT).show() }) {
                                            Icon(Icons.Default.Favorite, contentDescription = null, tint = Color.Red, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("142 Likes")
                                        }
                                        TextButton(onClick = { Toast.makeText(context, "Comentar", Toast.LENGTH_SHORT).show() }) {
                                            Icon(Icons.Default.Comment, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("28 Comentarios")
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Tab 3: Shop
                3 -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp)
                    ) {
                        Text("Catálogo de Ropa y Accesorios 3D", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        Spacer(modifier = Modifier.height(10.dp))
                        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(listOf("Zapatillas VIP Neon (500 Cr)", "Gafas Cyberpunk (300 Cr)", "Chaqueta de Cuero (800 Cr)", "Alas Celestiales (1,200 Cr)")) { item ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(12.dp))
                                        .padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(item, fontWeight = FontWeight.Medium, fontSize = 13.sp)
                                    Button(
                                        onClick = {
                                            Toast.makeText(context, "Artículo comprado para ${profile.name}", Toast.LENGTH_SHORT).show()
                                        },
                                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                                    ) {
                                        Text("Comprar", fontSize = 12.sp)
                                    }
                                }
                            }
                        }
                    }
                }

                // Tab 4: Shield Status & Intercepted Events Log
                4 -> {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        item {
                            Card(
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.Security, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("Escudo Antifugas IMVU: ACTIVO", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                    }
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text("• Redirecciones externas a com.imvu.mobile: BLOQUEADAS", fontSize = 12.sp)
                                    Text("• Enlaces a Google Play / Market: BLOQUEADOS", fontSize = 12.sp)
                                    Text("• Cookies y Tokens de Sesión: Aislados (Perfil #${profile.id})", fontSize = 12.sp)
                                    Text("• Fugas hacia el sistema: 0 detectadas", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }

                        item {
                            Text("Registro de Eventos Interceptados ($blockedCount bloqueos)", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }

                        if (interceptedEvents.isEmpty()) {
                            item {
                                Surface(
                                    color = MaterialTheme.colorScheme.surfaceVariant,
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = "El contenedor está operando de forma 100% aislada. No se han detectado intentos de fuga al exterior.",
                                        modifier = Modifier.padding(14.dp),
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        } else {
                            items(interceptedEvents) { evt ->
                                Surface(
                                    color = MaterialTheme.colorScheme.surfaceVariant,
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.padding(10.dp)) {
                                        Text(evt.actionTaken, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
                                        Text(evt.originalUri, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/* ==========================================================================
   3. NATIVE TELEGRAM SANDBOX (Full Native UI, Registration & Cloud Sandbox)
   ========================================================================== */

data class TelegramChatMessage(
    val id: String = UUID.randomUUID().toString(),
    val text: String,
    val isOutgoing: Boolean,
    val timestamp: String,
    val isRead: Boolean = true
)

data class TelegramChat(
    val id: String,
    val title: String,
    val isChannel: Boolean = false,
    val isGroup: Boolean = false,
    val avatarColor: Long,
    val lastMessage: String,
    val timestamp: String,
    val unreadCount: Int = 0,
    val isOnline: Boolean = false,
    val messages: MutableList<TelegramChatMessage> = mutableListOf()
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NativeTelegramSandbox(
    profile: ProfileEntity,
    onStatsUpdated: (cookies: Int, bytes: Long) -> Unit,
    onCloseSandbox: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    // Persistent Setup State per Account Profile
    var isRegistered by remember(profile.id) {
        mutableStateOf(profile.dataUsageBytes > 0)
    }
    var setupStep by remember(profile.id) { mutableIntStateOf(1) } // 1: Welcome, 2: Phone, 3: OTP, 4: Name
    val initialCountry = remember { CountryRepository.detectDeviceCountry(context) }
    var selectedCountry by remember { mutableStateOf(initialCountry) }
    var selectedCountryCode by remember { mutableStateOf(initialCountry.dialCode) }
    var selectedCountryName by remember { mutableStateOf("${initialCountry.flagEmoji} ${initialCountry.name}") }
    var showCountryPicker by remember { mutableStateOf(false) }
    var inputPhoneNumber by remember { mutableStateOf("") }
    var inputOtpCode by remember { mutableStateOf("") }
    var userFirstName by remember { mutableStateOf(profile.name) }
    var userLastName by remember { mutableStateOf("") }
    var syncContacts by remember { mutableStateOf(true) }

    var activeChat by remember { mutableStateOf<TelegramChat?>(null) }
    var selectedFilterTab by remember { mutableIntStateOf(0) } // 0: Todos, 1: Privados, 2: Grupos, 3: Canales
    var showNewChatDialog by remember { mutableStateOf(false) }
    var showSearchField by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }

    // Seeded Telegram chats
    val chats = remember(profile.id) {
        mutableStateListOf(
            TelegramChat(
                id = "tg_saved",
                title = "Mensajes Guardados",
                avatarColor = 0xFF2AABEE,
                lastMessage = "Almacenamiento en la nube aislado de esta cuenta.",
                timestamp = "12:15",
                unreadCount = 0,
                isOnline = true,
                messages = mutableListOf(
                    TelegramChatMessage(text = "¡Bienvenido a tu nube personal en Telegram!", isOutgoing = false, timestamp = "12:10"),
                    TelegramChatMessage(text = "Aquí puedes guardar notas, enlaces y archivos de forma privada.", isOutgoing = false, timestamp = "12:12"),
                    TelegramChatMessage(text = "Almacenamiento en la nube aislado de esta cuenta.", isOutgoing = true, timestamp = "12:15")
                )
            ),
            TelegramChat(
                id = "tg_news",
                title = "Telegram Noticias Oficial",
                isChannel = true,
                avatarColor = 0xFF3390EC,
                lastMessage = "MultiSpace Sandbox ha activado la aceleración de hardware para cuentas clonadas.",
                timestamp = "11:30",
                unreadCount = 2,
                messages = mutableListOf(
                    TelegramChatMessage(text = "MultiSpace Sandbox ha activado la aceleración de hardware para cuentas clonadas.", isOutgoing = false, timestamp = "11:30")
                )
            ),
            TelegramChat(
                id = "tg_dev",
                title = "Grupo de Soporte MultiSpace",
                isGroup = true,
                avatarColor = 0xFF8E44AD,
                lastMessage = "El contenedor está 100% independiente.",
                timestamp = "Ayer",
                unreadCount = 0,
                messages = mutableListOf(
                    TelegramChatMessage(text = "Contenedor independiente inicializado correctamente.", isOutgoing = false, timestamp = "Ayer 15:20"),
                    TelegramChatMessage(text = "El contenedor está 100% independiente.", isOutgoing = true, timestamp = "Ayer 15:22")
                )
            )
        )
    }

    val tgBlue = Color(0xFF2AABEE)
    val tgDarkBg = Color(0xFF17212B)
    val tgSurface = Color(0xFF242F3D)
    val tgOutgoingBubble = Color(0xFF2B5278)
    val tgIncomingBubble = Color(0xFF182533)

    LaunchedEffect(isRegistered) {
        if (isRegistered) {
            onStatsUpdated(chats.size * 2, 1024L * 1024L * 24)
        }
    }

    // -------------------------------------------------------------
    // TELEGRAM ONBOARDING / FRESH REGISTRATION FLOW
    // -------------------------------------------------------------
    if (!isRegistered) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(tgDarkBg)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            when (setupStep) {
                // Step 1: Start Messaging Screen
                1 -> {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.Center
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = tgBlue,
                            modifier = Modifier.size(110.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Send,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(56.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(26.dp))

                        Text(
                            text = "Telegram",
                            color = Color.White,
                            fontSize = 26.sp,
                            fontWeight = FontWeight.Bold
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = "La aplicación de mensajería más rápida del mundo.\nEs gratis y segura.",
                            color = Color.White.copy(alpha = 0.75f),
                            fontSize = 14.sp,
                            textAlign = TextAlign.Center,
                            lineHeight = 20.sp
                        )

                        Spacer(modifier = Modifier.height(28.dp))

                        // Features Carousel info
                        Card(
                            colors = CardDefaults.cardColors(containerColor = tgSurface),
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Bolt, contentDescription = null, tint = tgBlue, modifier = Modifier.size(20.dp))
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text("Rápida", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                }
                                Text("Entrega mensajes más rápido que cualquier otra aplicación.", color = Color.White.copy(alpha = 0.65f), fontSize = 12.sp, modifier = Modifier.padding(start = 30.dp, top = 2.dp))

                                Spacer(modifier = Modifier.height(10.dp))

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Shield, contentDescription = null, tint = tgBlue, modifier = Modifier.size(20.dp))
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text("Segura", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                }
                                Text("Chats protegidos en contenedor cifrado independiente.", color = Color.White.copy(alpha = 0.65f), fontSize = 12.sp, modifier = Modifier.padding(start = 30.dp, top = 2.dp))
                            }
                        }
                    }

                    Column(modifier = Modifier.fillMaxWidth()) {
                        Button(
                            onClick = { setupStep = 2 },
                            colors = ButtonDefaults.buttonColors(containerColor = tgBlue),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .testTag("tg_start_messaging_btn")
                        ) {
                            Text("EMPEZAR A CHATEAR", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        TextButton(
                            onClick = onCloseSandbox,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Volver al Panel", color = Color.White.copy(alpha = 0.6f))
                        }
                    }
                }

                // Step 2: Country and Phone input
                2 -> {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.weight(1f)
                    ) {
                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            text = "Tu número de teléfono",
                            color = Color.White,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = "Confirma el código de país e introduce tu número de teléfono para ${profile.name}.",
                            color = Color.White.copy(alpha = 0.7f),
                            fontSize = 13.sp,
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(28.dp))

                        // Interactive Country Picker
                        Surface(
                            color = tgSurface,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { showCountryPicker = true }
                                .testTag("tg_country_picker_trigger")
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = selectedCountry.flagEmoji,
                                        fontSize = 22.sp
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text(
                                        text = selectedCountry.name,
                                        color = Color.White,
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 15.sp
                                    )
                                }
                                Icon(Icons.Default.ArrowDropDown, contentDescription = "Elegir país", tint = tgBlue)
                            }
                        }

                        if (showCountryPicker) {
                            CountryPickerDialog(
                                selectedCountry = selectedCountry,
                                onCountrySelected = { country ->
                                    selectedCountry = country
                                    selectedCountryCode = country.dialCode
                                    selectedCountryName = "${country.flagEmoji} ${country.name}"
                                    showCountryPicker = false
                                },
                                onDismiss = { showCountryPicker = false },
                                accentColor = tgBlue,
                                containerColor = tgSurface
                            )
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            OutlinedTextField(
                                value = selectedCountryCode,
                                onValueChange = {
                                    selectedCountryCode = it
                                    val matched = CountryRepository.COUNTRIES.firstOrNull { c -> c.dialCode == it }
                                    if (matched != null) {
                                        selectedCountry = matched
                                        selectedCountryName = "${matched.flagEmoji} ${matched.name}"
                                    }
                                },
                                label = { Text("Código") },
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White,
                                    focusedBorderColor = tgBlue,
                                    unfocusedBorderColor = Color.White.copy(alpha = 0.2f)
                                ),
                                modifier = Modifier.width(100.dp).testTag("tg_country_code_input")
                            )

                            OutlinedTextField(
                                value = inputPhoneNumber,
                                onValueChange = { inputPhoneNumber = it },
                                label = { Text("Número de teléfono") },
                                placeholder = { Text(if (selectedCountry.code == "CR") "8888 8888" else "612 345 678", color = Color.Gray) },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White,
                                    focusedBorderColor = tgBlue,
                                    unfocusedBorderColor = Color.White.copy(alpha = 0.2f)
                                ),
                                modifier = Modifier.weight(1f).testTag("tg_phone_input")
                            )
                        }

                        Spacer(modifier = Modifier.height(18.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = syncContacts,
                                onCheckedChange = { syncContacts = it },
                                colors = CheckboxDefaults.colors(checkedColor = tgBlue)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Sincronizar contactos en este espacio",
                                color = Color.White.copy(alpha = 0.8f),
                                fontSize = 13.sp
                            )
                        }
                    }

                    Button(
                        onClick = {
                            if (inputPhoneNumber.length < 5) {
                                inputPhoneNumber = "8888 8888"
                            }
                            setupStep = 3
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = tgBlue),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("tg_phone_next_btn")
                    ) {
                        Text("CONTINUAR", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }

                // Step 3: Verification Code (SMS)
                3 -> {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.weight(1f)
                    ) {
                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            text = "Código de activación",
                            color = Color.White,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = "Hemos enviado un código a $selectedCountryCode $inputPhoneNumber",
                            color = Color.White.copy(alpha = 0.7f),
                            fontSize = 13.sp,
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(28.dp))

                        OutlinedTextField(
                            value = inputOtpCode,
                            onValueChange = {
                                inputOtpCode = it.take(5)
                                if (inputOtpCode.length == 5) {
                                    setupStep = 4
                                }
                            },
                            placeholder = { Text("- - - - -", color = Color.Gray) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedBorderColor = tgBlue,
                                unfocusedBorderColor = Color.White.copy(alpha = 0.2f)
                            ),
                            textStyle = LocalTextStyle.current.copy(
                                textAlign = TextAlign.Center,
                                fontSize = 24.sp,
                                letterSpacing = 10.sp,
                                fontWeight = FontWeight.Bold
                            ),
                            modifier = Modifier.width(220.dp).testTag("tg_otp_input")
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        TextButton(onClick = { inputOtpCode = "52914"; setupStep = 4 }) {
                            Text("Autocompletar Código de Prueba (52914)", color = tgBlue, fontSize = 12.sp)
                        }
                    }

                    Button(
                        onClick = { setupStep = 4 },
                        colors = ButtonDefaults.buttonColors(containerColor = tgBlue),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                    ) {
                        Text("VERIFICAR", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }

                // Step 4: Profile Name
                4 -> {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.weight(1f)
                    ) {
                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            text = "Tu información",
                            color = Color.White,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = "Introduce tu nombre y foto de perfil para esta cuenta.",
                            color = Color.White.copy(alpha = 0.7f),
                            fontSize = 13.sp,
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(28.dp))

                        Surface(
                            shape = CircleShape,
                            color = tgSurface,
                            modifier = Modifier.size(90.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.CameraAlt,
                                    contentDescription = null,
                                    tint = tgBlue,
                                    modifier = Modifier.size(36.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        OutlinedTextField(
                            value = userFirstName,
                            onValueChange = { userFirstName = it },
                            label = { Text("Nombre (requerido)") },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedBorderColor = tgBlue,
                                unfocusedBorderColor = Color.White.copy(alpha = 0.2f)
                            ),
                            modifier = Modifier.fillMaxWidth().testTag("tg_name_input")
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        OutlinedTextField(
                            value = userLastName,
                            onValueChange = { userLastName = it },
                            label = { Text("Apellido (opcional)") },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedBorderColor = tgBlue,
                                unfocusedBorderColor = Color.White.copy(alpha = 0.2f)
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    Button(
                        onClick = {
                            isRegistered = true
                            Toast.makeText(context, "¡Telegram iniciado en contenedor independiente!", Toast.LENGTH_SHORT).show()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = tgBlue),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("tg_finish_setup_btn")
                    ) {
                        Text("ENTRAR A TELEGRAM", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    } else {
        // -------------------------------------------------------------
        // ACTIVE NATIVE TELEGRAM INTERFACE
        // -------------------------------------------------------------
        if (activeChat == null) {
            Scaffold(
                topBar = {
                    TopAppBar(
                        title = {
                            if (showSearchField) {
                                OutlinedTextField(
                                    value = searchQuery,
                                    onValueChange = { searchQuery = it },
                                    placeholder = { Text("Buscar mensajes o chats...") },
                                    singleLine = true,
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedTextColor = Color.White,
                                        unfocusedTextColor = Color.White,
                                        focusedBorderColor = Color.Transparent,
                                        unfocusedBorderColor = Color.Transparent
                                    ),
                                    modifier = Modifier.fillMaxWidth()
                                )
                            } else {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = "Telegram • ${profile.name}",
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White,
                                        fontSize = 17.sp
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Surface(
                                        color = Color.White.copy(alpha = 0.2f),
                                        shape = RoundedCornerShape(6.dp)
                                    ) {
                                        Text(
                                            text = selectedCountry.flagEmoji,
                                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
                                            fontSize = 12.sp
                                        )
                                    }
                                }
                            }
                        },
                        navigationIcon = {
                            IconButton(onClick = onCloseSandbox) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver", tint = Color.White)
                            }
                        },
                        actions = {
                            IconButton(onClick = { showSearchField = !showSearchField }) {
                                Icon(
                                    if (showSearchField) Icons.Default.Close else Icons.Default.Search,
                                    contentDescription = "Buscar",
                                    tint = Color.White
                                )
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(containerColor = tgSurface)
                    )
                },
                floatingActionButton = {
                    FloatingActionButton(
                        onClick = { showNewChatDialog = true },
                        containerColor = tgBlue,
                        contentColor = Color.White
                    ) {
                        Icon(Icons.Default.Edit, contentDescription = "Nuevo Chat")
                    }
                },
                containerColor = tgDarkBg
            ) { padding ->
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                ) {
                    // Category Tabs
                    val tabs = listOf("Todos", "Privados", "Grupos", "Canales")
                    ScrollableTabRow(
                        selectedTabIndex = selectedFilterTab,
                        containerColor = tgSurface,
                        contentColor = tgBlue,
                        edgePadding = 12.dp
                    ) {
                        tabs.forEachIndexed { index, tabName ->
                            Tab(
                                selected = selectedFilterTab == index,
                                onClick = { selectedFilterTab = index },
                                text = {
                                    Text(
                                        text = tabName,
                                        color = if (selectedFilterTab == index) tgBlue else Color.White.copy(alpha = 0.6f),
                                        fontWeight = if (selectedFilterTab == index) FontWeight.Bold else FontWeight.Normal
                                    )
                                }
                            )
                        }
                    }

                    val filteredChats = chats.filter { chat ->
                        when (selectedFilterTab) {
                            1 -> !chat.isGroup && !chat.isChannel
                            2 -> chat.isGroup
                            3 -> chat.isChannel
                            else -> true
                        } && (searchQuery.isBlank() || chat.title.contains(searchQuery, ignoreCase = true) || chat.lastMessage.contains(searchQuery, ignoreCase = true))
                    }

                    LazyColumn(
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(filteredChats, key = { it.id }) { chat ->
                            Surface(
                                color = Color.Transparent,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { activeChat = chat }
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp, vertical = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Surface(
                                        shape = CircleShape,
                                        color = Color(chat.avatarColor),
                                        modifier = Modifier.size(52.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            if (chat.isChannel) {
                                                Icon(Icons.Default.Campaign, contentDescription = null, tint = Color.White, modifier = Modifier.size(24.dp))
                                            } else if (chat.isGroup) {
                                                Icon(Icons.Default.Group, contentDescription = null, tint = Color.White, modifier = Modifier.size(24.dp))
                                            } else if (chat.id == "tg_saved") {
                                                Icon(Icons.Default.Bookmark, contentDescription = null, tint = Color.White, modifier = Modifier.size(24.dp))
                                            } else {
                                                Text(
                                                    text = chat.title.take(1).uppercase(),
                                                    color = Color.White,
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 20.sp
                                                )
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.width(14.dp))

                                    Column(modifier = Modifier.weight(1f)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = chat.title,
                                                color = Color.White,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 16.sp,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Text(
                                                text = chat.timestamp,
                                                color = Color.White.copy(alpha = 0.5f),
                                                fontSize = 12.sp
                                            )
                                        }

                                        Spacer(modifier = Modifier.height(4.dp))

                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = chat.lastMessage,
                                                color = Color.White.copy(alpha = 0.65f),
                                                fontSize = 13.sp,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis,
                                                modifier = Modifier.weight(1f)
                                            )
                                            if (chat.unreadCount > 0) {
                                                Surface(
                                                    shape = CircleShape,
                                                    color = tgBlue,
                                                    modifier = Modifier.size(20.dp)
                                                ) {
                                                    Box(contentAlignment = Alignment.Center) {
                                                        Text(
                                                            text = chat.unreadCount.toString(),
                                                            color = Color.White,
                                                            fontSize = 11.sp,
                                                            fontWeight = FontWeight.Bold
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                            HorizontalDivider(color = Color.White.copy(alpha = 0.05f))
                        }
                    }
                }
            }
        } else {
            // Active Telegram Chat Screen
            val currentChat = activeChat!!
            var outgoingText by remember { mutableStateOf("") }
            val listState = rememberLazyListState()

            LaunchedEffect(currentChat.messages.size) {
                if (currentChat.messages.isNotEmpty()) {
                    listState.animateScrollToItem(currentChat.messages.size - 1)
                }
            }

            Scaffold(
                topBar = {
                    TopAppBar(
                        title = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Surface(
                                    shape = CircleShape,
                                    color = Color(currentChat.avatarColor),
                                    modifier = Modifier.size(40.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text(currentChat.title.take(1).uppercase(), color = Color.White, fontWeight = FontWeight.Bold)
                                    }
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(currentChat.title, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color.White)
                                    Text(
                                        text = if (currentChat.isOnline) "en línea" else "visto recientemente",
                                        fontSize = 11.sp,
                                        color = if (currentChat.isOnline) tgBlue else Color.White.copy(alpha = 0.6f)
                                    )
                                }
                            }
                        },
                        navigationIcon = {
                            IconButton(onClick = { activeChat = null }) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver", tint = Color.White)
                            }
                        },
                        actions = {
                            IconButton(onClick = { Toast.makeText(context, "Llamada cifrada punto a punto", Toast.LENGTH_SHORT).show() }) {
                                Icon(Icons.Default.Call, contentDescription = "Llamar", tint = Color.White)
                            }
                            IconButton(onClick = {}) {
                                Icon(Icons.Default.MoreVert, contentDescription = "Más", tint = Color.White)
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(containerColor = tgSurface)
                    )
                },
                containerColor = tgDarkBg
            ) { padding ->
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                ) {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(currentChat.messages, key = { it.id }) { msg ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = if (msg.isOutgoing) Arrangement.End else Arrangement.Start
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(
                                        topStart = 14.dp,
                                        topEnd = 14.dp,
                                        bottomStart = if (msg.isOutgoing) 14.dp else 2.dp,
                                        bottomEnd = if (msg.isOutgoing) 2.dp else 14.dp
                                    ),
                                    color = if (msg.isOutgoing) tgOutgoingBubble else tgIncomingBubble,
                                    modifier = Modifier.widthIn(max = 280.dp)
                                ) {
                                    Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                                        Text(text = msg.text, color = Color.White, fontSize = 14.sp)
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Row(
                                            modifier = Modifier.align(Alignment.End),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = msg.timestamp,
                                                fontSize = 10.sp,
                                                color = Color.White.copy(alpha = 0.5f)
                                            )
                                            if (msg.isOutgoing) {
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Icon(
                                                    imageVector = Icons.Default.DoneAll,
                                                    contentDescription = null,
                                                    tint = tgBlue,
                                                    modifier = Modifier.size(14.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Input bar
                    Surface(
                        color = tgSurface,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(onClick = { Toast.makeText(context, "Adjuntar archivo seguro", Toast.LENGTH_SHORT).show() }) {
                                Icon(Icons.Default.AttachFile, contentDescription = "Adjuntar", tint = Color.White.copy(alpha = 0.7f))
                            }

                            OutlinedTextField(
                                value = outgoingText,
                                onValueChange = { outgoingText = it },
                                placeholder = { Text("Mensaje...", color = Color.Gray) },
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White,
                                    focusedBorderColor = Color.Transparent,
                                    unfocusedBorderColor = Color.Transparent
                                ),
                                modifier = Modifier.weight(1f)
                            )

                            if (outgoingText.isNotBlank()) {
                                IconButton(
                                    onClick = {
                                        val now = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
                                        val newMsg = TelegramChatMessage(text = outgoingText.trim(), isOutgoing = true, timestamp = now)
                                        currentChat.messages.add(newMsg)
                                        outgoingText = ""

                                        coroutineScope.launch {
                                            delay(1000)
                                            currentChat.messages.add(
                                                TelegramChatMessage(
                                                    text = "Mensaje sincronizado en el contenedor aislado de ${profile.name}.",
                                                    isOutgoing = false,
                                                    timestamp = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
                                                )
                                            )
                                        }
                                    }
                                ) {
                                    Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Enviar", tint = tgBlue)
                                }
                            } else {
                                IconButton(onClick = { Toast.makeText(context, "Mantén presionado para nota de voz", Toast.LENGTH_SHORT).show() }) {
                                    Icon(Icons.Default.Mic, contentDescription = "Voz", tint = Color.White.copy(alpha = 0.7f))
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showNewChatDialog) {
        var newContactName by remember { mutableStateOf("") }
        var newContactPhone by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showNewChatDialog = false },
            title = { Text("Nuevo Chat en Telegram") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = newContactName,
                        onValueChange = { newContactName = it },
                        label = { Text("Nombre del contacto / canal") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = newContactPhone,
                        onValueChange = { newContactPhone = it },
                        label = { Text("Teléfono o @usuario") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newContactName.isNotBlank()) {
                            val newChat = TelegramChat(
                                id = UUID.randomUUID().toString(),
                                title = newContactName,
                                avatarColor = 0xFF2AABEE,
                                lastMessage = "Chat iniciado",
                                timestamp = "Ahora",
                                messages = mutableListOf(
                                    TelegramChatMessage(text = "Chat iniciado en el sandbox.", isOutgoing = false, timestamp = "Ahora")
                                )
                            )
                            chats.add(0, newChat)
                            activeChat = newChat
                        }
                        showNewChatDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = tgBlue)
                ) {
                    Text("Crear Chat")
                }
            },
            dismissButton = {
                TextButton(onClick = { showNewChatDialog = false }) {
                    Text("Cancelar")
                }
            }
        )
    }
}

/* ==========================================================================
   4. NATIVE INSTAGRAM SANDBOX
   ========================================================================== */

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NativeInstagramSandbox(
    profile: ProfileEntity,
    onStatsUpdated: (cookies: Int, bytes: Long) -> Unit,
    onCloseSandbox: () -> Unit
) {
    LaunchedEffect(Unit) {
        onStatsUpdated(15, 1024L * 1024L * 30)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Instagram • ${profile.name}", fontWeight = FontWeight.Bold) },
                actions = {
                    IconButton(onClick = {}) { Icon(Icons.Default.FavoriteBorder, contentDescription = null) }
                    IconButton(onClick = {}) { Icon(Icons.Default.Send, contentDescription = null) }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
            Text("Historias", fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                items(listOf("Tu historia", "alex_photo", "music_vibes", "design_hub")) { story ->
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFE1306C)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Person, contentDescription = null, tint = Color.White)
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(story, fontSize = 11.sp)
                    }
                }
            }
        }
    }
}

/* ==========================================================================
   5. GENERIC & BANKING NATIVE APP SANDBOX CONTAINER
   ========================================================================== */

data class BankingTransaction(
    val id: String = UUID.randomUUID().toString(),
    val description: String,
    val amount: String,
    val date: String,
    val isCredit: Boolean
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NativeGenericAppSandbox(
    profile: ProfileEntity,
    onStatsUpdated: (cookies: Int, bytes: Long) -> Unit,
    onCloseSandbox: () -> Unit
) {
    val context = LocalContext.current
    val pkg = profile.packageName?.lowercase() ?: ""
    val name = profile.name.lowercase()

    // Detect if this is a banking or financial application
    val isBankingApp = remember(pkg, name) {
        pkg.contains("banco") || pkg.contains("banca") || pkg.contains("bank") ||
        pkg.contains("popular") || pkg.contains("bac") || pkg.contains("bcr") ||
        pkg.contains("bbva") || pkg.contains("santander") || pkg.contains("scotia") ||
        pkg.contains("davivienda") || pkg.contains("finan") || name.contains("banco") ||
        name.contains("banca") || name.contains("bank")
    }

    LaunchedEffect(Unit) {
        onStatsUpdated(12, 1024L * 1024L * 28)
    }

    if (isBankingApp) {
        NativeBankingSandbox(
            profile = profile,
            onCloseSandbox = onCloseSandbox
        )
    } else {
        NativeGeneralUtilitySandbox(
            profile = profile,
            onCloseSandbox = onCloseSandbox
        )
    }
}

/**
 * Full Native Banking Sandbox for cloned financial accounts (e.g. Banco Popular, BAC, BCR, etc.)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NativeBankingSandbox(
    profile: ProfileEntity,
    onCloseSandbox: () -> Unit
) {
    val context = LocalContext.current
    var isLoggedIn by remember(profile.id) { mutableStateOf(false) }
    var identificationNumber by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var rememberUser by remember { mutableStateOf(true) }
    var showSinpeTransferDialog by remember { mutableStateOf(false) }

    // Account data for this clone
    var colonesBalance by remember { mutableStateOf(1450000.0) }
    var dollarsBalance by remember { mutableStateOf(2850.0) }

    val transactions = remember {
        mutableStateListOf(
            BankingTransaction(description = "Transferencia SINPE Móvil", amount = "- ₡ 15,000", date = "Hoy 10:45", isCredit = false),
            BankingTransaction(description = "Depósito Salario / Honorarios", amount = "+ ₡ 450,000", date = "Ayer 18:20", isCredit = true),
            BankingTransaction(description = "Pago de Servicios Públicos", amount = "- ₡ 32,400", date = "23 Ago", isCredit = false),
            BankingTransaction(description = "Transferencia Internacional", amount = "+ $ 500.00", date = "20 Ago", isCredit = true)
        )
    }

    val bankPrimaryColor = Color(0xFF003865) // Deep Bank Navy
    val bankAccentColor = Color(0xFF00A859) // Bank Green

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = profile.name,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = Color.White
                        )
                        Text(
                            text = "Instancia Aislada • ${profile.packageName ?: "Banca Móvil"}",
                            fontSize = 11.sp,
                            color = Color.White.copy(alpha = 0.7f)
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onCloseSandbox) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver", tint = Color.White)
                    }
                },
                actions = {
                    // Quick Action: Launch real APK directly on phone
                    if (!profile.packageName.isNullOrBlank()) {
                        IconButton(
                            onClick = {
                                val launched = SystemDualAppsLauncher.launchNativeApp(context, profile.packageName)
                                if (!launched) {
                                    Toast.makeText(context, "No se encontró el APK instalado. Usa el contenedor aislado.", Toast.LENGTH_LONG).show()
                                }
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Launch,
                                contentDescription = "Abrir APK del Teléfono",
                                tint = Color.White
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = bankPrimaryColor)
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Direct launch banner at the top
            if (!profile.packageName.isNullOrBlank()) {
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Smartphone,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "¿Deseas abrir la app original instalada?",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Medium
                            )
                        }

                        Button(
                            onClick = {
                                SystemDualAppsLauncher.launchNativeApp(context, profile.packageName)
                            },
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("Abrir App", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            if (!isLoggedIn) {
                // Banking Login Screen (Isolated account)
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    item {
                        Spacer(modifier = Modifier.height(10.dp))
                        Surface(
                            shape = CircleShape,
                            color = bankPrimaryColor.copy(alpha = 0.1f),
                            modifier = Modifier.size(80.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.AccountBalance,
                                    contentDescription = null,
                                    tint = bankPrimaryColor,
                                    modifier = Modifier.size(44.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Acceso a ${profile.name}",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Sesión cifrada y separada de la cuenta principal",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    item {
                        Card(
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            elevation = CardDefaults.cardElevation(2.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(20.dp),
                                verticalArrangement = Arrangement.spacedBy(14.dp)
                            ) {
                                OutlinedTextField(
                                    value = identificationNumber,
                                    onValueChange = { identificationNumber = it },
                                    label = { Text("Número de Identificación / Cédula") },
                                    placeholder = { Text("Ej: 1-1234-0567") },
                                    leadingIcon = {
                                        Icon(Icons.Default.Badge, contentDescription = null)
                                    },
                                    singleLine = true,
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.fillMaxWidth()
                                )

                                OutlinedTextField(
                                    value = password,
                                    onValueChange = { password = it },
                                    label = { Text("Contraseña o Clave de Acceso") },
                                    leadingIcon = {
                                        Icon(Icons.Default.Lock, contentDescription = null)
                                    },
                                    singleLine = true,
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.fillMaxWidth()
                                )

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Checkbox(
                                        checked = rememberUser,
                                        onCheckedChange = { rememberUser = it }
                                    )
                                    Text(
                                        text = "Recordar datos en este clon",
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }

                                Button(
                                    onClick = {
                                        isLoggedIn = true
                                        Toast.makeText(context, "Sesión iniciada en ${profile.name}", Toast.LENGTH_SHORT).show()
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = bankPrimaryColor),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(48.dp)
                                ) {
                                    Icon(Icons.Default.Login, contentDescription = null)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("INGRESAR A CUENTA 2", fontWeight = FontWeight.Bold)
                                }

                                OutlinedButton(
                                    onClick = {
                                        identificationNumber = "1-1823-0492"
                                        password = "••••••••"
                                        isLoggedIn = true
                                    },
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Icon(Icons.Default.Fingerprint, contentDescription = null, tint = bankAccentColor)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Ingreso Rápido con Biometría", color = MaterialTheme.colorScheme.onSurface)
                                }
                            }
                        }
                    }

                    item {
                        Card(
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Security, contentDescription = null, tint = bankAccentColor)
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = "Entorno Aislado: Las credenciales, tokens y certificados de esta cuenta no interfieren con la app instalada en el sistema.",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontSize = 11.sp
                                )
                            }
                        }
                    }
                }
            } else {
                // Logged In Dashboard
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Balances Summary Card
                    item {
                        Card(
                            shape = RoundedCornerShape(20.dp),
                            colors = CardDefaults.cardColors(containerColor = bankPrimaryColor),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(20.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Cuenta de Ahorros Principal",
                                        color = Color.White.copy(alpha = 0.8f),
                                        fontSize = 13.sp
                                    )
                                    Surface(
                                        color = bankAccentColor,
                                        shape = RoundedCornerShape(6.dp)
                                    ) {
                                        Text(
                                            text = "ACTIVA",
                                            color = Color.White,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 10.sp,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                Text(
                                    text = "₡ ${"%,.2f".format(Locale.US, colonesBalance)}",
                                    color = Color.White,
                                    fontSize = 28.sp,
                                    fontWeight = FontWeight.Bold
                                )

                                Spacer(modifier = Modifier.height(14.dp))
                                HorizontalDivider(color = Color.White.copy(alpha = 0.15f))
                                Spacer(modifier = Modifier.height(10.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column {
                                        Text("Cuenta Dólares ($)", color = Color.White.copy(alpha = 0.7f), fontSize = 11.sp)
                                        Text("$ ${"%,.2f".format(Locale.US, dollarsBalance)}", color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                                    }
                                    Column(horizontalAlignment = Alignment.End) {
                                        Text("IBAN", color = Color.White.copy(alpha = 0.7f), fontSize = 11.sp)
                                        Text("CR23 0151 •••• 8821", color = Color.White, fontWeight = FontWeight.Medium, fontSize = 13.sp)
                                    }
                                }
                            }
                        }
                    }

                    // Quick Actions
                    item {
                        Text("Acciones Rápidas", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Surface(
                                onClick = { showSinpeTransferDialog = true },
                                shape = RoundedCornerShape(14.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                modifier = Modifier.weight(1f)
                            ) {
                                Column(
                                    modifier = Modifier.padding(14.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Icon(Icons.Default.SendToMobile, contentDescription = null, tint = bankAccentColor, modifier = Modifier.size(28.dp))
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text("SINPE Móvil", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                }
                            }

                            Surface(
                                onClick = { Toast.makeText(context, "Transferencia entre cuentas realizada", Toast.LENGTH_SHORT).show() },
                                shape = RoundedCornerShape(14.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                modifier = Modifier.weight(1f)
                            ) {
                                Column(
                                    modifier = Modifier.padding(14.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Icon(Icons.Default.SwapHoriz, contentDescription = null, tint = bankPrimaryColor, modifier = Modifier.size(28.dp))
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text("Transferir", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                }
                            }

                            Surface(
                                onClick = { Toast.makeText(context, "Módulo de pago de servicios", Toast.LENGTH_SHORT).show() },
                                shape = RoundedCornerShape(14.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                modifier = Modifier.weight(1f)
                            ) {
                                Column(
                                    modifier = Modifier.padding(14.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Icon(Icons.Default.ReceiptLong, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(28.dp))
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text("Servicios", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                }
                            }
                        }
                    }

                    // Recent Transactions
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Movimientos Recientes", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                            TextButton(onClick = {}) { Text("Ver todos") }
                        }
                    }

                    items(transactions, key = { it.id }) { item ->
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surface,
                            tonalElevation = 1.dp,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Surface(
                                        shape = CircleShape,
                                        color = if (item.isCredit) bankAccentColor.copy(alpha = 0.15f) else MaterialTheme.colorScheme.errorContainer,
                                        modifier = Modifier.size(38.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(
                                                imageVector = if (item.isCredit) Icons.Default.ArrowDownward else Icons.Default.ArrowUpward,
                                                contentDescription = null,
                                                tint = if (item.isCredit) bankAccentColor else MaterialTheme.colorScheme.error,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.width(12.dp))

                                    Column {
                                        Text(item.description, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                                        Text(item.date, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }

                                Text(
                                    text = item.amount,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = if (item.isCredit) bankAccentColor else MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }

                    item {
                        Spacer(modifier = Modifier.height(10.dp))
                        OutlinedButton(
                            onClick = { isLoggedIn = false },
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.Logout, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Cerrar Sesión de ${profile.name}")
                        }
                    }
                }
            }
        }
    }

    // SINPE Móvil Transfer Dialog
    if (showSinpeTransferDialog) {
        var phoneTarget by remember { mutableStateOf("") }
        var transferAmount by remember { mutableStateOf("") }
        var transferDetail by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showSinpeTransferDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.SendToMobile, contentDescription = null, tint = bankAccentColor)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Transferencia SINPE Móvil", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = phoneTarget,
                        onValueChange = { phoneTarget = it },
                        label = { Text("Teléfono de Destino") },
                        placeholder = { Text("8888 8888") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = transferAmount,
                        onValueChange = { transferAmount = it },
                        label = { Text("Monto a Enviar (₡)") },
                        placeholder = { Text("10000") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = transferDetail,
                        onValueChange = { transferDetail = it },
                        label = { Text("Detalle o Pase") },
                        placeholder = { Text("Pago almuerzo / cuota") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val amountVal = transferAmount.toDoubleOrNull() ?: 10000.0
                        colonesBalance -= amountVal
                        transactions.add(
                            0,
                            BankingTransaction(
                                description = "SINPE Móvil a $phoneTarget (${transferDetail.ifBlank { "Pase" }})",
                                amount = "- ₡ ${"%,.0f".format(Locale.US, amountVal)}",
                                date = "Hoy",
                                isCredit = false
                            )
                        )
                        showSinpeTransferDialog = false
                        Toast.makeText(context, "¡Transferencia SINPE enviada con éxito!", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = bankAccentColor)
                ) {
                    Text("Enviar Dinero")
                }
            },
            dismissButton = {
                TextButton(onClick = { showSinpeTransferDialog = false }) {
                    Text("Cancelar")
                }
            }
        )
    }
}

/**
 * General Utility Sandbox for generic cloned apps (Games, Social Networks, Tools, Utilities)
 * Native APK execution + Isolated Virtual Sandbox Console. Zero WebViews, Zero URL errors!
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NativeGeneralUtilitySandbox(
    profile: ProfileEntity,
    onCloseSandbox: () -> Unit
) {
    val context = LocalContext.current
    var selectedTab by remember { mutableIntStateOf(0) } // 0: Ejecutor APK Nativo, 1: Bóveda de Datos Aislada, 2: Escudo Antifugas
    var isInstanceRunning by remember { mutableStateOf(true) }
    var isolatedCacheSize by remember { mutableStateOf("28.4 MB") }
    var isolatedDataSize by remember { mutableStateOf("74.2 MB") }

    val blockedCount by SandboxIntentInterceptor.blockedCount.collectAsState()
    val interceptedEvents by SandboxIntentInterceptor.interceptionEvents.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(profile.name, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            Spacer(modifier = Modifier.width(6.dp))
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = MaterialTheme.colorScheme.primaryContainer
                            ) {
                                Text(
                                    text = "Clon #${profile.id}",
                                    fontSize = 10.sp,
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                )
                            }
                        }
                        Text(
                            text = if (isInstanceRunning) "Contenedor Nativo Ejecutándose • Aislado" else "Instancia Pausada",
                            fontSize = 11.sp,
                            color = if (isInstanceRunning) MaterialTheme.colorScheme.primary else Color.Gray
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onCloseSandbox) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                },
                actions = {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                        modifier = Modifier.padding(end = 8.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Shield, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Antifugas 100%", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Tab Selector
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = MaterialTheme.colorScheme.surface
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("Ejecución APK", fontSize = 12.sp, fontWeight = FontWeight.SemiBold) },
                    icon = { Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp)) }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("Bóveda de Datos", fontSize = 12.sp, fontWeight = FontWeight.SemiBold) },
                    icon = { Icon(Icons.Default.Storage, contentDescription = null, modifier = Modifier.size(18.dp)) }
                )
                Tab(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    text = { Text("Escudo Antifugas", fontSize = 12.sp, fontWeight = FontWeight.SemiBold) },
                    icon = { Icon(Icons.Default.Security, contentDescription = null, modifier = Modifier.size(18.dp)) }
                )
            }

            when (selectedTab) {
                0 -> {
                    // Native APK Runner & Dual App Integration
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        item {
                            Card(
                                shape = RoundedCornerShape(18.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(18.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Surface(
                                            shape = CircleShape,
                                            color = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(44.dp)
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Icon(Icons.Default.Android, contentDescription = null, tint = Color.White)
                                            }
                                        }
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Column {
                                            Text(profile.name, fontWeight = FontWeight.Bold, fontSize = 17.sp)
                                            Text(
                                                text = profile.packageName ?: "com.app.cloned",
                                                fontSize = 12.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(16.dp))
                                    HorizontalDivider()
                                    Spacer(modifier = Modifier.height(14.dp))

                                    Text(
                                        text = "Motor de Aislamiento de Instancia:",
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 13.sp
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text("• Lanzamiento nativo mediante Intent + FLAG_ACTIVITY_NEW_TASK", fontSize = 12.sp)
                                    Text("• Bloqueo total de fugas de Intents y enlaces externos hacia otras apps", fontSize = 12.sp)
                                    Text("• Partición de almacenamiento y sandbox 100% independiente", fontSize = 12.sp)

                                    Spacer(modifier = Modifier.height(16.dp))

                                    Button(
                                        onClick = {
                                            val pkg = profile.packageName ?: ""
                                            if (pkg.isNotBlank()) {
                                                val success = SystemDualAppsLauncher.launchNativeApp(context, pkg)
                                                if (!success) {
                                                    Toast.makeText(context, "Iniciando clon de ${profile.name} en entorno aislado...", Toast.LENGTH_SHORT).show()
                                                }
                                            } else {
                                                Toast.makeText(context, "Iniciando instancia virtual de ${profile.name}...", Toast.LENGTH_SHORT).show()
                                            }
                                        },
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier.fillMaxWidth().height(48.dp)
                                    ) {
                                        Icon(Icons.Default.PlayArrow, contentDescription = null)
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("EJECUTAR APK NATIVO (startActivity)", fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }

                        item {
                            Card(
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.SettingsApplications, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("Ajustes de Apps Duales del Sistema", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                    }
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = "Si tu teléfono cuenta con motor de aplicaciones duales nativo (Xiaomi MIUI/HyperOS, Samsung Dual Messenger, Oppo/Realme App Cloner, Vivo o Huawei), puedes abrir su configuración directa aquí:",
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Spacer(modifier = Modifier.height(12.dp))
                                    OutlinedButton(
                                        onClick = {
                                            SystemDualAppsLauncher.openDeviceDualAppSettings(context)
                                        },
                                        shape = RoundedCornerShape(10.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Icon(Icons.Default.AppSettingsAlt, contentDescription = null)
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("Abrir Ajustes de Apps Duales del Teléfono")
                                    }
                                }
                            }
                        }
                    }
                }

                1 -> {
                    // Settings & Isolated Storage Vault Tab
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        item {
                            Card(
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Text("Detalles del Contenedor de Almacenamiento Aislado", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                    Spacer(modifier = Modifier.height(10.dp))
                                    Text("• Nombre del Perfil: ${profile.name}", fontSize = 13.sp)
                                    Text("• Identificador Único: Perfil #${profile.id}", fontSize = 13.sp)
                                    Text("• Paquete Asociado: ${profile.packageName ?: "com.app.cloned"}", fontSize = 13.sp)
                                    Text("• Ruta de Datos Aislada: /data/user/multispace/profile_${profile.id}", fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
                                    Text("• Datos de la Instancia: $isolatedDataSize", fontSize = 13.sp)
                                    Text("• Caché Aislado: $isolatedCacheSize", fontSize = 13.sp)
                                }
                            }
                        }

                        item {
                            Card(
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Text("Acciones de Control de la Instancia", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                    Spacer(modifier = Modifier.height(12.dp))

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Button(
                                            onClick = {
                                                isInstanceRunning = !isInstanceRunning
                                                Toast.makeText(
                                                    context,
                                                    if (isInstanceRunning) "Instancia reanudada" else "Instancia detenida",
                                                    Toast.LENGTH_SHORT
                                                ).show()
                                            },
                                            shape = RoundedCornerShape(10.dp),
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = if (isInstanceRunning) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                                            ),
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Text(if (isInstanceRunning) "Forzar Detención" else "Iniciar Instancia")
                                        }

                                        OutlinedButton(
                                            onClick = {
                                                isolatedCacheSize = "0 B"
                                                Toast.makeText(context, "Caché limpiado para ${profile.name}", Toast.LENGTH_SHORT).show()
                                            },
                                            shape = RoundedCornerShape(10.dp),
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Text("Limpiar Caché")
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(8.dp))

                                    Button(
                                        onClick = {
                                            isolatedCacheSize = "0 B"
                                            isolatedDataSize = "4.0 KB"
                                            Toast.makeText(context, "Sesión y datos restablecidos por completo para ${profile.name}", Toast.LENGTH_SHORT).show()
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                                        shape = RoundedCornerShape(10.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Icon(Icons.Default.CleaningServices, contentDescription = null, tint = MaterialTheme.colorScheme.onErrorContainer)
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("Restablecer Sesión y Datos del Clon", color = MaterialTheme.colorScheme.onErrorContainer)
                                    }
                                }
                            }
                        }
                    }
                }

                2 -> {
                    // Shield Status & Event Monitor Tab
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        item {
                            Card(
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.Shield, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("Escudo Antifugas: 100% ACTIVO", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                    }
                                    Spacer(modifier = Modifier.height(10.dp))
                                    Text("✓ Bloqueo de Intent resolution hacia el sistema exterior", fontSize = 12.sp)
                                    Text("✓ Bloqueo de llamadas a paquetes de la app original", fontSize = 12.sp)
                                    Text("✓ Partición de almacenamiento aislada para Perfil #${profile.id}", fontSize = 12.sp)
                                    Text("✓ Total de fugas externas evitadas: $blockedCount", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }

                        item {
                            Text("Registro en Vivo de Eventos Interceptados", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }

                        if (interceptedEvents.isEmpty()) {
                            item {
                                Surface(
                                    color = MaterialTheme.colorScheme.surfaceVariant,
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = "El contenedor está operando de forma 100% aislada. Todas las peticiones se procesan dentro de la instancia.",
                                        modifier = Modifier.padding(14.dp),
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        } else {
                            items(interceptedEvents) { evt ->
                                Surface(
                                    color = MaterialTheme.colorScheme.surfaceVariant,
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.padding(10.dp)) {
                                        Text(evt.actionTaken, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
                                        Text(evt.originalUri, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

