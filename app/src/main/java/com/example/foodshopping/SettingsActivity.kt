package com.example.foodshopping

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.example.foodshopping.ui.theme.FoodShoppingTheme

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
