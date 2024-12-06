package com.example.foodshopping

import android.content.Context
import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.*
import com.example.foodshopping.ui.theme.FoodShoppingTheme
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry
import com.github.mikephil.charting.utils.ColorTemplate
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.util.*

class AnalyticsActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            FoodShoppingTheme {
                var categoryData by remember { mutableStateOf(mapOf<String, Int>()) }
                var topCategories by remember { mutableStateOf(listOf<Pair<String, Int>>()) }
                var randomProduct by remember { mutableStateOf("") }
                var topCategoryProducts by remember { mutableStateOf(mapOf<String, Map<String, Int>>()) }

                val db = FirebaseFirestore.getInstance()
                val auth = FirebaseAuth.getInstance()
                val userId = auth.currentUser?.uid

                LaunchedEffect(userId) {
                    if (userId != null) {
                        fetchCategoryData(db, userId) { data ->
                            categoryData = data
                            val topCategoriesList = data.toList().sortedByDescending { it.second }.take(3)
                            topCategories = topCategoriesList

                            // Fetch product data for each top category
                            topCategoriesList.forEach { (category, _) ->
                                fetchProductsInCategory(db, userId, category) { products ->
                                    topCategoryProducts = topCategoryProducts + (category to products)
                                }
                            }
                        }

                        fetchRandomProduct(db, userId) { product ->
                            randomProduct = product
                        }
                    }
                }

                AnalyticsScreen(
                    categoryData = categoryData,
                    topCategories = topCategories,
                    topCategoryProducts = topCategoryProducts,
                    randomProduct = randomProduct
                )
            }
        }
    }

    private fun fetchCategoryData(
        db: FirebaseFirestore,
        userId: String,
        onUpdate: (Map<String, Int>) -> Unit
    ) {
        db.collection("History")
            .whereEqualTo("userId", userId)
            .get()
            .addOnSuccessListener { snapshot ->
                val categoryCounts = mutableMapOf<String, Int>()

                snapshot.documents.forEach { document ->
                    val products = document.get("products") as? List<Map<String, Any>> ?: emptyList()
                    for (product in products) {
                        val category = product["category"] as? String ?: "Unknown"
                        categoryCounts[category] = categoryCounts.getOrDefault(category, 0) + 1
                    }
                }

                onUpdate(categoryCounts)
            }
    }

    private fun fetchProductsInCategory(
        db: FirebaseFirestore,
        userId: String,
        category: String,
        onResult: (Map<String, Int>) -> Unit
    ) {
        val calendar = Calendar.getInstance()
        calendar.add(Calendar.MONTH, -1)
        val oneMonthAgo = calendar.time

        db.collection("History")
            .whereEqualTo("userId", userId)
            .get()
            .addOnSuccessListener { snapshot ->
                val productCounts = mutableMapOf<String, Int>()

                snapshot.documents.forEach { document ->
                    val datePurchased = document.getDate("datePurchased")
                    if (datePurchased != null && datePurchased.after(oneMonthAgo)) {
                        val products = document.get("products") as? List<Map<String, Any>> ?: emptyList()
                        for (product in products) {
                            if (product["category"] == category) {
                                val productName = (product["name"] as? String)?.trim()?.lowercase() ?: "Unknown"
                                productCounts[productName] = productCounts.getOrDefault(productName, 0) + 1
                            }
                        }
                    }
                }

                onResult(productCounts)
            }
    }

    private fun fetchRandomProduct(
        db: FirebaseFirestore,
        userId: String,
        onUpdate: (String) -> Unit
    ) {
        db.collection("History")
            .whereEqualTo("userId", userId)
            .get()
            .addOnSuccessListener { snapshot ->
                val allProducts = snapshot.documents.flatMap { document ->
                    val products = document.get("products") as? List<Map<String, Any>> ?: emptyList()
                    products.map { it["name"] as? String ?: "Unknown Product" }
                }
                onUpdate(allProducts.randomOrNull() ?: "No Products")
            }
    }
}
