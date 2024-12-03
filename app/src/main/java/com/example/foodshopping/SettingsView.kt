package com.example.foodshopping

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

@Composable
fun SettingsScreenView() {
    val db = FirebaseFirestore.getInstance()
    val auth = FirebaseAuth.getInstance()
    val userId = auth.currentUser?.uid

    var shoppingListNotificationsEnabled by remember { mutableStateOf(false) }
    var shoppingStatusNotificationsEnabled by remember { mutableStateOf(false) }

    // Fetch notification settings from the database
    LaunchedEffect(Unit) {
        userId?.let { uid ->
            db.collection("User").document(uid).get()
                .addOnSuccessListener { document ->
                    shoppingListNotificationsEnabled = document.getBoolean("notification_settings.item_updates") ?: false
                    shoppingStatusNotificationsEnabled = document.getBoolean("notification_settings.shopping_status") ?: false
                }
                .addOnFailureListener { e ->
                    println("Error fetching notification settings: $e")
                }
        }
    }

    // UI
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.linearGradient(
                    colors = listOf(
                        Color(0xFFB3E5FC),
                        Color(0xFFBBDEFB),
                        Color(0xFFFFF176)
                    )
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.Start
        ) {
            Text(
                text = "Settings",
                style = MaterialTheme.typography.headlineLarge,
                color = Color.Black,
                modifier = Modifier.padding(bottom = 24.dp)
            )

            // Toggle for shopping list notifications
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Shopping List Notifications",
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color.Black
                )
                Switch(
                    checked = shoppingListNotificationsEnabled,
                    onCheckedChange = { isChecked ->
                        shoppingListNotificationsEnabled = isChecked
                        updateNotificationSetting(
                            db = db,
                            userId = userId,
                            settingPath = "notification_settings.item_updates",
                            newValue = isChecked
                        )
                    },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color(0xFF4CAF50),
                        uncheckedThumbColor = Color(0xFFBDBDBD)
                    )
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Toggle for shopping status notifications
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Shopping Status Notifications",
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color.Black
                )
                Switch(
                    checked = shoppingStatusNotificationsEnabled,
                    onCheckedChange = { isChecked ->
                        shoppingStatusNotificationsEnabled = isChecked
                        updateNotificationSetting(
                            db = db,
                            userId = userId,
                            settingPath = "notification_settings.shopping_status",
                            newValue = isChecked
                        )
                    },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color(0xFF4CAF50),
                        uncheckedThumbColor = Color(0xFFBDBDBD)
                    )
                )
            }
        }

        // Bottom Navigation
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
        ) {
            BottomNavigationBar(currentScreen = "Settings")
        }
    }
}

// Function to update notification settings in the database
fun updateNotificationSetting(
    db: FirebaseFirestore,
    userId: String?,
    settingPath: String,
    newValue: Boolean
) {
    userId?.let {
        db.collection("User").document(it).update(settingPath, newValue)
            .addOnSuccessListener { Log.d("Settings", "Notification updated for $settingPath") }
            .addOnFailureListener { Log.w("Settings", "Failed to update $settingPath", it) }
    }
}
