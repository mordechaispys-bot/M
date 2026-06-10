package com.example.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Backspace
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.VpnKey
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.viewmodel.UnlockState
import com.example.viewmodel.VaultViewModel

@Composable
fun CalculatorScreen(
    viewModel: VaultViewModel,
    modifier: Modifier = Modifier
) {
    val displayState by viewModel.calculatorDisplay.collectAsState()
    val resultState by viewModel.calculatorResult.collectAsState()
    val unlockState by viewModel.unlockState.collectAsState()
    val feedbackMessage by viewModel.feedbackMessage.collectAsState()

    // Professional Dark Theme Palette
    val darkBg = Color(0xFF12141C)
    val displayBg = Color(0xFF1B1E29)
    val buttonNormal = Color(0xFF232736)
    val buttonOperator = Color(0xFF3843D0) // Modern Accent Color
    val buttonSpecial = Color(0xFFFFB13B)  // Premium Amber
    val textColor = Color(0xFFFFFFFF)

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(darkBg)
            .windowInsetsPadding(WindowInsets.safeDrawing),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // App disguised status/header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = "Safe",
                    tint = buttonSpecial.copy(alpha = 0.8f),
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "מחשבון מדעי",
                    color = textColor.copy(alpha = 0.7f),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.SansSerif
                )
            }

            // A tiny hidden reset in case user forgot during development
            IconButton(
                onClick = { viewModel.resetPasscode() },
                modifier = Modifier.size(24.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.VpnKey,
                    contentDescription = "Reset PIN",
                    tint = textColor.copy(alpha = 0.15f)
                )
            }
        }

        // Subtitle instructions depending on setting up PIN
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
        ) {
            when (unlockState) {
                UnlockState.SETUP_PIN -> {
                    Text(
                        text = "הגדרת קוד סודי: בחר 4-8 ספרות ולחץ על '=' לאישור",
                        color = buttonSpecial,
                        fontSize = 14.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth(),
                        fontWeight = FontWeight.Medium
                    )
                }
                UnlockState.CONFIRM_PIN -> {
                    Text(
                        text = "אשר קוד סודי: הקלד שנית את הקוד ולחץ על '='",
                        color = buttonSpecial,
                        fontSize = 14.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth(),
                        fontWeight = FontWeight.Medium
                    )
                }
                else -> {
                    Text(
                        text = "בצע חישוב רגיל או הקלד את הקוד ולחץ על '=' לפתיחה",
                        color = textColor.copy(alpha = 0.4f),
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }

        // Display screen
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(24.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(displayBg)
                .padding(24.dp),
            contentAlignment = Alignment.BottomEnd
        ) {
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.Bottom,
                modifier = Modifier.fillMaxWidth()
            ) {
                // Interactive message popup inside display
                AnimatedVisibility(
                    visible = feedbackMessage != null,
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    feedbackMessage?.let { msg ->
                        Surface(
                            color = buttonSpecial.copy(alpha = 0.15f),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .padding(bottom = 16.dp)
                                .align(Alignment.CenterHorizontally)
                        ) {
                            Text(
                                text = msg,
                                color = buttonSpecial,
                                fontSize = 13.sp,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }

                // Current mathematical formula entered
                Text(
                    text = displayState.ifEmpty { "0" },
                    color = textColor,
                    fontSize = if (displayState.length > 12) 28.sp else 38.sp,
                    textAlign = TextAlign.End,
                    modifier = Modifier.fillMaxWidth(),
                    lineHeight = 44.sp,
                    fontWeight = FontWeight.Light,
                    fontFamily = FontFamily.Monospace
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Calculated resulting output
                if (resultState.isNotEmpty()) {
                    Text(
                        text = resultState,
                        color = buttonSpecial,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.End,
                        modifier = Modifier.fillMaxWidth(),
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }

        // Button panel layout
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            val buttons = listOf(
                listOf("C", "DEL", "÷"),
                listOf("7", "8", "9", "×"),
                listOf("4", "5", "6", "-"),
                listOf("1", "2", "3", "+"),
                listOf("0", ".", "=")
            )

            buttons.forEach { row ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    row.forEach { char ->
                        val isOp = char == "+" || char == "-" || char == "×" || char == "÷"
                        val isSpecial = char == "C" || char == "DEL" || char == "="

                        val weight = if (char == "=" || char == "C") 2f else 1f
                        val bgColor = when {
                            char == "=" -> buttonSpecial
                            isOp -> buttonOperator
                            isSpecial -> buttonNormal.copy(alpha = 1.2f)
                            else -> buttonNormal
                        }
                        val contentColor = when {
                            char == "=" -> Color.Black
                            isOp -> textColor
                            else -> textColor
                        }

                        Box(
                            modifier = Modifier
                                .weight(weight)
                                .aspectRatio(if (weight > 1f) 1.6f else 1f)
                                .clip(RoundedCornerShape(40))
                                .background(bgColor)
                                .clickable { viewModel.onCalculatorButtonPress(char) },
                            contentAlignment = Alignment.Center
                        ) {
                            if (char == "DEL") {
                                Icon(
                                    imageVector = Icons.Default.Backspace,
                                    contentDescription = "Delete",
                                    tint = contentColor,
                                    modifier = Modifier.size(24.dp)
                                )
                            } else {
                                Text(
                                    text = char,
                                    color = contentColor,
                                    fontSize = 24.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
