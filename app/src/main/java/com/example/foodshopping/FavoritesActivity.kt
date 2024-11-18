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
                    val ingredients = ProductList.getProducts(this)

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
                        currentScreen = "Favorites"
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
}
