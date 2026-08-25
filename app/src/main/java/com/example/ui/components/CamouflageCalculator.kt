package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.security.SecurityPreferences

@Composable
fun CamouflageCalculator(
    securityPreferences: SecurityPreferences,
    onUnlockVault: (isDecoy: Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    var displayText by remember { mutableStateOf("0") }
    var expressionText by remember { mutableStateOf("") }
    var operand1 by remember { mutableStateOf<Double?>(null) }
    var pendingOp by remember { mutableStateOf<String?>(null) }
    var resetOnNextDigit by remember { mutableStateOf(false) }

    fun handleDigit(d: String) {
        if (displayText == "0" || resetOnNextDigit) {
            displayText = d
            resetOnNextDigit = false
        } else {
            displayText += d
        }
    }

    fun handleOp(op: String) {
        val currentVal = displayText.toDoubleOrNull() ?: 0.0
        operand1 = currentVal
        pendingOp = op
        expressionText = "$displayText $op"
        resetOnNextDigit = true
    }

    fun handleEqual() {
        val rawInput = displayText.replace(".", "").trim()
        // Check if raw input matches Vault PIN
        if (securityPreferences.verifyMasterPin(rawInput)) {
            onUnlockVault(false)
            return
        }
        if (securityPreferences.verifyDecoyPin(rawInput)) {
            onUnlockVault(true)
            return
        }

        // Otherwise perform normal math calculation
        if (operand1 != null && pendingOp != null) {
            val op2 = displayText.toDoubleOrNull() ?: 0.0
            val result = when (pendingOp) {
                "+" -> operand1!! + op2
                "-" -> operand1!! - op2
                "×", "*" -> operand1!! * op2
                "÷", "/" -> if (op2 != 0.0) operand1!! / op2 else Double.NaN
                else -> op2
            }
            expressionText = "$operand1 $pendingOp $op2 ="
            displayText = if (result.isNaN()) "Error" else if (result % 1.0 == 0.0) result.toLong().toString() else "%.4f".format(result).trimEnd('0').trimEnd('.')
            operand1 = null
            pendingOp = null
            resetOnNextDigit = true
        }
    }

    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(20.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Top Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Calculate,
                        contentDescription = "Calculadora",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Calculadora",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = "Cifrado",
                    tint = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                    modifier = Modifier.size(16.dp)
                )
            }

            // Display Screen
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 24.dp),
                horizontalAlignment = Alignment.End
            ) {
                Text(
                    text = expressionText,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = displayText,
                    style = MaterialTheme.typography.displayMedium.copy(
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold
                    ),
                    color = MaterialTheme.colorScheme.onBackground,
                    maxLines = 1,
                    textAlign = TextAlign.End
                )
            }

            // Keypad
            val buttons = listOf(
                listOf("C", "±", "%", "÷"),
                listOf("7", "8", "9", "×"),
                listOf("4", "5", "6", "-"),
                listOf("1", "2", "3", "+"),
                listOf("0", ".", "AC", "=")
            )

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                buttons.forEach { row ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        row.forEach { label ->
                            val isAction = label in listOf("÷", "×", "-", "+", "=")
                            val isSpecial = label in listOf("C", "AC", "±", "%")

                            val btnColor = when {
                                label == "=" -> MaterialTheme.colorScheme.primary
                                isAction -> MaterialTheme.colorScheme.primaryContainer
                                isSpecial -> MaterialTheme.colorScheme.surfaceVariant
                                else -> MaterialTheme.colorScheme.surface
                            }

                            val textColor = when {
                                label == "=" -> MaterialTheme.colorScheme.onPrimary
                                isAction -> MaterialTheme.colorScheme.onPrimaryContainer
                                isSpecial -> MaterialTheme.colorScheme.onSurfaceVariant
                                else -> MaterialTheme.colorScheme.onSurface
                            }

                            Surface(
                                modifier = Modifier
                                    .weight(1f)
                                    .aspectRatio(1.15f)
                                    .testTag("calc_btn_$label"),
                                shape = RoundedCornerShape(18.dp),
                                color = btnColor,
                                onClick = {
                                    when (label) {
                                        "C", "AC" -> {
                                            displayText = "0"
                                            expressionText = ""
                                            operand1 = null
                                            pendingOp = null
                                        }
                                        "±" -> {
                                            if (displayText != "0" && displayText != "Error") {
                                                displayText = if (displayText.startsWith("-")) displayText.substring(1) else "-$displayText"
                                            }
                                        }
                                        "%" -> {
                                            val v = displayText.toDoubleOrNull() ?: 0.0
                                            displayText = (v / 100.0).toString()
                                        }
                                        "÷", "×", "-", "+" -> handleOp(label)
                                        "=" -> handleEqual()
                                        "." -> {
                                            if (!displayText.contains(".")) {
                                                displayText += "."
                                            }
                                        }
                                        else -> handleDigit(label)
                                    }
                                }
                            ) {
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier.fillMaxSize()
                                ) {
                                    Text(
                                        text = label,
                                        style = MaterialTheme.typography.titleLarge,
                                        fontWeight = FontWeight.SemiBold,
                                        color = textColor
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
