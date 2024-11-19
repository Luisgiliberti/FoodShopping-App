package com.example.foodshopping

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.*
import com.example.foodshopping.ui.theme.FoodShoppingTheme
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import timber.log.Timber

class PurchaseHistoryActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            FoodShoppingTheme {
                var purchaseHistory by remember { mutableStateOf(listOf<Map<String, Any>>()) }

                val db = FirebaseFirestore.getInstance()
                val auth = FirebaseAuth.getInstance()
                val userId = auth.currentUser?.uid

                LaunchedEffect(userId) {
                    if (userId != null) {
                        fetchPurchaseHistory(db, userId) { history ->
                            purchaseHistory = history
                        }
                    }
                }

                PurchaseHistoryScreenView(purchaseHistory)
            }
        }
    }

    private fun fetchPurchaseHistory(
        db: FirebaseFirestore,
        userId: String,
        onUpdate: (List<Map<String, Any>>) -> Unit
    ) {
        db.collection("History")
            .whereEqualTo("userId", userId) // Filter by the logged-in user's ID
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Timber.tag("PurchaseHistory").e(error, "Failed to fetch purchase history.")
                    return@addSnapshotListener
                }

                val historyList = snapshot?.documents
                    ?.map { it.data ?: emptyMap<String, Any>() }
                    ?.sortedByDescending { document ->
                        (document["datePurchased"] as? com.google.firebase.Timestamp)?.toDate()
                    } ?: emptyList()

                onUpdate(historyList)
            }
    }
}
