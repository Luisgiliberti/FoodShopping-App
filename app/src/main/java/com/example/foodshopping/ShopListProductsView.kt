package com.example.foodshopping

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.DialogProperties
import com.google.firebase.firestore.FirebaseFirestore
import timber.log.Timber

@Composable
fun ShoppingListScreenView(
    searchText: String,
    onSearchTextChange: (String) -> Unit,
    searchResults: List<Product>,
    onSearchResultClick: (Product) -> Unit,
    shoppingListSearchText: String,
    onShoppingListSearchTextChange: (String) -> Unit,
    shoppingList: List<Map<String, Any>>,
    onRemoveShoppingListItem: (Map<String, Any>) -> Unit,
    db: FirebaseFirestore,
    shoppingListId: String,
    onBuy: () -> Unit
) {
    val filteredShoppingList = remember(shoppingListSearchText, shoppingList) {
        shoppingList.filter { productMap ->
            val productName = productMap["name"]?.toString()?.lowercase() ?: ""
            productName.contains(shoppingListSearchText.lowercase())
        }
    }

    // State for the Buy and Confirmation dialogs
    var showConfirmDialog by remember { mutableStateOf(false) }
    val selectedProducts = shoppingList.filter { it["checked"] as? Boolean == true }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.linearGradient(
                    colors = listOf(
                        Color(0xFFFFCDD2),
                        Color(0xFFBBDEFB),
                        Color(0xFFFFF176)
                    )
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxHeight()
                .padding(bottom = 64.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Top,
        ) {
            Text(
                text = "Shopping List",
                style = MaterialTheme.typography.headlineMedium,
                color = Color.Black,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            OutlinedTextField(
                value = searchText,
                onValueChange = onSearchTextChange,
                label = { Text("Add Product", color = Color.Black) },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                textStyle = LocalTextStyle.current.copy(color = Color.Black)
            )

            if (searchResults.isNotEmpty()) {
                LazyColumn {
                    items(searchResults) { product ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(text = product.name, fontSize = 16.sp, color = Color.Black)
                                Text(
                                    text = product.category,
                                    fontSize = 14.sp,
                                    color = Color.Gray
                                )
                            }
                            Button(
                                onClick = { onSearchResultClick(product) },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFF445E91)
                                )
                            ) {
                                Text("Add", color = Color.White)
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Your Shopping List",
                style = MaterialTheme.typography.headlineSmall,
                color = Color.Black,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                OutlinedTextField(
                    value = shoppingListSearchText,
                    onValueChange = onShoppingListSearchTextChange,
                    label = { Text("Search in Shopping List", color = Color.Black) },
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                    textStyle = LocalTextStyle.current.copy(color = Color.Black)
                )
                Spacer(modifier = Modifier.width(8.dp))

                Button(
                    onClick = { showConfirmDialog = true },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF445E91)),
                ) {
                    Text("Buy", color = Color.White)
                }
            }

            if (filteredShoppingList.isEmpty()) {
                Text(
                    text = "No items match your search.",
                    color = Color.Gray,
                    modifier = Modifier.padding(top = 8.dp)
                )
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f)
                ) {
                    items(filteredShoppingList) { productMap ->
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .background(Color.White, shape = MaterialTheme.shapes.medium)
                                .padding(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = productMap["name"] as String,
                                        fontSize = 18.sp,
                                        color = Color.Black
                                    )
                                    Text(
                                        text = "Quantity: ${productMap["quantity"]}",
                                        fontSize = 14.sp,
                                        color = Color.Black
                                    )
                                    Text(
                                        text = "Category: ${productMap["category"]}",
                                        fontSize = 14.sp,
                                        color = Color.Gray
                                    )
                                    Text(
                                        text = "Added by: ${productMap["addedBy"]}",
                                        fontSize = 14.sp,
                                        color = Color.Gray
                                    )
                                }
                                Checkbox(
                                    checked = productMap["checked"] as? Boolean ?: false,
                                    onCheckedChange = { isChecked ->
                                        val productId = productMap["name"] as String
                                        val shoppingListRef = db.collection("ShoppingList").document(shoppingListId)

                                        db.runTransaction { transaction ->
                                            val snapshot = transaction.get(shoppingListRef)
                                            val productsList = snapshot.get("products_list") as MutableList<Map<String, Any>>

                                            val updatedProducts = productsList.map { product ->
                                                if (product["name"] == productId) {
                                                    product.toMutableMap().apply {
                                                        this["checked"] = isChecked
                                                    }
                                                } else {
                                                    product
                                                }
                                            }

                                            transaction.update(shoppingListRef, "products_list", updatedProducts)
                                        }.addOnSuccessListener {
                                            Timber.d("Successfully updated checked status for $productId")
                                        }.addOnFailureListener { e ->
                                            Timber.e(e, "Failed to update checked status for $productId")
                                        }
                                    },
                                    colors = CheckboxDefaults.colors(
                                        checkedColor = Color(0xFF4CAF50),
                                        uncheckedColor = Color.Gray
                                    )
                                )
                                IconButton(onClick = { onRemoveShoppingListItem(productMap) }) {
                                    Text(
                                        text = "X",
                                        color = Color.Red,
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        if (showConfirmDialog) {
            ConfirmBuyDialog(
                selectedProducts = selectedProducts,
                onCancel = { showConfirmDialog = false },
                onConfirm = {
                    showConfirmDialog = false
                    onBuy()
                }
            )
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
        ) {
            BottomNavigationBar(currentScreen = "Shopping List Products")
        }
    }
}

@Composable
fun ConfirmBuyDialog(
    selectedProducts: List<Map<String, Any>>,
    onCancel: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = { onCancel() },
        title = { Text("Confirm Purchase") },
        text = {
            Column {
                Text("The following items will be purchased:")
                Spacer(modifier = Modifier.height(8.dp))
                selectedProducts.forEach { product ->
                    Text("- ${product["name"]} (x${product["quantity"]})")
                }
            }
        },
        confirmButton = {
            Button(onClick = onConfirm) {
                Text("Buy")
            }
        },
        dismissButton = {
            Button(onClick = onCancel) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun QuantityDialog(
    product: Product?,
    quantity: String,
    onQuantityChange: (String) -> Unit,
    onAdd: () -> Unit,
    onCancel: () -> Unit
) {
    if (product == null) return

    AlertDialog(
        onDismissRequest = { onCancel() },
        title = { Text("Enter Quantity") },
        text = {
            Column {
                Text("How many ${product.name} would you like to add?")
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = quantity,
                    onValueChange = { value ->
                        if (value.all { it.isDigit() }) {
                            onQuantityChange(value)
                        }
                    },
                    label = { Text("Quantity") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(onClick = onAdd) {
                Text("Add")
            }
        },
        dismissButton = {
            Button(onClick = onCancel) {
                Text("Cancel")
            }
        },
        properties = DialogProperties()
    )
}
