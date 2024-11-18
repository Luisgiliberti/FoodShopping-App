package com.example.foodshopping

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.example.foodshopping.ui.theme.FoodShoppingTheme

class AnalyticsActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            FoodShoppingTheme {
                AnalyticsScreen()
            }
        }
    }
}
