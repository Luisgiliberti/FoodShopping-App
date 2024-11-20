package com.example.foodshopping

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.*
import com.example.foodshopping.ui.theme.FoodShoppingTheme
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldPath
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import timber.log.Timber

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

                LaunchedEffect(shoppingListId) {
                    fetchShoppingList(db, shoppingListId) { updatedList ->
                        shoppingList = updatedList
                    }
                }

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
                                Timber.tag("ShoppingListScreen")
                                    .w("User ID is null; cannot add product.")
                            }
                        },
                        onCancel = {
                            showQuantityDialog = false
                        }
                    )
                }

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
                    db = db,
                    shoppingListId = shoppingListId,
                    onBuy = {
                        val shoppingListRef = db.collection("ShoppingList").document(shoppingListId)
                        val historyRef = db.collection("History")

                        db.runTransaction { transaction ->
                            val snapshot = transaction.get(shoppingListRef)
                            val productsList = snapshot.get("products_list") as MutableList<Map<String, Any>>
                            val purchasedProducts = productsList.filter { it["checked"] as? Boolean == true }
                            val remainingProducts = productsList.filter { it["checked"] as? Boolean != true }

                            // Get the shopping list name
                            val shoppingListName = snapshot.getString("name") ?: "Unnamed List"

                            // Prepare the history data
                            val historyData = mapOf(
                                "shoppingListName" to shoppingListName,
                                "datePurchased" to com.google.firebase.Timestamp.now(),
                                "products" to purchasedProducts,
                                "userId" to userId
                            )

                            // Add to History collection
                            transaction.set(historyRef.document(), historyData)

                            // Update the Shopping List to remove purchased products
                            transaction.update(shoppingListRef, "products_list", remainingProducts)
                        }.addOnSuccessListener {
                            Timber.d("Successfully added to purchase history and updated shopping list.")
                        }.addOnFailureListener { e ->
                            Timber.e(e, "Failed to transfer items to purchase history.")
                        }
                    }

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
                Timber.tag("ShoppingListScreen").w(error, "Listen failed.")
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
                        Timber.tag("ShoppingListScreen").e(it, "Failed to fetch usernames")
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
            "addedBy" to addedBy,
            "checked" to false
        )

        val shoppingListRef = db.collection("ShoppingList").document(shoppingListId)

        shoppingListRef.update("products_list", FieldValue.arrayUnion(productData))
            .addOnSuccessListener {
                Timber.tag("ShoppingList").d("Product added to shopping list.")
            }
            .addOnFailureListener { e ->
                Timber.tag("ShoppingList").e(e, "Failed to add product to shopping list")
            }
    }

    private fun removeProductFromShoppingList(
        productMap: Map<String, Any>,
        db: FirebaseFirestore,
        shoppingListId: String
    ) {
        val username = productMap["addedBy"] as? String ?: return

        db.collection("User")
            .whereEqualTo("username", username)
            .get()
            .addOnSuccessListener { querySnapshot ->
                val userId = querySnapshot.documents.firstOrNull()?.id
                if (userId != null) {
                    val shoppingListRef = db.collection("ShoppingList").document(shoppingListId)
                    shoppingListRef.get()
                        .addOnSuccessListener { document ->
                            val productsList = document.get("products_list") as? List<Map<String, Any>> ?: emptyList()
                            val productToRemove = productsList.firstOrNull { product ->
                                product["name"] == productMap["name"] &&
                                        product["category"] == productMap["category"] &&
                                        product["quantity"] == productMap["quantity"] &&
                                        product["addedBy"] == userId
                            }

                            if (productToRemove != null) {
                                shoppingListRef.update("products_list", FieldValue.arrayRemove(productToRemove))
                                    .addOnSuccessListener {
                                        Timber.tag("ShoppingList").d("Product removed from shopping list.")
                                    }
                                    .addOnFailureListener { e ->
                                        Timber.tag("ShoppingList")
                                            .e(e, "Failed to remove product from shopping list")
                                    }
                            } else {
                                Timber.tag("ShoppingList").e("Product not found in the shopping list.")
                            }
                        }
                        .addOnFailureListener { e ->
                            Timber.tag("ShoppingList")
                                .e(e, "Failed to fetch shopping list for ID: $shoppingListId")
                        }
                } else {
                    Timber.tag("ShoppingList").e("Failed to find userId for username: $username")
                }
            }
            .addOnFailureListener { e ->
                Timber.tag("ShoppingList").e(e, "Failed to fetch userId for username: $username")
            }
    }
}
