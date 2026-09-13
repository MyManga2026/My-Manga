package com.example.ui.screens.maintenance

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.utils.UserPreferences

@Composable
fun MaintenanceScreen(
    onNavigateToOwnerPanel: () -> Unit
) {
    val context = LocalContext.current
    val maintenanceMessage = remember { UserPreferences.getMaintenanceMessage(context) }
    val maintenanceEstimate = remember { UserPreferences.getMaintenanceEstimate(context) }
    var reminderRequested by remember {
        mutableStateOf(UserPreferences.isMaintenanceReminderRequested(context))
    }

    // Secret Admin Bypass states on Maintenance screen
    var wrenchTapCount by remember { mutableIntStateOf(0) }
    var lastTapTime by remember { mutableLongStateOf(0L) }
    var showAdminPasswordDialog by remember { mutableStateOf(false) }
    var adminPasswordInput by remember { mutableStateOf("") }
    var adminPasswordVisible by remember { mutableStateOf(false) }
    var adminPasswordError by remember { mutableStateOf(false) }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Spacer(modifier = Modifier.height(20.dp))

                // Wrench Icon with 5-tap hidden admin bypass
                Box(
                    modifier = Modifier
                        .size(100.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.linearGradient(
                                listOf(
                                    Color(0xFFFFA726),
                                    Color(0xFFF57C00)
                                )
                            )
                        )
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) {
                            val now = System.currentTimeMillis()
                            if (now - lastTapTime < 2000L) {
                                wrenchTapCount++
                            } else {
                                wrenchTapCount = 1
                            }
                            lastTapTime = now

                            if (wrenchTapCount >= 5) {
                                wrenchTapCount = 0
                                adminPasswordInput = ""
                                adminPasswordError = false
                                adminPasswordVisible = false
                                showAdminPasswordDialog = true
                            }
                        }
                        .testTag("maintenance_wrench_icon"),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Engineering,
                        contentDescription = "Maintenance Icon",
                        tint = Color.White,
                        modifier = Modifier.size(54.dp)
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                Text(
                    text = "App Under Maintenance 🛠️",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onBackground
                )

                Spacer(modifier = Modifier.height(10.dp))

                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = Color(0xFFFFF3E0),
                    modifier = Modifier.padding(horizontal = 8.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Schedule,
                            contentDescription = null,
                            tint = Color(0xFFE65100),
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = maintenanceEstimate.ifBlank { "Estimated time: 1 - 2 hours" },
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFE65100)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Custom message card
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = maintenanceMessage.ifBlank {
                            "We are currently upgrading our systems and manga catalog sources to serve you better. We'll be back online as soon as possible!"
                        },
                        style = MaterialTheme.typography.bodyLarge,
                        textAlign = TextAlign.Center,
                        lineHeight = 24.sp,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(20.dp)
                    )
                }

                Spacer(modifier = Modifier.height(28.dp))

                // Remind Me Button
                Button(
                    onClick = {
                        reminderRequested = true
                        UserPreferences.setMaintenanceReminderRequested(context, true)
                        Toast.makeText(
                            context,
                            "🔔 Reminder set! You will receive a notification as soon as MyManga is back online.",
                            Toast.LENGTH_LONG
                        ).show()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (reminderRequested) Color(0xFF2E7D32) else Color(0xFFF57C00),
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp)
                        .testTag("btn_remind_me_maintenance")
                ) {
                    Icon(
                        imageVector = if (reminderRequested) Icons.Default.CheckCircle else Icons.Default.NotificationsActive,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = if (reminderRequested) "✓ Reminder Active — You'll be notified!" else "Remind me when the app is working",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                }

                if (reminderRequested) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "We will send an Android push alert the moment the server maintenance is complete.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF2E7D32),
                        textAlign = TextAlign.Center
                    )
                }

                Spacer(modifier = Modifier.height(30.dp))

                // Discrete Owner Entrance for Joydeep
                TextButton(
                    onClick = {
                        adminPasswordInput = ""
                        adminPasswordError = false
                        adminPasswordVisible = false
                        showAdminPasswordDialog = true
                    },
                    modifier = Modifier.testTag("maintenance_owner_bypass_button")
                ) {
                    Icon(Icons.Default.Lock, contentDescription = null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.outline)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "App Owner / Developer Login",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
            }

            // Owner Verification Dialog on Maintenance Screen
            if (showAdminPasswordDialog) {
                AlertDialog(
                    onDismissRequest = {
                        showAdminPasswordDialog = false
                        adminPasswordInput = ""
                        adminPasswordError = false
                    },
                    icon = {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier.size(52.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Lock,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                        }
                    },
                    title = {
                        Text(
                            text = "Owner Verification",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                    },
                    text = {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text(
                                text = "Enter Joydeep's secret owner password to bypass maintenance mode and enter the control panel.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            OutlinedTextField(
                                value = adminPasswordInput,
                                onValueChange = {
                                    adminPasswordInput = it
                                    if (adminPasswordError) adminPasswordError = false
                                },
                                label = { Text("Owner Password") },
                                placeholder = { Text("Enter owner password...") },
                                singleLine = true,
                                isError = adminPasswordError,
                                visualTransformation = if (adminPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                                trailingIcon = {
                                    IconButton(onClick = { adminPasswordVisible = !adminPasswordVisible }) {
                                        Icon(
                                            imageVector = if (adminPasswordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                            contentDescription = if (adminPasswordVisible) "Hide password" else "Show password"
                                        )
                                    }
                                },
                                keyboardOptions = KeyboardOptions(
                                    keyboardType = KeyboardType.Password,
                                    imeAction = ImeAction.Done
                                ),
                                keyboardActions = KeyboardActions(
                                    onDone = {
                                        val currentPass = UserPreferences.getOwnerPassword(context)
                                        if (adminPasswordInput == currentPass) {
                                            showAdminPasswordDialog = false
                                            adminPasswordInput = ""
                                            adminPasswordError = false
                                            onNavigateToOwnerPanel()
                                        } else {
                                            adminPasswordError = true
                                        }
                                    }
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("maintenance_admin_password_input")
                            )

                            if (adminPasswordError) {
                                Text(
                                    text = "Incorrect password. Access denied.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.error,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                val currentPass = UserPreferences.getOwnerPassword(context)
                                if (adminPasswordInput == currentPass) {
                                    showAdminPasswordDialog = false
                                    adminPasswordInput = ""
                                    adminPasswordError = false
                                    onNavigateToOwnerPanel()
                                } else {
                                    adminPasswordError = true
                                }
                            },
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.testTag("maintenance_admin_unlock_button")
                        ) {
                            Text("Unlock")
                        }
                    },
                    dismissButton = {
                        TextButton(
                            onClick = {
                                showAdminPasswordDialog = false
                                adminPasswordInput = ""
                                adminPasswordError = false
                            }
                        ) {
                            Text("Cancel")
                        }
                    }
                )
            }
        }
    }
}
