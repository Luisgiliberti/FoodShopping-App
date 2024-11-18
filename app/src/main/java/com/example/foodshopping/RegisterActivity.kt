package com.example.foodshopping

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.example.foodshopping.ui.theme.FoodShoppingTheme
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class RegisterActivity : ComponentActivity() {
    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        setContent {
            FoodShoppingTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    var errorMessage by remember { mutableStateOf<String?>(null) }
                    RegisterScreenView(
                        onRegister = { username, email, password ->
                            registerUser(username, email, password) { error ->
                                errorMessage = error
                            }
                        },
                        onNavigateToLogin = {
                            startActivity(Intent(this, LoginActivity::class.java))
                        },
                        errorMessage = errorMessage
                    )
                }
            }
        }
    }

    private fun registerUser(username: String, email: String, password: String, onError: (String?) -> Unit) {
        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    auth.currentUser?.uid?.let { userId ->
                        saveUserToDatabase(userId, username, email, onError)
                    }
                } else {
                    onError("Registration failed: ${task.exception?.message}")
                }
            }
    }

    private fun saveUserToDatabase(userId: String, username: String, email: String, onError: (String?) -> Unit) {
        val user = hashMapOf(
            "username" to username,
            "email" to email,
            "favorites" to listOf<String>(),
            "notification_settings" to mapOf("item_updates" to true, "reminders" to true),
            "recently_used_items" to listOf<String>()
        )

        db.collection("User").document(userId).set(user)
            .addOnSuccessListener {
                Toast.makeText(this, "Registration Successful", Toast.LENGTH_SHORT).show()
                val intent = Intent(this, LoginActivity::class.java)
                startActivity(intent)
                finish()
            }
            .addOnFailureListener { e ->
                onError("Failed to save user data: ${e.message}")
            }
    }
}
