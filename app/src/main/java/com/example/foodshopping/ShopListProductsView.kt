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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.DialogProperties

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
    currentScreen: String
) {
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
                .padding(bottom = 64.dp), // Leave space for the navigation bar
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Top,
        ) {
            // Search bar for products
            Text(
                text = "Search for Products",
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

            // Search results display
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

            // Search bar for shopping list
            Text(
                text = "Your Shopping List",
                style = MaterialTheme.typography.headlineSmall,
                color = Color.Black,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            OutlinedTextField(
                value = shoppingListSearchText,
                onValueChange = onShoppingListSearchTextChange,
                label = { Text("Filter Shopping List", color = Color.Black) },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                textStyle = LocalTextStyle.current.copy(color = Color.Black)
            )

            // Shopping list display
            if (shoppingList.isEmpty()) {
                Text(
                    text = "No items in the shopping list.",
                    color = Color.Gray,
                    modifier = Modifier.padding(top = 8.dp)
                )
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f)
                ) {
                    items(shoppingList) { productMap ->
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

        // Navigation bar positioned independently at the bottom
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
        ) {
            BottomNavigationBar(currentScreen = currentScreen)
        }
    }
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
                    onValueChange = onQuantityChange,
                    label = { Text("Quantity") },
                    singleLine = true,
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