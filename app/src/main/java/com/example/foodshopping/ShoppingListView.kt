package com.example.foodshopping

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun ShoppingListView(
    shoppingLists: List<ShoppingList>,
    friendsList: List<String>,
    friendsShopping: List<String>,
    isShopping: Boolean,
    onShoppingStatusChange: (Boolean) -> Unit,
    onAddShoppingList: (String, List<String>) -> Unit,
    onRenameShoppingList: (String, String) -> Unit,
    onDeleteShoppingList: (String) -> Unit,
    onUpdateSharedUsers: (String, List<String>) -> Unit,
    onNavigateToProducts: (String) -> Unit
) {
    var showDialog by remember { mutableStateOf(false) }
    var shoppingListName by remember { mutableStateOf("") }
    var selectedFriends by remember { mutableStateOf(listOf<String>()) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.linearGradient(
                    colors = listOf(
                        Color(0xFFC8E6C9),
                        Color(0xFFBBDEFB),
                        Color(0xFFFFF176)
                    )
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = 56.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Are you shopping?",
                        fontSize = 18.sp,
                        color = Color.Black
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Switch(
                        checked = isShopping,
                        onCheckedChange = onShoppingStatusChange,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color(0xFF4CAF50),
                            uncheckedThumbColor = Color(0xFFBDBDBD)
                        )
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isShopping) "Yes" else "No",
                        fontSize = 16.sp,
                        color = if (isShopping) Color(0xFF4CAF50) else Color(0xFFBDBDBD)
                    )
                }

                FloatingActionButton(
                    onClick = { showDialog = true },
                    containerColor = Color(0xFF445E91),
                    contentColor = Color.White,
                    modifier = Modifier.size(48.dp)
                ) {
                    Text("+", fontSize = 20.sp)
                }
            }

            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(shoppingLists) { shoppingList ->
                    ShoppingListItem(
                        name = shoppingList.name,
                        shoppingListId = shoppingList.id,
                        creatorUsername = shoppingList.username,
                        isOwned = shoppingList.isOwned,
                        friendsList = friendsList,
                        sharedWith = shoppingList.sharedWith,
                        onRename = onRenameShoppingList,
                        onDelete = onDeleteShoppingList,
                        onUpdateSharedUsers = onUpdateSharedUsers,
                        onNavigateToProducts = onNavigateToProducts
                    )
                }
            }

            if (showDialog) {
                AddShoppingListDialog(
                    shoppingListName = shoppingListName,
                    onNameChange = { shoppingListName = it },
                    friendsList = friendsList,
                    selectedFriends = selectedFriends,
                    onFriendSelected = { friend ->
                        selectedFriends = if (selectedFriends.contains(friend)) {
                            selectedFriends - friend
                        } else {
                            selectedFriends + friend
                        }
                    },
                    onDismiss = {
                        showDialog = false
                        shoppingListName = ""
                        selectedFriends = emptyList()
                    },
                    onAddList = {
                        if (shoppingListName.isNotBlank()) {
                            onAddShoppingList(shoppingListName, selectedFriends)
                            showDialog = false
                            shoppingListName = ""
                            selectedFriends = emptyList()
                        }
                    }
                )
            }

            if (friendsShopping.isNotEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                ) {
                    Text(
                        text = "Friends Shopping Right Now:",
                        fontSize = 18.sp,
                        color = Color.Black,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                    friendsShopping.forEach { friend ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                        ) {
                            Text(
                                text = friend,
                                fontSize = 16.sp,
                                color = Color.Black
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Box(
                                modifier = Modifier
                                    .size(12.dp)
                                    .background(color = Color(0xFF4CAF50), shape = CircleShape)
                            )
                        }
                    }
                }
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
        ) {
            BottomNavigationBar(currentScreen = "Shopping List")
        }
    }
}

@Composable
fun ShoppingListItem(
    name: String,
    shoppingListId: String,
    creatorUsername: String,
    isOwned: Boolean,
    friendsList: List<String>,
    sharedWith: List<String>,
    onRename: (String, String) -> Unit,
    onDelete: (String) -> Unit,
    onUpdateSharedUsers: (String, List<String>) -> Unit,
    onNavigateToProducts: (String) -> Unit
) {
    var showOptionsDialog by remember { mutableStateOf(false) }
    var showSharedUsersDialog by remember { mutableStateOf(false) }
    var showRenameDialog by remember { mutableStateOf(false) }
    var showDeleteConfirmationDialog by remember { mutableStateOf(false) }
    var newName by remember { mutableStateOf(name) }
    var updatedSharedUsers by remember { mutableStateOf(sharedWith.toList()) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White, shape = MaterialTheme.shapes.medium)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
                .clickable { onNavigateToProducts(shoppingListId) },
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = name, fontSize = 18.sp, color = Color.Black)
                Text(text = "Created by: $creatorUsername", fontSize = 14.sp, color = Color.Gray)
            }
            if (isOwned) {
                IconButton(onClick = { showOptionsDialog = true }) {
                    Icon(Icons.Default.MoreVert, contentDescription = "Options")
                }
            }
        }

        if (showOptionsDialog) {
            AlertDialog(
                onDismissRequest = { showOptionsDialog = false },
                title = { Text("Options") },
                text = {
                    Column {
                        Text(
                            "Rename",
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    showOptionsDialog = false
                                    showRenameDialog = true
                                }
                                .padding(vertical = 8.dp)
                        )
                        Text(
                            "Manage Shared Users",
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    showOptionsDialog = false
                                    showSharedUsersDialog = true
                                }
                                .padding(vertical = 8.dp)
                        )
                        Text(
                            "Delete",
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    showOptionsDialog = false
                                    showDeleteConfirmationDialog = true
                                }
                                .padding(vertical = 8.dp)
                        )
                    }
                },
                confirmButton = {},
                dismissButton = {
                    Button(onClick = { showOptionsDialog = false }) {
                        Text("Cancel")
                    }
                }
            )
        }

        if (showDeleteConfirmationDialog) {
            AlertDialog(
                onDismissRequest = { showDeleteConfirmationDialog = false },
                title = { Text("Confirm Deletion") },
                text = { Text("Are you sure you want to delete this shopping list? This action cannot be undone.") },
                confirmButton = {
                    Button(onClick = {
                        onDelete(shoppingListId)
                        showDeleteConfirmationDialog = false
                    }) {
                        Text("Delete")
                    }
                },
                dismissButton = {
                    Button(onClick = { showDeleteConfirmationDialog = false }) {
                        Text("Cancel")
                    }
                }
            )
        }

        if (showRenameDialog) {
            AlertDialog(
                onDismissRequest = { showRenameDialog = false },
                title = { Text("Rename Shopping List") },
                text = {
                    OutlinedTextField(
                        value = newName,
                        onValueChange = { newName = it },
                        label = { Text("New Name") },
                        modifier = Modifier.fillMaxWidth()
                    )
                },
                confirmButton = {
                    Button(onClick = {
                        onRename(shoppingListId, newName)
                        showRenameDialog = false
                    }) {
                        Text("Rename")
                    }
                },
                dismissButton = {
                    Button(onClick = { showRenameDialog = false }) {
                        Text("Cancel")
                    }
                }
            )
        }

        if (showSharedUsersDialog) {
            AlertDialog(
                onDismissRequest = { showSharedUsersDialog = false },
                title = { Text("Manage Shared Users") },
                text = {
                    Column {
                        Text("Select friends to share this list with:")
                        Spacer(modifier = Modifier.height(8.dp))
                        LazyColumn {
                            items(friendsList) { friend ->
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            updatedSharedUsers = if (updatedSharedUsers.contains(friend)) {
                                                updatedSharedUsers - friend
                                            } else {
                                                updatedSharedUsers + friend
                                            }
                                        }
                                        .padding(vertical = 4.dp)
                                ) {
                                    Checkbox(
                                        checked = updatedSharedUsers.contains(friend),
                                        onCheckedChange = { isChecked ->
                                            updatedSharedUsers = if (isChecked) {
                                                updatedSharedUsers + friend
                                            } else {
                                                updatedSharedUsers - friend
                                            }
                                        }
                                    )
                                    Text(friend, modifier = Modifier.padding(start = 8.dp))
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    Button(onClick = {
                        onUpdateSharedUsers(shoppingListId, updatedSharedUsers)
                        showSharedUsersDialog = false
                    }) {
                        Text("Update")
                    }
                },
                dismissButton = {
                    Button(onClick = { showSharedUsersDialog = false }) {
                        Text("Cancel")
                    }
                }
            )
        }
    }
}

@Composable
fun AddShoppingListDialog(
    shoppingListName: String,
    onNameChange: (String) -> Unit,
    friendsList: List<String>,
    selectedFriends: List<String>,
    onFriendSelected: (String) -> Unit,
    onDismiss: () -> Unit,
    onAddList: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            Button(onClick = onAddList, enabled = shoppingListName.isNotBlank()) {
                Text("Add")
            }
        },
        dismissButton = {
            Button(onClick = onDismiss) {
                Text("Cancel")
            }
        },
        title = { Text("Create New Shopping List") },
        text = {
            Column {
                Text("List Name")
                OutlinedTextField(
                    value = shoppingListName,
                    onValueChange = onNameChange,
                    label = { Text("Name") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text("Share with Friends")
                LazyColumn {
                    items(friendsList) { friend ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onFriendSelected(friend) }
                                .padding(vertical = 4.dp)
                        ) {
                            Checkbox(
                                checked = selectedFriends.contains(friend),
                                onCheckedChange = { onFriendSelected(friend) }
                            )
                            Text(friend, modifier = Modifier.padding(start = 8.dp))
                        }
                    }
                }
            }
        }
    )
}
