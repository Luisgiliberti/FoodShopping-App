package com.example.foodshopping

import android.graphics.Color as AndroidColor
import android.content.Context
import android.widget.FrameLayout
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry
import com.github.mikephil.charting.utils.ColorTemplate

@Composable
fun AnalyticsScreen(categoryData: Map<String, Int>) {
    val context = LocalContext.current
    val pieData = preparePieData(context, categoryData)

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
        // Main content
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = 56.dp) // Leave space for the navigation bar
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                Text(
                    text = "Analytics: Purchases by Category",
                    style = MaterialTheme.typography.headlineMedium,
                    color = Color.Black,
                    modifier = Modifier
                        .padding(vertical = 16.dp)
                        .align(Alignment.CenterHorizontally)
                )

                AndroidView(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(300.dp)
                        .align(Alignment.CenterHorizontally),
                    factory = { context ->
                        PieChart(context).apply {
                            layoutParams = FrameLayout.LayoutParams(
                                FrameLayout.LayoutParams.MATCH_PARENT,
                                FrameLayout.LayoutParams.MATCH_PARENT
                            )
                            data = pieData
                            description.isEnabled = false
                            setUsePercentValues(true)
                            setDrawEntryLabels(false)
                            legend.isEnabled = true
                        }
                    },
                    update = { pieChart ->
                        pieChart.data = pieData
                        pieChart.invalidate()
                    }
                )

                Spacer(modifier = Modifier.height(16.dp))

                LazyColumn {
                    items(categoryData.toList()) { (category, count) ->
                        Text(
                            text = "$category: $count purchases",
                            style = MaterialTheme.typography.bodyLarge,
                            color = Color.Black,
                            modifier = Modifier.padding(vertical = 4.dp)
                        )
                    }
                }
            }
        }

        // Bottom navigation bar
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
        ) {
            BottomNavigationBar(currentScreen = "Analytics")
        }
    }
}

fun preparePieData(context: Context, categoryData: Map<String, Int>): PieData {
    val entries = categoryData.map { (category, count) ->
        PieEntry(count.toFloat(), category)
    }

    val dataSet = PieDataSet(entries, "Categories").apply {
        colors = ColorTemplate.COLORFUL_COLORS.toList()
        valueTextColor = AndroidColor.BLACK
        valueTextSize = 12f
    }

    return PieData(dataSet)
}

fun getColorForCategory(category: String): Color {
    return when (category) {
        "Fruit" -> Color(0xFFFFA726)
        "Pastry" -> Color(0xFF29B6F6)
        "Dairy" -> Color(0xFF66BB6A)
        "Vegetable" -> Color(0xFF81C784)
        "Meat" -> Color(0xFFEF5350)
        "Spice" -> Color(0xFFFF7043)
        "Grain" -> Color(0xFF8D6E63)
        "Sweetener" -> Color(0xFFFFC107)
        "Oil" -> Color(0xFFFFD54F)
        "Protein" -> Color(0xFF26C6DA)
        "Nuts" -> Color(0xFFAB47BC)
        "Herb" -> Color(0xFF8E24AA)
        "Legume" -> Color(0xFF26A69A)
        "Condiment" -> Color(0xFF78909C)
        "Fermented Food" -> Color(0xFFFFA000)
        "Spread" -> Color(0xFFFF8A65)
        "Medicine" -> Color(0xFFD32F2F)
        "Snack" -> Color(0xFF4DB6AC)
        "Seed" -> Color(0xFF4CAF50)
        "Cereal" -> Color(0xFFFFB74D)
        "Beverage" -> Color(0xFF9575CD)
        "Dairy Alternative" -> Color(0xFFBDBDBD)
        "Sauce" -> Color(0xFF9E9E9E)
        "Frozen Food" -> Color(0xFF42A5F5)
        "Home Supplies" -> Color(0xFF616161)
        "Personal Care" -> Color(0xFFBA68C8)
        "Flour" -> Color(0xFFF5F5F5)
        "Starch" -> Color(0xFFFFF176)
        "Alcoholic Beverage" -> Color(0xFFCE93D8)
        "Supplement" -> Color(0xFF90A4AE)
        "Confection" -> Color(0xFFFFC1E3)
        else -> Color(0xFFAB47BC)
    }
}