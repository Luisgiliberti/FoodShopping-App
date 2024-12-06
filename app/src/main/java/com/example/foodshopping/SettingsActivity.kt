package com.example.foodshopping

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.example.foodshopping.ui.theme.FoodShoppingTheme
import com.google.firebase.firestore.FirebaseFirestore

class SettingsActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            FoodShoppingTheme {
                Surface(
                    modifier = Modifier.fillMaxSize()
                ) {
                    SettingsScreenView()
                }
            }
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