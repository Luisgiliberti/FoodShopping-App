package com.example.foodshopping

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp

@Composable
fun MenuScreenView(
    onAccountClick: () -> Unit,
    onPurchaseHistoryClick: () -> Unit,
    onAnalyticsClick: () -> Unit
) {
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
                .fillMaxWidth()
                .align(Alignment.TopCenter)
                .padding(top = 60.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Top,
        ) {
            Image(
                painter = painterResource(id = R.drawable.foodshoppinglogocircle),
                contentDescription = "Food Shopping Logo",
                modifier = Modifier
                    .size(100.dp)
                    .padding(bottom = 16.dp)
            )

            Button(
                onClick = onAccountClick,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF445E91)),
                modifier = Modifier
                    .fillMaxWidth(0.8f)
                    .padding(vertical = 8.dp)
                    .padding(top = 80.dp)
            ) {
                Text("Account", color = Color.White)
            }

            Button(
                onClick = onPurchaseHistoryClick,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF445E91)),
                modifier = Modifier
                    .fillMaxWidth(0.8f)
                    .padding(vertical = 8.dp)
            ) {
                Text("Purchase History", color = Color.White)
            }

            Button(
                onClick = onAnalyticsClick,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF445E91)),
                modifier = Modifier
                    .fillMaxWidth(0.8f)
                    .padding(vertical = 8.dp)
            ) {
                Text("Analytics", color = Color.White)
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
        ) {
            BottomNavigationBar(currentScreen = "Menu")
        }
    }
}
