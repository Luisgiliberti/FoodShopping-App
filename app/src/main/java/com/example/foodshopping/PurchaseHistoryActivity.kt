package com.example.foodshopping

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.*
import com.example.foodshopping.ui.theme.FoodShoppingTheme
import com.google.firebase.firestore.FirebaseFirestore
import timber.log.Timber

class PurchaseHistoryActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            FoodShoppingTheme {
                var purchaseHistory by remember { mutableStateOf(listOf<Map<String, Any>>()) }

                val db = FirebaseFirestore.getInstance()
                LaunchedEffect(Unit) {
                    fetchPurchaseHistory(db) { history ->
                        purchaseHistory = history
                    }
                }

                PurchaseHistoryScreenView(purchaseHistory)
            }
        }
    }

    private fun fetchPurchaseHistory(
        db: FirebaseFirestore,
        onUpdate: (List<Map<String, Any>>) -> Unit
    ) {
        db.collection("History")
            .orderBy("datePurchased", com.google.firebase.firestore.Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Timber.tag("PurchaseHistory").e(error, "Failed to fetch purchase history.")
                    return@addSnapshotListener
                }

                val historyList = snapshot?.documents?.map { it.data ?: emptyMap<String, Any>() } ?: emptyList()
                onUpdate(historyList)
            }
    }
}
