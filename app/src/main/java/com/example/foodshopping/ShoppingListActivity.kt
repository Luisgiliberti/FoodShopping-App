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

    private var ownedShoppingLists by mutableStateOf<List<ShoppingList>>(emptyList())
    private var sharedShoppingLists by mutableStateOf<List<ShoppingList>>(emptyList())
    private var friendsList by mutableStateOf<List<String>>(emptyList())

    private val shoppingLists: List<ShoppingList>
        get() = ownedShoppingLists + sharedShoppingLists

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        fetchShoppingLists()

        setContent {
            FoodShoppingTheme {
                ShoppingListView(
                    shoppingLists = shoppingLists,
                    friendsList = friendsList,
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

    private fun fetchShoppingLists() {
        userId?.let { uid ->
            db.collection("User").document(uid).get()
                .addOnSuccessListener { doc ->
                    friendsList = (doc.get("friends_list") as? List<String>) ?: emptyList()
                }
                .addOnFailureListener { e ->
                    Log.e("ShoppingListActivity", "Error fetching friends list: $e")
                }

            db.collection("ShoppingList")
                .whereEqualTo("created_by", uid)
                .addSnapshotListener { snapshots, e ->
                    if (e != null) {
                        Log.e("ShoppingListActivity", "Error fetching owned shopping lists: $e")
                        return@addSnapshotListener
                    }

                    val tempOwned = mutableListOf<ShoppingList>()
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
                        tempOwned.add(list)
                    }
                    ownedShoppingLists = tempOwned
                    Log.d("ShoppingListActivity", "Owned lists updated: $ownedShoppingLists")
                }

            db.collection("ShoppingList")
                .whereArrayContains("shared_with", userEmail ?: "")
                .addSnapshotListener { snapshots, e ->
                    if (e != null) {
                        Log.e("ShoppingListActivity", "Error fetching shared shopping lists: $e")
                        return@addSnapshotListener
                    }

                    if (snapshots == null) return@addSnapshotListener

                    val tempShared = mutableListOf<ShoppingList>()
                    val tasks = mutableListOf<com.google.android.gms.tasks.Task<*>?>()

                    snapshots.documents.forEach { doc ->
                        val createdBy = doc.getString("created_by") ?: ""
                        val sharedWith = (doc.get("shared_with") as? List<String>) ?: emptyList()

                        val task = db.collection("User").document(createdBy).get()
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
                                tempShared.add(list)
                                sharedShoppingLists = tempShared
                                Log.d("ShoppingListActivity", "Shared lists updated: $sharedShoppingLists")
                            }
                            .addOnFailureListener { ex ->
                                Log.e("ShoppingListActivity", "Error fetching creator's username: $ex")
                            }
                        tasks.add(task)
                    }

                    if (snapshots.documents.isEmpty()) {
                        sharedShoppingLists = emptyList()
                        Log.d("ShoppingListActivity", "No shared shopping lists found.")
                    }
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
                .addOnSuccessListener { documentReference ->
                    Log.d("ShoppingListActivity", "Shopping list added with ID: ${documentReference.id}")
                }
                .addOnFailureListener { e ->
                    Log.e("ShoppingListActivity", "Error adding shopping list: $e")
                }
        }
    }

    private fun renameShoppingList(listId: String, newName: String) {
        db.collection("ShoppingList").document(listId).update("name", newName)
            .addOnSuccessListener {
                Log.d("ShoppingListActivity", "Shopping list renamed to $newName")
            }
            .addOnFailureListener { e ->
                Log.e("ShoppingListActivity", "Error renaming shopping list: $e")
            }
    }

    private fun deleteShoppingList(listId: String) {
        db.collection("ShoppingList").document(listId).delete()
            .addOnSuccessListener {
                Log.d("ShoppingListActivity", "Shopping list deleted")
            }
            .addOnFailureListener { e ->
                Log.e("ShoppingListActivity", "Error deleting shopping list: $e")
            }
    }

    private fun updateSharedUsers(listId: String, updatedUsers: List<String>) {
        db.collection("ShoppingList").document(listId).update("shared_with", updatedUsers)
            .addOnSuccessListener {
                Log.d("ShoppingListActivity", "Shared users updated")
            }
            .addOnFailureListener { e ->
                Log.e("ShoppingListActivity", "Error updating shared users: $e")
            }
    }

    private fun navigateToShopListProductsActivity(context: Context, shoppingListId: String) {
        val intent = Intent(context, ShopListProductsActivity::class.java).apply {
            putExtra("SHOPPING_LIST_ID", shoppingListId)
        }
        context.startActivity(intent)
        overridePendingTransition(0, 0)
    }
}
