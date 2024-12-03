package com.example.foodshopping

import android.graphics.Color as AndroidColor
import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
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
fun AnalyticsScreen(
    categoryData: Map<String, Int>,
    topCategories: List<Pair<String, Int>>,
    topCategoryProducts: Map<String, Map<String, Int>>,
    randomProduct: String
) {
    val context = LocalContext.current
    val pieData = remember(categoryData) { preparePieData(context, categoryData) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFFC8E6C9),
                        Color(0xFFBBDEFB),
                        Color(0xFFFFF176)
                    )
                )
            )
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
                .padding(bottom = 56.dp) // Leave space for the navigation bar
        ) {
            // Analytics Title
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Analytics",
                        style = MaterialTheme.typography.titleLarge
                    )
                }
            }

            // Main Pie Chart
            item {
                AndroidView(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(300.dp),
                    factory = { context ->
                        PieChart(context).apply {
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
            }

            // Top Categories with Detailed Graphs
            items(topCategories) { (category, count) ->
                val productsInCategory = topCategoryProducts[category] ?: emptyMap()
                DetailedCategoryGraph(
                    category = category,
                    products = productsInCategory,
                    context = context,
                    rank = topCategories.indexOfFirst { it.first == category } + 1
                )
                Spacer(modifier = Modifier.height(16.dp))
            }

            // Random Product of the Month
            item {
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Random Product of the Month: $randomProduct",
                        style = MaterialTheme.typography.bodyLarge,
                        color = Color.DarkGray,
                        modifier = Modifier.padding(vertical = 16.dp)
                    )
                }
            }
        }

        // Bottom Navigation Bar
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
        ) {
            BottomNavigationBar(currentScreen = "Analytics")
        }
    }
}

@Composable
fun DetailedCategoryGraph(
    category: String,
    products: Map<String, Int>,
    context: Context,
    rank: Int
) {
    // Handle Empty Products Case
    if (products.isEmpty()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Top $rank Category: $category",
                style = MaterialTheme.typography.bodyLarge,
                color = Color.Black
            )
            Text(
                text = "No product data available for this category.",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.Gray
            )
        }
        return
    }

    val aggregatedProducts = products.mapValues { it.value.toFloat() } // Aggregate product counts
    val pieData = PieData(PieDataSet(
        aggregatedProducts.map { (product, percentage) -> PieEntry(percentage, product) },
        "Products in $category"
    ).apply {
        colors = ColorTemplate.COLORFUL_COLORS.toList()
        valueTextColor = AndroidColor.BLACK
        valueTextSize = 12f
    })

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
    ) {
        Text(
            text = "Top $rank Category: $category",
            style = MaterialTheme.typography.bodyLarge,
            color = Color.Black
        )
        AndroidView(
            modifier = Modifier
                .fillMaxWidth()
                .height(150.dp),
            factory = {
                PieChart(it).apply {
                    data = pieData
                    description.isEnabled = false
                    legend.isEnabled = true
                    setDrawEntryLabels(false) // Disable entry labels to hide product names
                }
            },
            update = { pieChart ->
                pieChart.data = pieData
                pieChart.invalidate()
            }
        )
    }
}

fun preparePieData(context: Context, categoryData: Map<String, Int>): PieData {
    val aggregatedData = categoryData.map { (category, count) ->
        PieEntry(count.toFloat(), category) // Aggregate counts for each category
    }

    val dataSet = PieDataSet(aggregatedData, "Categories").apply {
        colors = ColorTemplate.COLORFUL_COLORS.toList()
        valueTextColor = AndroidColor.BLACK
        valueTextSize = 12f
    }

    return PieData(dataSet)
}
