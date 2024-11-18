package com.example.foodshopping

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.example.foodshopping.ui.theme.FoodShoppingTheme

class MenuActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            FoodShoppingTheme {
                MenuScreenView(
                    onAccountClick = {
                        navigateTo(AccountActivity::class.java)
                    },
                    onPurchaseHistoryClick = {
                        navigateTo(PurchaseHistoryActivity::class.java)
                    },
                    onAnalyticsClick = {
                        navigateTo(AnalyticsActivity::class.java)
                    }
                )
            }
        }
    }

    private fun navigateTo(activityClass: Class<*>) {
        startActivity(Intent(this, activityClass))
        overridePendingTransition(0, 0)
    }
}
