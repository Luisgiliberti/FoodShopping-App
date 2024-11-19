package com.example.foodshopping

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.*
import com.example.foodshopping.ui.theme.FoodShoppingTheme
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldPath
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore

class ShopListProductsActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            FoodShoppingTheme {
                var searchText by remember { mutableStateOf("") }
                var searchResults by remember { mutableStateOf(listOf<Product>()) }
                var shoppingList by remember { mutableStateOf(listOf<Map<String, Any>>()) }
                var shoppingListSearchText by remember { mutableStateOf("") }
                var showQuantityDialog by remember { mutableStateOf(false) }
                var selectedProduct by remember { mutableStateOf<Product?>(null) }
                var quantity by remember { mutableStateOf("") }

                val db = FirebaseFirestore.getInstance()
                val auth = FirebaseAuth.getInstance()
                val userId = auth.currentUser?.uid
                val shoppingListId = intent.getStringExtra("SHOPPING_LIST_ID") ?: return@FoodShoppingTheme

                // Fetch Shopping List on Launch
                LaunchedEffect(shoppingListId) {
                    fetchShoppingList(db, shoppingListId) { updatedList ->
                        shoppingList = updatedList
                    }
                }

                // Quantity Dialog
                if (showQuantityDialog) {
                    QuantityDialog(
                        product = selectedProduct,
                        quantity = quantity,
                        onQuantityChange = { quantity = it },
                        onAdd = {
                            val product = selectedProduct ?: return@QuantityDialog
                            val quantityInt = quantity.toIntOrNull() ?: 1
                            if (userId != null) {
                                addProductToShoppingList(
                                    product,
                                    quantityInt,
                                    userId,
                                    db,
                                    shoppingListId
                                )
                                shoppingList = shoppingList + mapOf(
                                    "name" to product.name,
                                    "category" to product.category,
                                    "quantity" to quantityInt,
                                    "addedBy" to userId
                                )
                                showQuantityDialog = false
                                quantity = ""
                                selectedProduct = null
                            } else {
                                Log.w("ShoppingListScreen", "User ID is null; cannot add product.")
                            }
                        },
                        onCancel = {
                            showQuantityDialog = false
                        }
                    )
                }

                // Shopping List Screen
                ShoppingListScreenView(
                    searchText = searchText,
                    onSearchTextChange = { query ->
                        searchText = query
                        searchResults = if (query.isNotBlank()) {
                            ProductList.getProducts(this).filter {
                                it.name.startsWith(query, ignoreCase = true) &&
                                        shoppingList.none { productMap -> productMap["name"] == it.name }
                            }.take(3)
                        } else {
                            listOf()
                        }
                    },
                    searchResults = searchResults,
                    onSearchResultClick = { product ->
                        selectedProduct = product
                        showQuantityDialog = true
                    },
                    shoppingListSearchText = shoppingListSearchText,
                    onShoppingListSearchTextChange = { shoppingListSearchText = it },
                    shoppingList = shoppingList,
                    onRemoveShoppingListItem = { productMap ->
                        removeProductFromShoppingList(productMap, db, shoppingListId)
                        shoppingList = shoppingList - productMap
                    },
                    currentScreen = "Shopping List Products"
                )
            }
        }
    }

    private fun fetchShoppingList(
        db: FirebaseFirestore,
        shoppingListId: String,
        onUpdate: (List<Map<String, Any>>) -> Unit
    ) {
        val shoppingListRef = db.collection("ShoppingList").document(shoppingListId)

        shoppingListRef.addSnapshotListener { snapshot, error ->
            if (error != null) {
                Log.w("ShoppingListScreen", "Listen failed.", error)
                return@addSnapshotListener
            }

            val updatedList = mutableListOf<Map<String, Any>>()
            val products = snapshot?.get("products_list") as? List<Map<String, Any>> ?: listOf()
            val userIds = products.mapNotNull { it["addedBy"] as? String }.toSet()

            if (userIds.isNotEmpty()) {
                db.collection("User").whereIn(FieldPath.documentId(), userIds.toList()).get()
                    .addOnSuccessListener { userSnapshots ->
                        val userMap = userSnapshots.documents.associate {
                            it.id to (it.getString("username") ?: "Unknown")
                        }

                        products.forEach { product ->
                            val addedById = product["addedBy"] as? String
                            val updatedProduct = product.toMutableMap()
                            updatedProduct["addedBy"] = userMap[addedById] ?: "Unknown"
                            updatedList.add(updatedProduct)
                        }

                        onUpdate(updatedList)
                    }
                    .addOnFailureListener {
                        Log.e("ShoppingListScreen", "Failed to fetch usernames", it)
                        onUpdate(products)
                    }
            } else {
                onUpdate(products)
            }
        }
    }


    private fun addProductToShoppingList(
        product: Product,
        quantity: Int,
        addedBy: String,
        db: FirebaseFirestore,
        shoppingListId: String
    ) {
        val productData = mapOf(
            "name" to product.name,
            "category" to product.category,
            "quantity" to quantity,
            "addedBy" to addedBy
        )

        val shoppingListRef = db.collection("ShoppingList").document(shoppingListId)

        shoppingListRef.update("products_list", FieldValue.arrayUnion(productData))
            .addOnSuccessListener {
                Log.d("ShoppingList", "Product added to shopping list.")
            }
            .addOnFailureListener { e ->
                Log.e("ShoppingList", "Failed to add product to shopping list", e)
            }
    }

    private fun removeProductFromShoppingList(
        productMap: Map<String, Any>,
        db: FirebaseFirestore,
        shoppingListId: String
    ) {
        // Fetch userId based on username from the User collection
        val username = productMap["addedBy"] as? String ?: return

        db.collection("User")
            .whereEqualTo("username", username)
            .get()
            .addOnSuccessListener { querySnapshot ->
                val userId = querySnapshot.documents.firstOrNull()?.id
                if (userId != null) {
                    // Replace addedBy field in product data with userId
                    val productData = mapOf(
                        "name" to productMap["name"],
                        "category" to productMap["category"],
                        "quantity" to productMap["quantity"],
                        "addedBy" to userId
                    )

                    val shoppingListRef = db.collection("ShoppingList").document(shoppingListId)

                    // Remove the product from Firestore
                    shoppingListRef.update("products_list", FieldValue.arrayRemove(productData))
                        .addOnSuccessListener {
                            Log.d("ShoppingList", "Product removed from shopping list.")
                        }
                        .addOnFailureListener { e ->
                            Log.e("ShoppingList", "Failed to remove product from shopping list", e)
                        }
                } else {
                    Log.e("ShoppingList", "Failed to find userId for username: $username")
                }
            }
            .addOnFailureListener { e ->
                Log.e("ShoppingList", "Failed to fetch userId for username: $username", e)
            }
    }

}
