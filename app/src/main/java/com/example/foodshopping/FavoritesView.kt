package com.example.foodshopping

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun FavoritesView(
    searchText: String,
    onSearchTextChange: (String) -> Unit,
    searchResults: List<Product>,
    favorites: List<Product>,
    shoppingLists: List<String>,
    onAddFavorite: (Product) -> Unit,
    onRemoveFavorite: (Product) -> Unit,
    onAddToShoppingList: (Product, String, Int) -> Unit, // Ensure this parameter is defined and used
) {
    var showAddDialog by remember { mutableStateOf(false) }
    var selectedProduct by remember { mutableStateOf<Product?>(null) }

    Box {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(Color(0xFFFFCDD2), Color(0xFFBBDEFB), Color(0xFFFFF176))
                    )
                )
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Search Text Field
            Text(
                text = "Search for Products",
                style = MaterialTheme.typography.headlineMedium,
                color = Color.Black,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            OutlinedTextField(
                value = searchText,
                onValueChange = onSearchTextChange,
                label = { Text("Enter Product", color = Color.Black) },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                textStyle = TextStyle(color = Color.Black)
            )

            // Search Results List
            if (searchResults.isNotEmpty()) {
                LazyColumn(modifier = Modifier.fillMaxWidth()) {
                    items(searchResults) { product ->
                        SearchResultItem(
                            product = product,
                            onAddFavorite = onAddFavorite,
                            isFavorite = favorites.any { it.name == product.name }
                        )
                    }
                }
            } else if (searchText.isNotEmpty()) {
                Text(
                    text = "No matching products found.",
                    color = Color.Gray,
                    modifier = Modifier.padding(8.dp)
                )
            }

            // Favorites List
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Your Favorites",
                style = MaterialTheme.typography.headlineSmall,
                color = Color.Black,
                modifier = Modifier.padding(vertical = 8.dp)
            )
            if (favorites.isEmpty()) {
                Text(
                    text = "No favorites added.",
                    color = Color.Gray,
                    modifier = Modifier.padding(8.dp)
                )
            } else {
                LazyColumn(modifier = Modifier.fillMaxWidth()) {
                    items(favorites) { product ->
                        FavoriteItem(
                            product = product,
                            onRemoveFavorite = onRemoveFavorite,
                            onAddToShoppingList = { // Show dialog for shopping list addition
                                selectedProduct = product
                                showAddDialog = true
                            }
                        )
                    }
                }
            }
        }

        // Add to Shopping List Dialog
        if (showAddDialog && selectedProduct != null) {
            AddToShoppingListDialog(
                product = selectedProduct!!,
                shoppingLists = shoppingLists,
                onDismiss = { showAddDialog = false },
                onConfirm = { listId, quantity ->
                    onAddToShoppingList(selectedProduct!!, listId, quantity)
                    showAddDialog = false
                }
            )
        }

        // Bottom Navigation
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
        ) {
            BottomNavigationBar(currentScreen = "Favorites")
        }
    }
}

@Composable
fun SearchResultItem(
    product: Product,
    onAddFavorite: (Product) -> Unit,
    isFavorite: Boolean
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = product.name,
                color = Color.Black,
                fontSize = 16.sp
            )
            Text(
                text = product.category,
                color = Color.Gray,
                fontSize = 14.sp
            )
        }
        Button(
            onClick = { onAddFavorite(product) },
            colors = ButtonDefaults.buttonColors(
                containerColor = if (isFavorite) Color.Gray else Color(0xFF445E91)
            ),
            enabled = !isFavorite
        ) {
            Text(
                text = if (isFavorite) "Added" else "Add to Favorites",
                color = Color.White
            )
        }
    }
}

@Composable
fun FavoriteItem(
    product: Product,
    onRemoveFavorite: (Product) -> Unit,
    onAddToShoppingList: (Product) -> Unit // Add to shopping list callback
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(4.dp)
            .background(Color.White, shape = MaterialTheme.shapes.medium)
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = product.name,
                color = Color.Black,
                fontSize = 18.sp
            )
            Text(
                text = product.category,
                color = Color.Gray,
                fontSize = 16.sp
            )
        }
        Button(
            onClick = { onAddToShoppingList(product) },
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50))
        ) {
            Text(text = "Add to List", color = Color.White)
        }
        IconButton(onClick = { onRemoveFavorite(product) }, modifier = Modifier.size(24.dp)) {
            Text(
                text = "X",
                color = Color.Red,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun AddToShoppingListDialog(
    product: Product,
    shoppingLists: List<String>,
    onDismiss: () -> Unit,
    onConfirm: (String, Int) -> Unit
) {
    var selectedList by remember { mutableStateOf(shoppingLists.firstOrNull() ?: "") }
    var quantity by remember { mutableStateOf("") }
    var expanded by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add ${product.name} to Shopping List") },
        text = {
            Column {
                // Dropdown Menu
                Box {
                    OutlinedButton(onClick = { expanded = true }, modifier = Modifier.fillMaxWidth()) {
                        Text(selectedList.ifEmpty { "Select Shopping List" })
                    }
                    DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                        shoppingLists.forEach { list ->
                            DropdownMenuItem(
                                text = { Text(list) },
                                onClick = {
                                    selectedList = list
                                    expanded = false
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Quantity Input
                OutlinedTextField(
                    value = quantity,
                    onValueChange = { if (it.all { char -> char.isDigit() }) quantity = it },
                    label = { Text("Quantity") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(onClick = {
                val parsedQuantity = quantity.toIntOrNull() ?: 0
                if (selectedList.isNotEmpty() && parsedQuantity > 0) {
                    onConfirm(selectedList, parsedQuantity)
                    onDismiss()
                }
            }) {
                Text("Add")
            }
        },
        dismissButton = {
            Button(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
