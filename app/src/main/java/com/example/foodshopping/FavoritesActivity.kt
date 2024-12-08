package com.example.foodshopping

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import com.example.foodshopping.ui.theme.FoodShoppingTheme
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore

class FavoritesActivity : ComponentActivity() {
    private lateinit var db: FirebaseFirestore
    private lateinit var auth: FirebaseAuth
    private var userId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        db = FirebaseFirestore.getInstance()
        auth = FirebaseAuth.getInstance()
        userId = auth.currentUser?.uid

        setContent {
            FoodShoppingTheme {
                Surface {
                    var searchText by remember { mutableStateOf("") }
                    var searchResults by remember { mutableStateOf(listOf<Product>()) }
                    var favorites by remember { mutableStateOf(listOf<Product>()) }
                    var shoppingLists by remember { mutableStateOf(listOf<Pair<String, String>>()) }

                    val ingredients = ProductList.getProducts(this)

                    LaunchedEffect(userId) {
                        fetchShoppingLists { fetchedLists ->
                            shoppingLists = fetchedLists
                        }
                    }

                    LaunchedEffect(userId) {
                        fetchFavorites { fetchedFavorites ->
                            favorites = fetchedFavorites
                        }
                    }

                    FavoritesView(
                        searchText = searchText,
                        onSearchTextChange = { query ->
                            searchText = query
                            searchResults = if (query.isNotBlank()) {
                                ingredients.filter {
                                    it.name.startsWith(query, ignoreCase = true) &&
                                            favorites.none { fav -> fav.name == it.name }
                                }.take(3)
                            } else {
                                listOf()
                            }
                        },
                        searchResults = searchResults,
                        favorites = favorites,
                        shoppingLists = shoppingLists.map { it.second },
                        onAddFavorite = { product ->
                            addFavorite(product) {
                                favorites = favorites + product
                                searchText = ""
                                searchResults = listOf()
                            }
                        },
                        onRemoveFavorite = { product ->
                            removeFavorite(product) {
                                favorites = favorites - product
                            }
                        },
                        onAddToShoppingList = { product, listName, quantity ->
                            val listId = shoppingLists.firstOrNull { it.second == listName }?.first
                            if (listId != null) {
                                addProductToShoppingList(product, quantity, listId, {}, { e ->
                                    e.printStackTrace()
                                })
                            } else {
                                println("Error: No matching list ID found for name $listName")
                            }
                        }
                    )
                }
            }
        }
    }

    private fun fetchFavorites(onComplete: (List<Product>) -> Unit) {
        userId?.let { uid ->
            db.collection("User").document(uid).get().addOnSuccessListener { document ->
                val favs = document.get("favorites") as? List<Map<String, String>> ?: listOf()
                val favorites = favs.map { fav ->
                    Product(name = fav["name"] ?: "", category = fav["category"] ?: "")
                }
                onComplete(favorites)
            }
        }
    }

    private fun fetchShoppingLists(onComplete: (List<Pair<String, String>>) -> Unit) {
        userId?.let { uid ->
            val userEmail = auth.currentUser?.email.orEmpty()
            db.collection("ShoppingList").get().addOnSuccessListener { snapshot ->
                val fetchedLists = snapshot.documents.filter {
                    val createdBy = it.getString("created_by")
                    val sharedWith = it.get("shared_with") as? List<String> ?: emptyList()
                    createdBy == uid || sharedWith.contains(userEmail)
                }.mapNotNull { doc ->
                    val id = doc.id
                    val name = doc.getString("name") ?: return@mapNotNull null
                    Pair(id, name)
                }
                onComplete(fetchedLists)
            }
        }
    }

    private fun addFavorite(product: Product, onComplete: () -> Unit) {
        userId?.let { uid ->
            val userDoc = db.collection("User").document(uid)
            val productData = mapOf("name" to product.name, "category" to product.category)
            userDoc.update("favorites", FieldValue.arrayUnion(productData))
                .addOnSuccessListener { onComplete() }
        }
    }

    private fun removeFavorite(product: Product, onComplete: () -> Unit) {
        userId?.let { uid ->
            val userDoc = db.collection("User").document(uid)
            val productData = mapOf("name" to product.name, "category" to product.category)
            userDoc.update("favorites", FieldValue.arrayRemove(productData))
                .addOnSuccessListener { onComplete() }
        }
    }

    private fun addProductToShoppingList(
        product: Product,
        quantity: Int,
        shoppingListId: String,
        onSuccess: () -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        userId?.let { uid ->
            val shoppingListRef = db.collection("ShoppingList").document(shoppingListId)
            shoppingListRef.get().addOnSuccessListener { document ->
                if (document.exists()) {
                    val productData = mapOf(
                        "name" to product.name,
                        "category" to product.category,
                        "quantity" to quantity,
                        "addedBy" to uid,
                        "checked" to false
                    )
                    shoppingListRef.update("products_list", FieldValue.arrayUnion(productData))
                        .addOnSuccessListener {
                            onSuccess()
                            println("Product added successfully: $productData to $shoppingListId")
                        }
                        .addOnFailureListener { e ->
                            onFailure(e)
                            println("Failed to add product: ${e.message}")
                        }
                } else {
                    println("Error: Shopping list $shoppingListId does not exist")
                }
            }.addOnFailureListener { e ->
                println("Failed to fetch shopping list: ${e.message}")
            }
        }
    }
}
