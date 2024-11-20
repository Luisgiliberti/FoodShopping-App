package com.example.foodshopping

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.*
import com.example.foodshopping.ui.theme.FoodShoppingTheme
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.util.*

class AnalyticsActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            FoodShoppingTheme {
                var categoryData by remember { mutableStateOf(mapOf<String, Int>()) }

                val db = FirebaseFirestore.getInstance()
                val auth = FirebaseAuth.getInstance()
                val userId = auth.currentUser?.uid

                LaunchedEffect(userId) {
                    if (userId != null) {
                        fetchCategoryData(db, userId) { data ->
                            categoryData = data
                        }
                    }
                }

                AnalyticsScreen(categoryData)
            }
        }
    }

    private fun fetchCategoryData(
        db: FirebaseFirestore,
        userId: String,
        onUpdate: (Map<String, Int>) -> Unit
    ) {
        val calendar = Calendar.getInstance()
        calendar.add(Calendar.MONTH, -1)
        val oneMonthAgo = calendar.time

        db.collection("History")
            .whereEqualTo("userId", userId)
            .get()
            .addOnSuccessListener { snapshot ->
                val categoryCounts = mutableMapOf<String, Int>()

                snapshot.documents.forEach { document ->
                    val datePurchased = document.getDate("datePurchased")
                    if (datePurchased != null && datePurchased.after(oneMonthAgo)) {
                        val products = document.get("products") as? List<Map<String, Any>> ?: emptyList()
                        for (product in products) {
                            val category = product["category"] as? String ?: "Unknown"
                            categoryCounts[category] = categoryCounts.getOrDefault(category, 0) + 1
                        }
                    }
                }

                onUpdate(categoryCounts)
            }
    }
}
