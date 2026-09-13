package com.example.ui.screens.settings

import android.content.Context
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.utils.NotificationHelper
import com.example.utils.UserPreferences

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OwnerControlPanelScreen(
    onBackClick: () -> Unit
) {
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current

    // State 1: App Version Update
    var updateAvailableEnabled by remember {
        mutableStateOf(UserPreferences.isUpdateAvailable(context))
    }
    var updateVersionName by remember {
        mutableStateOf(UserPreferences.getUpdateVersionName(context))
    }
    var updateFeatures by remember {
        mutableStateOf(UserPreferences.getUpdateFeatures(context))
    }
    var updateUrl by remember {
        mutableStateOf(UserPreferences.getUpdateUrl(context))
    }
    var showPreviewUpdateDialog by remember { mutableStateOf(false) }

    // State 2: App Maintenance Mode
    var maintenanceEnabled by remember {
        mutableStateOf(UserPreferences.isMaintenanceModeEnabled(context))
    }
    var maintenanceMessage by remember {
        mutableStateOf(UserPreferences.getMaintenanceMessage(context))
    }
    var maintenanceEstimate by remember {
        mutableStateOf(UserPreferences.getMaintenanceEstimate(context))
    }
    var showPreviewMaintenanceDialog by remember { mutableStateOf(false) }

    // State 3: Password Change
    var oldPasswordInput by remember { mutableStateOf("") }
    var newPasswordInput by remember { mutableStateOf("") }
    var confirmPasswordInput by remember { mutableStateOf("") }
    var oldPasswordVisible by remember { mutableStateOf(false) }
    var newPasswordVisible by remember { mutableStateOf(false) }
    var confirmPasswordVisible by remember { mutableStateOf(false) }
    var passwordErrorMessage by remember { mutableStateOf<String?>(null) }
    var passwordSuccessMessage by remember { mutableStateOf<String?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Owner Control Panel",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(text = "👑", fontSize = 18.sp)
                        }
                        Text(
                            text = "Joydeep • Lead Developer",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBackClick,
                        modifier = Modifier.testTag("owner_panel_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Exit Owner Panel"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer,
                    titleContentColor = MaterialTheme.colorScheme.onSurface,
                    navigationIconContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Header Identity Card
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.linearGradient(
                                    listOf(
                                        MaterialTheme.colorScheme.primary,
                                        Color(0xFFE53935)
                                    )
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.AdminPanelSettings,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(26.dp)
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Full-Screen Administrative Hub",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Authenticated as Joydeep (@_joydeep11). Manage broadcasts, server maintenance, and system credentials.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // ==========================================
            // 1ST FEATURE: NEW VERSION AVAILABLE & VERSION FEATURES
            // ==========================================
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("feature_version_update_card")
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.SystemUpdate,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "1. App Version Update Broadcast",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                    // Switch to show update
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Show 'New Version Available' to Users",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = if (updateAvailableEnabled) "Active: Users will see an update alert upon opening the app" else "Inactive: No update prompt shown",
                                style = MaterialTheme.typography.bodySmall,
                                color = if (updateAvailableEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = updateAvailableEnabled,
                            onCheckedChange = { checked ->
                                updateAvailableEnabled = checked
                                UserPreferences.setUpdateAvailable(context, checked)
                                val msg = if (checked) "Update broadcast enabled for users!" else "Update broadcast disabled"
                                Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.testTag("switch_version_update")
                        )
                    }

                    // Version Tag / Name
                    OutlinedTextField(
                        value = updateVersionName,
                        onValueChange = { updateVersionName = it },
                        label = { Text("New Version Tag / Number") },
                        placeholder = { Text("e.g. v2.5.0 or v3.0.0") },
                        singleLine = true,
                        leadingIcon = {
                            Icon(Icons.Default.Numbers, contentDescription = null)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_update_version_name")
                    )

                    // Version Features & Release Notes
                    OutlinedTextField(
                        value = updateFeatures,
                        onValueChange = { updateFeatures = it },
                        label = { Text("New Version Features & Release Notes") },
                        placeholder = { Text("• Add bullet points or highlights of new features...") },
                        minLines = 3,
                        maxLines = 6,
                        leadingIcon = {
                            Icon(Icons.Default.FormatListBulleted, contentDescription = null)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_update_features")
                    )

                    // Download / Update Link
                    OutlinedTextField(
                        value = updateUrl,
                        onValueChange = { updateUrl = it },
                        label = { Text("Update Download Link / URL") },
                        placeholder = { Text(UserPreferences.DEFAULT_UPDATE_URL) },
                        singleLine = true,
                        leadingIcon = {
                            Icon(Icons.Default.Link, contentDescription = null)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_update_url")
                    )

                    // Actions Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                focusManager.clearFocus()
                                UserPreferences.setUpdateAvailable(context, updateAvailableEnabled)
                                UserPreferences.setUpdateVersionName(context, updateVersionName.trim())
                                UserPreferences.setUpdateFeatures(context, updateFeatures.trim())
                                UserPreferences.setUpdateUrl(context, updateUrl.trim())
                                Toast.makeText(context, "Version update details saved successfully! 🚀", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("btn_save_update_settings")
                        ) {
                            Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Save Update")
                        }

                        OutlinedButton(
                            onClick = { showPreviewUpdateDialog = true },
                            modifier = Modifier.testTag("btn_preview_update_dialog")
                        ) {
                            Icon(Icons.Default.Visibility, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Preview")
                        }
                    }

                    // Push alert to trigger notification right away
                    OutlinedButton(
                        onClick = {
                            NotificationHelper.sendNotification(
                                context = context,
                                title = "🚀 New Version $updateVersionName is Available!",
                                message = "Check out the newest features and improvements in MyManga! Tap to explore.",
                                bypassQuietHours = true
                            )
                            Toast.makeText(context, "Push notification sent to user device! 🔔", Toast.LENGTH_SHORT).show()
                        },
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("btn_broadcast_update_push")
                    ) {
                        Icon(Icons.Default.NotificationsActive, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Send Update Notification to Users")
                    }
                }
            }

            // ==========================================
            // 2ND FEATURE: APP IN MAINTENANCE MODE & REMIND ME NOTIFICATION
            // ==========================================
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("feature_maintenance_mode_card")
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Engineering,
                            contentDescription = null,
                            tint = Color(0xFFF57C00)
                        )
                        Text(
                            text = "2. App Maintenance Mode",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                    // Maintenance Switch
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Activate Maintenance Mode",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = if (maintenanceEnabled) "⚠️ Active: Users will see the full-screen maintenance screen when opening the app" else "Normal: App is open and fully operational",
                                style = MaterialTheme.typography.bodySmall,
                                color = if (maintenanceEnabled) Color(0xFFF57C00) else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = maintenanceEnabled,
                            onCheckedChange = { checked ->
                                maintenanceEnabled = checked
                                UserPreferences.setMaintenanceModeEnabled(context, checked)
                                if (!checked) {
                                    // If owner turned off maintenance mode, notify users who requested reminder!
                                    if (UserPreferences.isMaintenanceReminderRequested(context)) {
                                        NotificationHelper.sendNotification(
                                            context = context,
                                            title = "MyManga is Back Online! 🎉",
                                            message = "Maintenance is complete! All manga sources and features are now fully working.",
                                            bypassQuietHours = true
                                        )
                                        UserPreferences.setMaintenanceReminderRequested(context, false)
                                        Toast.makeText(context, "Maintenance ended: Waiting users notified! 🔔", Toast.LENGTH_LONG).show()
                                    } else {
                                        Toast.makeText(context, "Maintenance mode deactivated.", Toast.LENGTH_SHORT).show()
                                    }
                                } else {
                                    Toast.makeText(context, "Maintenance mode activated!", Toast.LENGTH_SHORT).show()
                                }
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = Color(0xFFF57C00)
                            ),
                            modifier = Modifier.testTag("switch_maintenance_mode")
                        )
                    }

                    // Maintenance Message Text Field
                    OutlinedTextField(
                        value = maintenanceMessage,
                        onValueChange = { maintenanceMessage = it },
                        label = { Text("Maintenance Reason / Message") },
                        placeholder = { Text("We are currently performing scheduled maintenance...") },
                        minLines = 2,
                        maxLines = 4,
                        leadingIcon = {
                            Icon(Icons.Default.Message, contentDescription = null)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_maintenance_message")
                    )

                    // Estimated Completion
                    OutlinedTextField(
                        value = maintenanceEstimate,
                        onValueChange = { maintenanceEstimate = it },
                        label = { Text("Estimated Completion / Time") },
                        placeholder = { Text("e.g. Within 2 hours, or Today 8:00 PM") },
                        singleLine = true,
                        leadingIcon = {
                            Icon(Icons.Default.Schedule, contentDescription = null)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_maintenance_estimate")
                    )

                    // Reminder status info
                    val reminderRequested = UserPreferences.isMaintenanceReminderRequested(context)
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                imageVector = if (reminderRequested) Icons.Default.NotificationsActive else Icons.Default.Notifications,
                                contentDescription = null,
                                tint = if (reminderRequested) Color(0xFF2E7D32) else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Column {
                                Text(
                                    text = "User Reminder Subscriptions",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = if (reminderRequested) "Users have clicked 'Remind me when working'. They will receive a notification immediately when you turn off maintenance mode." else "Waiting for users to request reminders on the maintenance screen.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    // Actions Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                focusManager.clearFocus()
                                UserPreferences.setMaintenanceModeEnabled(context, maintenanceEnabled)
                                UserPreferences.setMaintenanceMessage(context, maintenanceMessage.trim())
                                UserPreferences.setMaintenanceEstimate(context, maintenanceEstimate.trim())
                                Toast.makeText(context, "Maintenance settings saved! 🛠️", Toast.LENGTH_SHORT).show()
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFFF57C00),
                                contentColor = Color.White
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("btn_save_maintenance_settings")
                        ) {
                            Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Save Settings")
                        }

                        OutlinedButton(
                            onClick = { showPreviewMaintenanceDialog = true },
                            modifier = Modifier.testTag("btn_preview_maintenance_dialog")
                        ) {
                            Icon(Icons.Default.Visibility, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Preview")
                        }
                    }

                    // Manual Notify button
                    OutlinedButton(
                        onClick = {
                            NotificationHelper.sendNotification(
                                context = context,
                                title = "MyManga is Back Online! 🎉",
                                message = "Maintenance is complete! All manga chapters and services are working normally.",
                                bypassQuietHours = true
                            )
                            UserPreferences.setMaintenanceReminderRequested(context, false)
                            Toast.makeText(context, "Broadcast alert dispatched to users! 🔔", Toast.LENGTH_SHORT).show()
                        },
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("btn_notify_maintenance_resolved")
                    ) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(18.dp), tint = Color(0xFF2E7D32))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Notify Users 'Maintenance Over' Now")
                    }
                }
            }

            // ==========================================
            // 3RD FEATURE: CHANGE OWNER PASSWORD
            // ==========================================
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("feature_password_change_card")
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "3. Change Control Panel Password",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                    Text(
                        text = "To change the owner password, enter your current old password followed by the new password twice.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    // Old Password
                    OutlinedTextField(
                        value = oldPasswordInput,
                        onValueChange = {
                            oldPasswordInput = it
                            passwordErrorMessage = null
                            passwordSuccessMessage = null
                        },
                        label = { Text("Current (Old) Password") },
                        placeholder = { Text("Enter old password...") },
                        singleLine = true,
                        visualTransformation = if (oldPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        leadingIcon = {
                            Icon(Icons.Default.Key, contentDescription = null)
                        },
                        trailingIcon = {
                            IconButton(onClick = { oldPasswordVisible = !oldPasswordVisible }) {
                                Icon(
                                    imageVector = if (oldPasswordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                    contentDescription = if (oldPasswordVisible) "Hide password" else "Show password"
                                )
                            }
                        },
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Password,
                            imeAction = ImeAction.Next
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_old_password")
                    )

                    // New Password
                    OutlinedTextField(
                        value = newPasswordInput,
                        onValueChange = {
                            newPasswordInput = it
                            passwordErrorMessage = null
                            passwordSuccessMessage = null
                        },
                        label = { Text("New Password") },
                        placeholder = { Text("Enter new secret password...") },
                        singleLine = true,
                        visualTransformation = if (newPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        leadingIcon = {
                            Icon(Icons.Default.LockReset, contentDescription = null)
                        },
                        trailingIcon = {
                            IconButton(onClick = { newPasswordVisible = !newPasswordVisible }) {
                                Icon(
                                    imageVector = if (newPasswordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                    contentDescription = if (newPasswordVisible) "Hide password" else "Show password"
                                )
                            }
                        },
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Password,
                            imeAction = ImeAction.Next
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_new_password")
                    )

                    // Confirm New Password
                    OutlinedTextField(
                        value = confirmPasswordInput,
                        onValueChange = {
                            confirmPasswordInput = it
                            passwordErrorMessage = null
                            passwordSuccessMessage = null
                        },
                        label = { Text("Confirm New Password") },
                        placeholder = { Text("Re-enter new secret password...") },
                        singleLine = true,
                        visualTransformation = if (confirmPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        leadingIcon = {
                            Icon(Icons.Default.Password, contentDescription = null)
                        },
                        trailingIcon = {
                            IconButton(onClick = { confirmPasswordVisible = !confirmPasswordVisible }) {
                                Icon(
                                    imageVector = if (confirmPasswordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                    contentDescription = if (confirmPasswordVisible) "Hide password" else "Show password"
                                )
                            }
                        },
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Password,
                            imeAction = ImeAction.Done
                        ),
                        keyboardActions = KeyboardActions(
                            onDone = {
                                focusManager.clearFocus()
                            }
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_confirm_new_password")
                    )

                    // Error or Success Banner
                    AnimatedVisibility(visible = passwordErrorMessage != null) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.errorContainer,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    Icons.Default.ErrorOutline,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.error
                                )
                                Text(
                                    text = passwordErrorMessage ?: "",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onErrorContainer,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }

                    AnimatedVisibility(visible = passwordSuccessMessage != null) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFFE8F5E9),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = Color(0xFF2E7D32)
                                )
                                Text(
                                    text = passwordSuccessMessage ?: "",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color(0xFF1B5E20),
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    // Update Password Button
                    Button(
                        onClick = {
                            focusManager.clearFocus()
                            val currentOwnerPass = UserPreferences.getOwnerPassword(context)

                            if (oldPasswordInput != currentOwnerPass) {
                                passwordErrorMessage = "Current (old) password is incorrect!"
                                passwordSuccessMessage = null
                                return@Button
                            }

                            if (newPasswordInput.length < 6) {
                                passwordErrorMessage = "New password must be at least 6 characters!"
                                passwordSuccessMessage = null
                                return@Button
                            }

                            if (newPasswordInput != confirmPasswordInput) {
                                passwordErrorMessage = "New password and confirmation do not match!"
                                passwordSuccessMessage = null
                                return@Button
                            }

                            // Update password
                            UserPreferences.setOwnerPassword(context, newPasswordInput)
                            passwordErrorMessage = null
                            passwordSuccessMessage = "Owner password changed successfully! 🔒"
                            oldPasswordInput = ""
                            newPasswordInput = ""
                            confirmPasswordInput = ""
                            Toast.makeText(context, "Password updated successfully!", Toast.LENGTH_SHORT).show()
                        },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("btn_change_owner_password")
                    ) {
                        Icon(Icons.Default.Security, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Update Owner Password", fontWeight = FontWeight.Bold)
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }

    // ==========================================
    // PREVIEW DIALOG 1: How Users See Update Dialog
    // ==========================================
    if (showPreviewUpdateDialog) {
        AlertDialog(
            onDismissRequest = { showPreviewUpdateDialog = false },
            icon = {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.size(54.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.RocketLaunch,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }
            },
            title = {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Update Available! 🚀",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                    ) {
                        Text(
                            text = updateVersionName.ifBlank { "v2.5.0" },
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                    }
                }
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "What's New in this Version:",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.surfaceContainerLow,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = updateFeatures.ifBlank { "• Performance optimizations and bug fixes" },
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                    Text(
                        text = "(Preview Mode: This is how your users will see the update popup)",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = { showPreviewUpdateDialog = false },
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Update Now")
                }
            },
            dismissButton = {
                TextButton(onClick = { showPreviewUpdateDialog = false }) {
                    Text("Later")
                }
            }
        )
    }

    // ==========================================
    // PREVIEW DIALOG 2: How Users See Maintenance Screen
    // ==========================================
    if (showPreviewMaintenanceDialog) {
        AlertDialog(
            onDismissRequest = { showPreviewMaintenanceDialog = false },
            icon = {
                Surface(
                    shape = CircleShape,
                    color = Color(0xFFFFF3E0),
                    modifier = Modifier.size(54.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Engineering,
                            contentDescription = null,
                            tint = Color(0xFFF57C00),
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }
            },
            title = {
                Text(
                    text = "App Under Maintenance 🛠️",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = maintenanceMessage.ifBlank { "We're currently performing scheduled maintenance to improve your experience." },
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center
                    )
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFFFFF3E0)
                    ) {
                        Text(
                            text = maintenanceEstimate.ifBlank { "Estimated: Soon" },
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFE65100),
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                    Button(
                        onClick = {
                            Toast.makeText(context, "User clicked 'Remind Me'! Notification queued.", Toast.LENGTH_SHORT).show()
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFF57C00),
                            contentColor = Color.White
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.NotificationsActive, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Remind me when the app is working")
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showPreviewMaintenanceDialog = false }) {
                    Text("Close Preview")
                }
            }
        )
    }
}
