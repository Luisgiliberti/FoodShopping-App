package com.example.foodshopping

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun PurchaseHistoryScreenView(purchaseHistory: List<Map<String, Any>>) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.linearGradient(
                    colors = listOf(
                        Color(0xFFC8E6C9),
                        Color(0xFFBBDEFB),
                        Color(0xFFFFF176)
                    )
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = 56.dp)
        ) {
            Text(
                text = "Purchase History",
                style = MaterialTheme.typography.headlineMedium,
                color = Color.Black,
                modifier = Modifier
                    .padding(vertical = 16.dp)
                    .align(Alignment.CenterHorizontally)
            )

            if (purchaseHistory.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No purchase history available.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = Color.Gray
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp)
                ) {
                    items(purchaseHistory) { purchase ->
                        PurchaseHistoryItem(purchase)
                        Spacer(modifier = Modifier.height(12.dp))
                    }
                }
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
        ) {
            BottomNavigationBar(currentScreen = "Purchase History")
        }
    }
}

@Composable
fun PurchaseHistoryItem(purchase: Map<String, Any>) {
    val shoppingListName = purchase["shoppingListName"] as? String ?: "Unnamed List"
    val datePurchased = (purchase["datePurchased"] as? com.google.firebase.Timestamp)?.toDate()
    val products = purchase["products"] as? List<Map<String, Any>> ?: emptyList()

    val formattedDate = datePurchased?.let {
        SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()).format(it)
    } ?: "Unknown Date"

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(6.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = shoppingListName,
                style = MaterialTheme.typography.headlineSmall,
                color = Color.Black,
                modifier = Modifier.padding(bottom = 4.dp)
            )
            Text(
                text = "Date Purchased: $formattedDate",
                style = MaterialTheme.typography.bodySmall,
                color = Color.Gray,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            if (products.isNotEmpty()) {
                Text(
                    text = "Products:",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.Black
                )
                products.forEach { product ->
                    val name = product["name"] as? String ?: "Unknown Product"
                    val quantity = product["quantity"] as? Long ?: 0
                    Text(
                        text = "- $name (x$quantity)",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Black,
                        modifier = Modifier.padding(start = 8.dp, top = 4.dp)
                    )
                }
            } else {
                Text(
                    text = "No products were purchased.",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
        }
    }
}
