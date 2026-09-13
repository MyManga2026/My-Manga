package com.example

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.example.ui.navigation.AppNavigation
import com.example.ui.theme.MangaPillTheme
import com.example.utils.AutoBackupScheduler
import com.example.utils.NotificationHelper
import com.example.utils.NotificationScheduler
import kotlinx.coroutines.flow.MutableStateFlow

class MainActivity : ComponentActivity() {
    private val intentState = MutableStateFlow<Intent?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        NotificationHelper.createNotificationChannel(this)
        NotificationScheduler.schedulePeriodicNotifications(this)
        AutoBackupScheduler.checkAndSchedule(this)
        enableEdgeToEdge()
        intentState.value = intent
        setContent {
            val currentIntent by intentState.collectAsState()
            MangaPillTheme {
                AppNavigation(
                    intent = currentIntent,
                    onIntentHandled = { intentState.value = null }
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        intentState.value = intent
    }
}


