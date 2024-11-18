package com.example.foodshopping

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.example.foodshopping.ui.theme.FoodShoppingTheme
import com.google.firebase.auth.FirebaseAuth

class MainActivity : ComponentActivity() {
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        auth = FirebaseAuth.getInstance()

        if (auth.currentUser != null) {
            navigateToShoppingList()
        } else {
            setContent {
                FoodShoppingTheme {
                    HomePageView(
                        onLoginClick = { navigateToLogin() },
                        onRegisterClick = { navigateToRegister() }
                    )
                }
            }
        }
    }

    private fun navigateToShoppingList() {
        val intent = Intent(this, ShoppingListActivity::class.java)
        startActivity(intent)
        finish()
    }

    private fun navigateToLogin() {
        startActivity(Intent(this, LoginActivity::class.java))
    }

    private fun navigateToRegister() {
        startActivity(Intent(this, RegisterActivity::class.java))
    }
}
