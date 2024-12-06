package com.example.foodshopping

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry
import com.github.mikephil.charting.highlight.Highlight
import com.github.mikephil.charting.listener.OnChartValueSelectedListener
import com.github.mikephil.charting.utils.ColorTemplate

@Composable
fun AnalyticsScreen(
    categoryData: Map<String, Int>,
    topCategories: List<Pair<String, Int>>,
    topCategoryProducts: Map<String, Map<String, Int>>,
    randomProduct: String
) {
    val pieEntries = categoryData.map { PieEntry(it.value.toFloat(), it.key) }
    val pieDataSet = PieDataSet(pieEntries, "Categories").apply {
        colors = ColorTemplate.COLORFUL_COLORS.toList()
        valueTextSize = 16f // Increased font size for values inside the chart
    }
    val pieData = PieData(pieDataSet)

    val selectedLabel = remember { mutableStateOf<String?>(null) }

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
                .padding(bottom = 56.dp),
            horizontalAlignment = Alignment.CenterHorizontally
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
                        style = MaterialTheme.typography.headlineMedium,
                        color = Color.Black,
                        modifier = Modifier.padding(bottom = 8.dp)
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
                            legend.isEnabled = false // Disable default legend

                            setOnChartValueSelectedListener(object : OnChartValueSelectedListener {
                                override fun onValueSelected(e: Entry?, h: Highlight?) {
                                    val pieEntry = e as? PieEntry
                                    selectedLabel.value = pieEntry?.label
                                }

                                override fun onNothingSelected() {
                                    selectedLabel.value = null
                                }
                            })
                        }
                    },
                    update = { pieChart ->
                        pieChart.data = pieData
                        pieChart.invalidate()
                    }
                )
                Spacer(modifier = Modifier.height(16.dp))
            }

            // Custom Legend for Main Pie Chart
            item {
                CustomLegend(
                    entries = categoryData.keys.toList(),
                    colors = ColorTemplate.COLORFUL_COLORS.toList(),
                    selectedLabel = selectedLabel.value
                )
            }

            // Top Categories with Detailed Graphs
            items(topCategories) { (category) ->
                val productsInCategory = topCategoryProducts[category] ?: emptyMap()
                DetailedCategoryGraph(
                    category = category,
                    products = productsInCategory,
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
                        text = "Random Product: $randomProduct",
                        style = MaterialTheme.typography.bodyLarge.copy(
                            fontSize = MaterialTheme.typography.titleLarge.fontSize),
                        color = Color.Black,
                        modifier = Modifier.padding(vertical = 16.dp) // Adjusted padding for balance
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
    rank: Int
) {
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
                color = Color.Black
            )
        }
        return
    }

    val aggregatedProducts = products.mapValues { it.value.toFloat() }
    val pieEntries = aggregatedProducts.map { PieEntry(it.value, it.key) }
    val pieDataSet = PieDataSet(pieEntries, "Products in $category").apply {
        colors = ColorTemplate.COLORFUL_COLORS.toList()
        valueTextSize = 16f // Larger numbers inside the graph
    }
    val pieData = PieData(pieDataSet)

    val selectedLabel = remember { mutableStateOf<String?>(null) }

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
                .height(200.dp),
            factory = { context ->
                PieChart(context).apply {
                    data = pieData
                    description.isEnabled = false
                    setUsePercentValues(true)
                    setDrawEntryLabels(false)
                    legend.isEnabled = false // Disable default legend

                    setOnChartValueSelectedListener(object : OnChartValueSelectedListener {
                        override fun onValueSelected(e: Entry?, h: Highlight?) {
                            val pieEntry = e as? PieEntry
                            selectedLabel.value = pieEntry?.label
                        }

                        override fun onNothingSelected() {
                            selectedLabel.value = null
                        }
                    })
                }
            },
            update = { pieChart ->
                pieChart.data = pieData
                pieChart.invalidate()
            }
        )

        // Custom Legend for Detailed Graph
        CustomLegend(
            entries = products.keys.toList(),
            colors = ColorTemplate.COLORFUL_COLORS.toList(),
            selectedLabel = selectedLabel.value
        )
    }
}

@Composable
fun CustomLegend(entries: List<String>, colors: List<Int>, selectedLabel: String?) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 16.dp),
        horizontalAlignment = Alignment.Start
    ) {
        entries.forEachIndexed { index, entry ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(vertical = 4.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(16.dp)
                        .background(Color(colors[index]))
                        .padding(end = 8.dp)
                )
                Text(
                    text = entry,
                    color = if (entry == selectedLabel) Color.Red else Color.Black,
                    style = if (entry == selectedLabel) MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold)
                    else MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(horizontal = 8.dp)
                )
            }
        }
    }
}
