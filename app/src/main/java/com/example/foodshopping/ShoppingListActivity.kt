package com.example.foodshopping

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.*
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.foodshopping.MainActivity.Companion.CHANNEL_ID
import com.example.foodshopping.ui.theme.FoodShoppingTheme
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import timber.log.Timber

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

    // Permission launcher for notification permissions
    private val notificationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (isGranted) {
                Timber.d("Notification permission granted.")
            } else {
                Timber.w("Notification permission denied.")
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Request notification permission if required
        requestNotificationPermission()

        createNotificationChannel()
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

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ActivityCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Shopping Notifications"
            val descriptionText = "Notifications for shopping list updates"
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
            }
            val notificationManager: NotificationManager =
                getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun fetchFriendsShoppingStatus() {
        val shoppingFriends = mutableStateListOf<String>() // Use a mutable state list for real-time updates

        friendsList.forEach { friendEmail ->
            db.collection("User")
                .whereEqualTo("email", friendEmail)
                .addSnapshotListener { snapshots, error ->
                    if (error != null) {
                        Log.e("ShoppingListActivity", "Error fetching friend's shopping status: $error")
                        return@addSnapshotListener
                    }

                    snapshots?.documents?.forEach { doc ->
                        val isShopping = doc.getBoolean("shopping_status") ?: false
                        val username = doc.getString("username") ?: "Unknown"

                        if (isShopping && !shoppingFriends.contains(username)) {
                            // Friend started shopping
                            shoppingFriends.add(username)
                            sendLocalNotification("Shopping Update", "Your friend $username started shopping!")
                        } else if (!isShopping && shoppingFriends.contains(username)) {
                            // Friend stopped shopping
                            shoppingFriends.remove(username)
                            sendLocalNotification("Shopping Update", "Your friend $username is not shopping anymore.")
                        }
                    }

                    // Update the real-time list of friends shopping
                    friendsShopping = shoppingFriends
                }
        }
    }

    private fun fetchShoppingStatus() {
        userId?.let { uid ->
            db.collection("User").document(uid).addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("ShoppingListActivity", "Error fetching shopping status: $error")
                    return@addSnapshotListener
                }
                isShopping = snapshot?.getBoolean("shopping_status") ?: false
            }
        }
    }

    private fun updateShoppingStatus(newStatus: Boolean) {
        userId?.let { uid ->
            db.collection("User").document(uid).update("shopping_status", newStatus)
                .addOnFailureListener { e ->
                    Log.e("ShoppingListActivity", "Error updating shopping status: $e")
                }
        }
    }

    private fun sendLocalNotification(title: String, message: String) {
        userId?.let { uid ->
            db.collection("User").document(uid).get()
                .addOnSuccessListener { document ->
                    // Check if shopping_status in notification_settings is false
                    val shoppingStatusEnabled =
                        document.getBoolean("notification_settings.shopping_status") ?: false
                    if (!shoppingStatusEnabled) {
                        Timber.d("Notification not sent because shopping_status is false.")
                        return@addOnSuccessListener
                    }

                    // Proceed to send notification if shopping_status is true
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                        ActivityCompat.checkSelfPermission(
                            this,
                            Manifest.permission.POST_NOTIFICATIONS
                        ) != PackageManager.PERMISSION_GRANTED
                    ) {
                        Timber.w("Notification permission not granted.")
                        return@addOnSuccessListener
                    }

                    val notificationId = System.currentTimeMillis().toInt()
                    val notification = NotificationCompat.Builder(this, CHANNEL_ID)
                        .setSmallIcon(R.drawable.ic_notification) // Replace with your app's icon
                        .setContentTitle(title)
                        .setContentText(message)
                        .setPriority(NotificationCompat.PRIORITY_HIGH)
                        .build()

                    NotificationManagerCompat.from(this).notify(notificationId, notification)
                }
                .addOnFailureListener { e ->
                    Timber.e("Failed to fetch notification settings: $e")
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
                                val sharedWith =
                                    (doc.get("shared_with") as? List<String>) ?: emptyList()
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
                                        shoppingLists =
                                            tempShoppingLists // Update the combined list
                                    }
                            }
                        }
                        .addOnFailureListener { e ->
                            Log.e(
                                "ShoppingListActivity",
                                "Error fetching shared shopping lists: $e"
                            )
                        }
                }
                .addOnFailureListener { e ->
                    Log.e("ShoppingListActivity", "Error fetching owned shopping lists: $e")
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
