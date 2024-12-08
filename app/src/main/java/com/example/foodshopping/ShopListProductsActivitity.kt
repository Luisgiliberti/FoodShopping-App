package com.example.foodshopping

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.*
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
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
                val shoppingListId =
                    intent.getStringExtra("SHOPPING_LIST_ID") ?: return@FoodShoppingTheme

                listenToShoppingListChanges(db, shoppingListId)

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
                        transferToPurchaseHistory(db, shoppingListId, userId)
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

            val products = snapshot?.get("products_list") as? List<Map<String, Any>> ?: listOf()
            val userIds = products.mapNotNull { it["addedBy"] as? String }.toSet()

            if (userIds.isNotEmpty()) {
                db.collection("User").whereIn(FieldPath.documentId(), userIds.toList()).get()
                    .addOnSuccessListener { userSnapshots ->
                        val userMap = userSnapshots.documents.associate {
                            it.id to (it.getString("username") ?: "Unknown")
                        }

                        val updatedList = products.map { product ->
                            val updatedProduct = product.toMutableMap()
                            updatedProduct["addedBy"] = userMap[product["addedBy"]] ?: "Unknown"
                            updatedProduct
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
                    val productData = mapOf(
                        "name" to productMap["name"],
                        "category" to productMap["category"],
                        "quantity" to productMap["quantity"],
                        "checked" to productMap["checked"],
                        "addedBy" to userId
                    )

                    val shoppingListRef = db.collection("ShoppingList").document(shoppingListId)

                    shoppingListRef.update("products_list", FieldValue.arrayRemove(productData))
                        .addOnSuccessListener {
                            Timber.tag("ShoppingList").d("Product removed from shopping list.")
                        }
                        .addOnFailureListener { e ->
                            Timber.tag("ShoppingList")
                                .e(e, "Failed to remove product from shopping list")
                        }
                } else {
                    Timber.tag("ShoppingList").e("Failed to find userId for username: $username")
                }
            }
            .addOnFailureListener { e ->
                Timber.tag("ShoppingList").e(e, "Failed to fetch userId for username: $username")
            }
    }

    private fun transferToPurchaseHistory(
        db: FirebaseFirestore,
        shoppingListId: String,
        userId: String?
    ) {
        val shoppingListRef = db.collection("ShoppingList").document(shoppingListId)
        val historyRef = db.collection("History")

        db.runTransaction { transaction ->
            val snapshot = transaction.get(shoppingListRef)
            val productsList = snapshot.get("products_list") as List<Map<String, Any>>
            val purchasedProducts = productsList.filter { it["checked"] == true }
            val remainingProducts = productsList.filter { it["checked"] != true }

            val shoppingListName = snapshot.getString("name") ?: "Unnamed List"

            val historyData = mapOf(
                "shoppingListName" to shoppingListName,
                "datePurchased" to com.google.firebase.Timestamp.now(),
                "products" to purchasedProducts,
                "userId" to userId
            )

            transaction.set(historyRef.document(), historyData)
            transaction.update(shoppingListRef, "products_list", remainingProducts)
        }.addOnSuccessListener {
            Timber.d("Purchase history updated successfully.")
        }.addOnFailureListener { e ->
            Timber.e(e, "Failed to transfer items to purchase history.")
        }
    }

    private fun listenToShoppingListChanges(db: FirebaseFirestore, shoppingListId: String) {
        val shoppingListRef = db.collection("ShoppingList").document(shoppingListId)

        var previousData: Map<String, Any>? = null
        var isInitialFetch = true

        shoppingListRef.addSnapshotListener { snapshot, error ->
            if (error != null) {
                Timber.tag("ShoppingListChanges")
                    .e(error, "Error listening to shopping list updates.")
                return@addSnapshotListener
            }

            if (snapshot != null && snapshot.exists()) {
                val afterData = snapshot.data

                if (isInitialFetch) {
                    previousData = afterData
                    isInitialFetch = false
                    Timber.tag("ShoppingListChanges").d("Initial data fetch ignored.")
                    return@addSnapshotListener
                }

                if (afterData != null) {
                    handleShoppingListChanges(previousData, afterData)
                    previousData = afterData
                }
            }
        }
    }

    private fun handleShoppingListChanges(
        beforeData: Map<String, Any>?,
        afterData: Map<String, Any>
    ) {
        val beforeProductList =
            beforeData?.get("products_list") as? List<Map<String, Any>> ?: emptyList()
        val afterProductList = afterData["products_list"] as? List<Map<String, Any>> ?: emptyList()
        val sharedWith = afterData["shared_with"] as? List<String> ?: emptyList()
        val currentUser = FirebaseAuth.getInstance().currentUser?.uid ?: return

        val db = FirebaseFirestore.getInstance()

        val userIds = afterProductList.mapNotNull { it["addedBy"] as? String }.toSet()

        if (userIds.isNotEmpty()) {
            db.collection("User").whereIn(FieldPath.documentId(), userIds.toList()).get()
                .addOnSuccessListener { userSnapshots ->
                    val userMap = userSnapshots.documents.associate {
                        it.id to (it.getString("username") ?: "Unknown")
                    }

                    db.collection("User").document(currentUser).get()
                        .addOnSuccessListener { document ->
                            val itemUpdatesEnabled =
                                document.getBoolean("notification_settings.item_updates") ?: false

                            if (!itemUpdatesEnabled) {
                                Timber.tag("ShoppingListChanges")
                                    .d("Item updates notifications are disabled. Changes are logged but no notifications sent.")
                                return@addOnSuccessListener
                            }

                            val addedProducts = afterProductList.filter { product ->
                                beforeProductList.none { it["name"] == product["name"] }
                            }

                            val removedProducts = beforeProductList.filter { product ->
                                afterProductList.none { it["name"] == product["name"] }
                            }

                            val markedAsBoughtProducts = afterProductList.filter { product ->
                                product["checked"] == true &&
                                        beforeProductList.any { it["name"] == product["name"] && it["checked"] != true }
                            }

                            val unmarkedAsBoughtProducts = afterProductList.filter { product ->
                                product["checked"] != true &&
                                        beforeProductList.any { it["name"] == product["name"] && it["checked"] == true }
                            }

                            addedProducts.forEach { product ->
                                val addedBy = product["addedBy"] as? String
                                val username = userMap[addedBy] ?: "Unknown"
                                sendNotificationToOthers(
                                    "A new product has been added to the shopping list by $username!",
                                    sharedWith,
                                    currentUser
                                )
                            }

                            removedProducts.forEach { product ->
                                val addedBy = product["addedBy"] as? String
                                val username = userMap[addedBy] ?: "Unknown"
                                sendNotificationToOthers(
                                    "A product has been removed from the shopping list by $username!",
                                    sharedWith,
                                    currentUser
                                )
                            }

                            markedAsBoughtProducts.forEach { product ->
                                val addedBy = product["addedBy"] as? String
                                val username = userMap[addedBy] ?: "Unknown"
                                sendNotificationToOthers(
                                    "A product has been marked as bought in the shopping list by $username!",
                                    sharedWith,
                                    currentUser
                                )
                            }

                            unmarkedAsBoughtProducts.forEach { product ->
                                val addedBy = product["addedBy"] as? String
                                val username = userMap[addedBy] ?: "Unknown"
                                sendNotificationToOthers(
                                    "A product has been unmarked as bought in the shopping list by $username!",
                                    sharedWith,
                                    currentUser
                                )
                            }
                        }
                        .addOnFailureListener { e ->
                            Timber.tag("ShoppingListChanges")
                                .e(e, "Failed to fetch user notification settings.")
                        }
                }
                .addOnFailureListener { e ->
                    Timber.tag("ShoppingListChanges")
                        .e(e, "Failed to fetch usernames for addedBy IDs.")
                }
        }
    }

    private fun sendNotificationToOthers(
        message: String,
        sharedWith: List<String>,
        currentUserId: String
    ) {
        val otherUsers = sharedWith.filter { it != currentUserId }

        if (otherUsers.isEmpty()) {
            Timber.tag("ShoppingListNotification").d("No other users to notify.")
            return
        }

        val db = FirebaseFirestore.getInstance()
        val auth = FirebaseAuth.getInstance()

        auth.currentUser?.let { user ->
            db.collection("User").document(user.uid).get()
                .addOnSuccessListener { document ->
                    val itemUpdatesEnabled =
                        document.getBoolean("notification_settings.item_updates") ?: false

                    if (!itemUpdatesEnabled) {
                        Timber.tag("ShoppingListNotification")
                            .d("Notifications disabled by user settings.")
                        return@addOnSuccessListener
                    }

                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                        if (checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                            Timber.tag("ShoppingListNotification")
                                .w("Notification permission not granted.")
                            return@addOnSuccessListener
                        }
                    }

                    val title = "Shopping List Update"
                    val notification = NotificationCompat.Builder(this, MainActivity.CHANNEL_ID)
                        .setSmallIcon(R.drawable.ic_notification)
                        .setContentTitle(title)
                        .setContentText(message)
                        .setPriority(NotificationCompat.PRIORITY_HIGH)
                        .build()

                    NotificationManagerCompat.from(this).notify(0, notification)
                    Timber.tag("ShoppingListNotification").d("Notification sent: $message")
                }
                .addOnFailureListener { e ->
                    Timber.tag("ShoppingListNotification")
                        .e(e, "Failed to fetch notification settings.")
                }
        }
    }
}