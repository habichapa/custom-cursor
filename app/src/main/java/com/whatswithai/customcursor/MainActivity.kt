package com.whatswithai.customcursor

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat

class MainActivity : ComponentActivity() {

    private var isServiceRunning by mutableStateOf(false)

    private val overlayPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        // Re-check after user returns from settings
        checkAndStartService()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        isServiceRunning = CursorService.isRunning

        setContent {
            MaterialTheme(
                colorScheme = darkColorScheme(
                    background = Color(0xFF0A0A0F),
                    surface = Color(0xFF141420),
                    primary = Color(0xFF7C5CFC),
                    onBackground = Color.White,
                )
            ) {
                CursorAppUI(
                    isRunning = isServiceRunning,
                    onToggle = { toggleService() }
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        isServiceRunning = CursorService.isRunning
    }

    private fun toggleService() {
        if (isServiceRunning) {
            stopCursorService()
        } else {
            checkAndStartService()
        }
    }

    private fun checkAndStartService() {
        if (!Settings.canDrawOverlays(this)) {
            val intent = Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:$packageName")
            )
            overlayPermissionLauncher.launch(intent)
            return
        }
        startCursorService()
    }

    private fun startCursorService() {
        val intent = Intent(this, CursorService::class.java).apply {
            action = CursorService.ACTION_START
        }
        ContextCompat.startForegroundService(this, intent)
        isServiceRunning = true
    }

    private fun stopCursorService() {
        val intent = Intent(this, CursorService::class.java).apply {
            action = CursorService.ACTION_STOP
        }
        startService(intent)
        isServiceRunning = false
    }
}

@Composable
fun CursorAppUI(isRunning: Boolean, onToggle: () -> Unit) {
    val bg = Color(0xFF0A0A0F)
    val card = Color(0xFF141420)
    val accent = Color(0xFF7C5CFC)
    val accentGlow = Color(0xFF9D7FFF)
    val green = Color(0xFF22C55E)
    val red = Color(0xFFEF4444)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(bg),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp),
            modifier = Modifier.padding(32.dp)
        ) {
            // Title
            Text(
                "Custom Cursor",
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
            )
            Text(
                "OTG / scrcpy mouse overlay",
                fontSize = 14.sp,
                color = Color(0xFF8888AA),
            )

            Spacer(Modifier.height(8.dp))

            // Status card
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(card)
                    .border(
                        1.dp,
                        if (isRunning) green.copy(alpha = 0.4f) else red.copy(alpha = 0.2f),
                        RoundedCornerShape(16.dp)
                    )
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Status dot
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(RoundedCornerShape(50))
                            .background(if (isRunning) green else red)
                    )
                    Text(
                        if (isRunning) "Cursor Overlay ACTIVE" else "Cursor Overlay OFF",
                        color = if (isRunning) green else red,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 16.sp
                    )
                }
            }

            // Toggle button
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(if (isRunning) red.copy(alpha = 0.15f) else accent)
                    .border(
                        1.dp,
                        if (isRunning) red.copy(alpha = 0.5f) else accentGlow.copy(alpha = 0.6f),
                        RoundedCornerShape(14.dp)
                    )
                    .clickable { onToggle() }
                    .padding(vertical = 18.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    if (isRunning) "Stop Cursor" else "Start Cursor",
                    color = if (isRunning) red else Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            }

            Spacer(Modifier.height(8.dp))

            // Info card
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(card)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("How it works", color = accentGlow, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                InfoRow("Connect a mouse via OTG cable or use scrcpy")
                InfoRow("The custom cursor replaces the system pointer")
                InfoRow("Tap ▶ Start, then use your mouse anywhere")
                InfoRow("Grant 'Draw over apps' permission when asked")
            }
        }
    }
}

@Composable
fun InfoRow(text: String) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.Top
    ) {
        Text("•", color = Color(0xFF7C5CFC), fontSize = 13.sp)
        Text(text, color = Color(0xFF9999BB), fontSize = 13.sp)
    }
}
