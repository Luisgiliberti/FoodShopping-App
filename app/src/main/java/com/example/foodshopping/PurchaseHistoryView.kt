package com.example.foodshopping

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

@Composable
fun PurchaseHistoryScreenView() {
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
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.Center),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Welcome to Purchase History!",
                style = MaterialTheme.typography.headlineLarge,
                color = Color.Black
            )
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
        ) {
            BottomNavigationBar(currentScreen = "Purchase History")
        }
    }
}
