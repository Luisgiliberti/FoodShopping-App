package com.example.foodshopping

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.*
import com.example.foodshopping.ui.theme.FoodShoppingTheme
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

data class ShoppingList(
    val name: String,
    val id: String,
    val createdBy: String,
    val username: String,
    val sharedWith: List<String>,
    val isOwned: Boolean
)

class ShoppingListActivity : ComponentActivity() {

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val userId = auth.currentUser?.uid
    private val userEmail = auth.currentUser?.email

    private var shoppingLists by mutableStateOf<List<ShoppingList>>(emptyList()) // Combined lists
    private var friendsList by mutableStateOf<List<String>>(emptyList())
    private var isShopping by mutableStateOf(false)
    private var friendsShopping by mutableStateOf<List<String>>(emptyList())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        fetchShoppingLists()
        fetchShoppingStatus()

        setContent {
            FoodShoppingTheme {
                ShoppingListView(
                    shoppingLists = shoppingLists,
                    friendsList = friendsList,
                    friendsShopping = friendsShopping,
                    isShopping = isShopping,
                    onShoppingStatusChange = { newStatus ->
                        updateShoppingStatus(newStatus)
                    },
                    onAddShoppingList = { name, sharedFriends ->
                        addShoppingList(name, sharedFriends)
                    },
                    onRenameShoppingList = { listId, newName ->
                        renameShoppingList(listId, newName)
                    },
                    onDeleteShoppingList = { listId ->
                        deleteShoppingList(listId)
                    },
                    onUpdateSharedUsers = { listId, updatedFriends ->
                        updateSharedUsers(listId, updatedFriends)
                    },
                    onNavigateToProducts = { shoppingListId ->
                        navigateToShopListProductsActivity(this, shoppingListId)
                    }
                )
            }
        }
    }

    private fun fetchShoppingStatus() {
        userId?.let { uid ->
            db.collection("User").document(uid).get()
                .addOnSuccessListener { doc ->
                    isShopping = doc.getBoolean("shopping_status") ?: false
                }
                .addOnFailureListener { e ->
                    Log.e("ShoppingListActivity", "Error fetching shopping status: $e")
                }
        }
    }

    private fun updateShoppingStatus(newStatus: Boolean) {
        userId?.let { uid ->
            db.collection("User").document(uid).update("shopping_status", newStatus)
                .addOnSuccessListener {
                    isShopping = newStatus
                    fetchFriendsShoppingStatus()
                }
                .addOnFailureListener { e ->
                    Log.e("ShoppingListActivity", "Error updating shopping status: $e")
                }
        }
    }

    private fun fetchShoppingLists() {
        userId?.let { uid ->
            val tempShoppingLists = mutableListOf<ShoppingList>()

            // Fetch user's friends list
            db.collection("User").document(uid).get()
                .addOnSuccessListener { doc ->
                    friendsList = (doc.get("friends_list") as? List<String>) ?: emptyList()
                    fetchFriendsShoppingStatus()
                }
                .addOnFailureListener { e ->
                    Log.e("ShoppingListActivity", "Error fetching friends list: $e")
                }

            // Fetch owned shopping lists
            db.collection("ShoppingList")
                .whereEqualTo("created_by", uid)
                .get()
                .addOnSuccessListener { snapshots ->
                    snapshots?.documents?.forEach { doc ->
                        val sharedWith = (doc.get("shared_with") as? List<String>) ?: emptyList()
                        val list = ShoppingList(
                            name = doc.getString("name") ?: "",
                            id = doc.id,
                            createdBy = uid,
                            username = "You",
                            sharedWith = sharedWith,
                            isOwned = true
                        )
                        tempShoppingLists.add(list)
                    }

                    // Fetch shared shopping lists
                    db.collection("ShoppingList")
                        .whereArrayContains("shared_with", userEmail ?: "")
                        .get()
                        .addOnSuccessListener { sharedSnapshots ->
                            sharedSnapshots?.documents?.forEach { doc ->
                                val createdBy = doc.getString("created_by") ?: ""
                                val sharedWith = (doc.get("shared_with") as? List<String>) ?: emptyList()
                                db.collection("User").document(createdBy).get()
                                    .addOnSuccessListener { creatorDoc ->
                                        val username = creatorDoc.getString("username") ?: "Unknown"
                                        val list = ShoppingList(
                                            name = doc.getString("name") ?: "",
                                            id = doc.id,
                                            createdBy = createdBy,
                                            username = username,
                                            sharedWith = sharedWith,
                                            isOwned = false
                                        )
                                        tempShoppingLists.add(list)
                                        shoppingLists = tempShoppingLists // Update the combined list
                                    }
                            }
                        }
                        .addOnFailureListener { e ->
                            Log.e("ShoppingListActivity", "Error fetching shared shopping lists: $e")
                        }
                }
                .addOnFailureListener { e ->
                    Log.e("ShoppingListActivity", "Error fetching owned shopping lists: $e")
                }
        }
    }

    private fun fetchFriendsShoppingStatus() {
        val shoppingFriends = mutableListOf<String>()
        friendsList.forEach { friendEmail ->
            db.collection("User")
                .whereEqualTo("email", friendEmail)
                .get()
                .addOnSuccessListener { documents ->
                    for (doc in documents) {
                        val isShopping = doc.getBoolean("shopping_status") ?: false
                        val username = doc.getString("username") ?: "Unknown"
                        if (isShopping) {
                            shoppingFriends.add(username)
                        }
                    }
                    friendsShopping = shoppingFriends
                }
                .addOnFailureListener { e ->
                    Log.e("ShoppingListActivity", "Error fetching friend's shopping status: $e")
                }
        }
    }

    private fun addShoppingList(name: String, sharedFriends: List<String>) {
        userId?.let { uid ->
            val shoppingListData = mapOf(
                "name" to name,
                "shared_with" to sharedFriends,
                "created_by" to uid
            )

            db.collection("ShoppingList").add(shoppingListData)
        }
    }

    private fun renameShoppingList(listId: String, newName: String) {
        db.collection("ShoppingList").document(listId).update("name", newName)
    }

    private fun deleteShoppingList(listId: String) {
        db.collection("ShoppingList").document(listId).delete()
    }

    private fun updateSharedUsers(listId: String, updatedUsers: List<String>) {
        db.collection("ShoppingList").document(listId).update("shared_with", updatedUsers)
    }

    private fun navigateToShopListProductsActivity(context: Context, shoppingListId: String) {
        val intent = Intent(context, ShopListProductsActivity::class.java).apply {
            putExtra("SHOPPING_LIST_ID", shoppingListId)
        }
        context.startActivity(intent)
        overridePendingTransition(0, 0)
    }
}
