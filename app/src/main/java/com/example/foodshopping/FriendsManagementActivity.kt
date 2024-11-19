package com.example.foodshopping

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.*
import com.example.foodshopping.ui.theme.FoodShoppingTheme
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

data class Friend(val email: String, val username: String)

class FriendsManagementActivity : ComponentActivity() {
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val currentUser = auth.currentUser
    private val currentUserEmail = currentUser?.email
    private var friendsListListener: ListenerRegistration? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            FoodShoppingTheme {
                var searchText by remember { mutableStateOf("") }
                var searchResult by remember { mutableStateOf<String?>(null) }
                var friendsList by remember { mutableStateOf(listOf<Friend>()) }
                var duplicateFriendError by remember { mutableStateOf(false) }
                val coroutineScope = rememberCoroutineScope()

                LaunchedEffect(currentUser) {
                    currentUser?.uid?.let { userId ->
                        setupFriendsListListener(userId, coroutineScope) { updatedList ->
                            friendsList = updatedList
                        }
                    }
                }

                DisposableEffect(Unit) {
                    onDispose {
                        friendsListListener?.remove()
                    }
                }

                FriendsManagementView(
                    searchText = searchText,
                    onSearchTextChange = { searchText = it },
                    searchResult = searchResult,
                    onSearch = {
                        coroutineScope.launch {
                            searchResult = searchEmail(searchText)
                        }
                    },
                    onAddFriend = {
                        if (friendsList.any { it.email == searchText }) {
                            duplicateFriendError = true
                        } else {
                            coroutineScope.launch {
                                addFriend(searchText) { newFriend ->
                                    friendsList = friendsList + newFriend
                                    searchText = ""
                                    searchResult = null
                                    duplicateFriendError = false
                                }
                            }
                        }
                    },
                    friendsList = friendsList,
                    onRemoveFriend = { friend ->
                        coroutineScope.launch {
                            removeFriend(friend) {
                                friendsList = friendsList.filter { it.email != friend.email }
                            }
                        }
                    },
                    duplicateFriendError = duplicateFriendError
                )
            }
        }
    }

    private fun setupFriendsListListener(
        userId: String,
        coroutineScope: CoroutineScope,
        onUpdate: (List<Friend>) -> Unit
    ) {
        friendsListListener = db.collection("User").document(userId)
            .addSnapshotListener { snapshot, _ ->
                if (snapshot != null && snapshot.exists()) {
                    val emailList = snapshot.get("friends_list") as? List<String> ?: listOf()
                    coroutineScope.launch {
                        val updatedList = emailList.map { email ->
                            Friend(email, fetchUsername(email))
                        }
                        onUpdate(updatedList)
                    }
                }
            }
    }

    private suspend fun fetchUsername(email: String): String {
        val userSnapshot = db.collection("User").whereEqualTo("email", email).get().await()
        return userSnapshot.documents.firstOrNull()?.getString("username") ?: email
    }

    private suspend fun searchEmail(email: String): String {
        if (email == currentUserEmail) return "You cannot add yourself to the friends list."
        val querySnapshot = db.collection("User").whereEqualTo("email", email).get().await()
        return if (querySnapshot.documents.isNotEmpty()) {
            val friendUsername = querySnapshot.documents.first().getString("username") ?: ""
            "User Found: $friendUsername"
        } else {
            "No user found with this email."
        }
    }

    private suspend fun addFriend(email: String, onComplete: (Friend) -> Unit) {
        val userId = currentUser?.uid ?: return

        val otherUserQuery = db.collection("User").whereEqualTo("email", email).get().await()
        if (otherUserQuery.documents.isNotEmpty()) {
            val otherUserDoc = otherUserQuery.documents.first()
            val otherUserId = otherUserDoc.id
            val otherUsername = otherUserDoc.getString("username") ?: ""
            val friend = Friend(email, otherUsername)

            val currentUsername = fetchUsername(currentUserEmail ?: "")
            updateFriendsList(userId, friend, add = true)
            updateFriendsList(otherUserId, Friend(currentUserEmail ?: "", currentUsername), add = true)

            onComplete(friend)
        }
    }

    private suspend fun removeFriend(friend: Friend, onComplete: () -> Unit) {
        val userId = currentUser?.uid ?: return

        updateFriendsList(userId, friend, add = false)
        val otherUserQuery = db.collection("User").whereEqualTo("email", friend.email).get().await()
        if (otherUserQuery.documents.isNotEmpty()) {
            val otherUserId = otherUserQuery.documents.first().id
            val currentUsername = fetchUsername(currentUserEmail ?: "")
            updateFriendsList(otherUserId, Friend(currentUserEmail ?: "", currentUsername), add = false)
        }
        onComplete()
    }

    private fun updateFriendsList(userId: String, friend: Friend, add: Boolean) {
        val userDocRef = db.collection("User").document(userId)
        if (add) {
            userDocRef.update("friends_list", FieldValue.arrayUnion(friend.email))
        } else {
            userDocRef.update("friends_list", FieldValue.arrayRemove(friend.email))
        }
    }
}
