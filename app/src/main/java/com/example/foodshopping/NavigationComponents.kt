package com.example.foodshopping

import android.content.Context
import android.content.Intent
import androidx.activity.ComponentActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

@Composable
fun BottomNavigationBar(currentScreen: String) {
    val context = LocalContext.current

    val navigationItems = listOf(
        "Shopping List" to Icons.Default.ShoppingCart,
        "Favorites" to Icons.Default.Favorite,
        "Menu" to Icons.Default.Menu
    )

    NavigationBar(
        modifier = Modifier
            .fillMaxWidth()
            .height(70.dp)
            .background(Color.Black),
        containerColor = Color.Black
    ) {
        navigationItems.forEach { (item, icon) ->
            val isSelected = item == currentScreen
            val iconColor = if (isSelected) Color(0xFFFFFF77) else Color.LightGray
            val textColor = if (isSelected) Color(0xFFFFFF77) else Color.LightGray

            NavigationBarItem(
                label = { Text(item, color = textColor) },
                icon = {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = iconColor
                    )
                },
                selected = isSelected,
                onClick = {
                    navigateToScreenWithoutTransition(context, item)
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = iconColor,
                    selectedTextColor = textColor,
                    unselectedIconColor = Color.LightGray,
                    unselectedTextColor = Color.LightGray,
                    indicatorColor = Color.Transparent
                )
            )
        }
    }
}


fun navigateToScreenWithoutTransition(context: Context, screen: String) {
    when (screen) {
        "Shopping List" -> {
            context.startActivity(Intent(context, ShoppingListActivity::class.java))
            (context as? ComponentActivity)?.overridePendingTransition(0, 0)
        }
        "Favorites" -> {
            context.startActivity(Intent(context, FavoritesActivity::class.java))
            (context as? ComponentActivity)?.overridePendingTransition(0, 0)
        }
        "Menu" -> {
            context.startActivity(Intent(context, MenuActivity::class.java))
            (context as? ComponentActivity)?.overridePendingTransition(0, 0)
        }
    }
}

