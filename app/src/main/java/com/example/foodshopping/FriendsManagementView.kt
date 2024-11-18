package com.example.foodshopping

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp

@Composable
fun FriendsManagementView(
    searchText: String,
    onSearchTextChange: (String) -> Unit,
    searchResult: String?,
    onSearch: () -> Unit,
    onAddFriend: () -> Unit,
    friendsList: List<Friend>,
    onRemoveFriend: (Friend) -> Unit,
    duplicateFriendError: Boolean,
) {
    Box {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.linearGradient(
                        colors = listOf(
                            Color(0xFFFFCDD2),
                            Color(0xFFBBDEFB),
                            Color(0xFFFFF176)
                        )
                    )
                )
                .padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 64.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            item {
                Text(
                    text = "Add Friends by Email",
                    style = MaterialTheme.typography.headlineMedium,
                    color = Color.Black,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                OutlinedTextField(
                    value = searchText,
                    onValueChange = onSearchTextChange,
                    label = { Text("Enter Email", color = Color.Black) },
                    singleLine = true,
                    textStyle = TextStyle(color = Color.Black),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                )
                Button(
                    onClick = onSearch,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF445E91)),
                    modifier = Modifier.padding(8.dp)
                ) {
                    Text("Search", color = Color.White)
                }
            }
            if (duplicateFriendError) {
                item {
                    Text(
                        text = "Friend already in Friend List",
                        color = Color.Red,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                }
            }
            searchResult?.let {
                item {
                    Text(
                        text = it,
                        color = if (it.contains("No user") || it.contains("yourself")) Color.Red else Color.Black
                    )
                    if (!it.contains("No user") && !it.contains("yourself")) {
                        Button(
                            onClick = onAddFriend,
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF445E91)),
                            modifier = Modifier.padding(8.dp)
                        ) {
                            Text("Add to Friends List", color = Color.White)
                        }
                    }
                }
            }
            item {
                Text(
                    text = "Friends List",
                    style = MaterialTheme.typography.headlineSmall,
                    color = Color.Black,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }
            items(friendsList) { friend ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .background(Color.White, shape = MaterialTheme.shapes.medium)
                        .padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = friend.username,
                        modifier = Modifier.weight(1f),
                        color = Color.Black
                    )
                    IconButton(onClick = { onRemoveFriend(friend) }) {
                        Text("X", color = Color.Red)
                    }
                }
            }
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
        ) {
            BottomNavigationBar(currentScreen = "Friends Management")
        }
    }
}
