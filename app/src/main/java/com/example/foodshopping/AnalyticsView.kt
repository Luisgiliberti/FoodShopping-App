package com.example.foodshopping

import android.graphics.Color as AndroidColor
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

@Composable
fun AnalyticsScreen(
    categoryData: Map<String, Int>,
    topCategories: List<Pair<String, Int>>,
    topCategoryProducts: Map<String, Map<String, Int>>,
    randomProduct: String
) {
    val categoryColors = generateDistinctColors(categoryData.keys.size)
    val pieEntries = categoryData.entries.mapIndexed { index, (key, value) ->
        PieEntry(value.toFloat(), key) to categoryColors[index]
    }
    val pieDataSet = PieDataSet(pieEntries.map { it.first }, "Categories").apply {
        colors = pieEntries.map { it.second }
        valueTextSize = 16f
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
                        color = Color.Black
                    )
                }
            }

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
                            legend.isEnabled = false
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
                        pieChart.setOnChartValueSelectedListener(object : OnChartValueSelectedListener {
                            override fun onValueSelected(e: Entry?, h: Highlight?) {
                                val pieEntry = e as? PieEntry
                                selectedLabel.value = pieEntry?.label
                            }

                            override fun onNothingSelected() {
                                selectedLabel.value = null
                            }
                        })
                    }
                )
                Spacer(modifier = Modifier.height(16.dp))
            }

            item {
                CustomLegend(
                    entries = categoryData.keys.toList(),
                    colors = categoryColors,
                    selectedLabel = selectedLabel.value
                )
            }

            items(topCategories) { (category) ->
                val productsInCategory = topCategoryProducts[category] ?: emptyMap()
                DetailedCategoryGraph(
                    category = category,
                    products = productsInCategory,
                    rank = topCategories.indexOfFirst { it.first == category } + 1
                )
                Spacer(modifier = Modifier.height(16.dp))
            }

            item {
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Random Product: $randomProduct",
                        style = MaterialTheme.typography.bodyLarge.copy(
                            fontSize = MaterialTheme.typography.titleLarge.fontSize
                        ),
                        color = Color.Black
                    )
                }
            }
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
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

    val productColors = remember { generateDistinctColors(products.size) }
    val pieEntries = products.map { PieEntry(it.value.toFloat(), it.key) }
    val pieDataSet = PieDataSet(pieEntries, "Products in $category").apply {
        colors = productColors
        valueTextSize = 16f
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
                    legend.isEnabled = false

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
                if (pieChart.data != pieData) {
                    pieChart.data = pieData
                }
                pieChart.invalidate()
            }
        )

        CustomLegend(
            entries = products.keys.toList(),
            colors = productColors,
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

fun generateDistinctColors(count: Int): List<Int> {
    val colorPool = listOf(
        AndroidColor.rgb(115, 92, 176),
        AndroidColor.rgb(0, 164, 239),
        AndroidColor.rgb(106, 180, 62),
        AndroidColor.rgb(232, 157, 65),
        AndroidColor.rgb(253, 64, 132),
        AndroidColor.rgb(255, 99, 71),
        AndroidColor.rgb(0, 191, 255),
        AndroidColor.rgb(34, 139, 34),
        AndroidColor.rgb(255, 215, 0),
        AndroidColor.rgb(238, 130, 238),
        AndroidColor.rgb(220, 20, 60),
        AndroidColor.rgb(0, 128, 128),
        AndroidColor.rgb(210, 105, 30),
        AndroidColor.rgb(123, 104, 238),
        AndroidColor.rgb(72, 61, 139),
        AndroidColor.rgb(255, 20, 147),
        AndroidColor.rgb(127, 255, 0),
        AndroidColor.rgb(255, 165, 0),
        AndroidColor.rgb(139, 69, 19),
        AndroidColor.rgb(144, 238, 144),
        AndroidColor.rgb(240, 128, 128),
        AndroidColor.rgb(70, 130, 180),
        AndroidColor.rgb(152, 251, 152),
        AndroidColor.rgb(244, 164, 96),
        AndroidColor.rgb(250, 128, 114),
        AndroidColor.rgb(245, 222, 179),
        AndroidColor.rgb(64, 224, 208),
        AndroidColor.rgb(95, 158, 160),
        AndroidColor.rgb(32, 178, 170),
        AndroidColor.rgb(0, 206, 209),
        AndroidColor.rgb(72, 209, 204),
        AndroidColor.rgb(175, 238, 238),
        AndroidColor.rgb(127, 255, 212),
        AndroidColor.rgb(0, 255, 127),
        AndroidColor.rgb(124, 252, 0),
        AndroidColor.rgb(173, 255, 47),
        AndroidColor.rgb(250, 250, 210),
        AndroidColor.rgb(255, 239, 213),
        AndroidColor.rgb(255, 228, 181),
        AndroidColor.rgb(255, 218, 185),
        AndroidColor.rgb(255, 182, 193),
        AndroidColor.rgb(255, 105, 180),
        AndroidColor.rgb(255, 20, 147),
        AndroidColor.rgb(255, 160, 122),
        AndroidColor.rgb(255, 99, 71),
        AndroidColor.rgb(233, 150, 122),
        AndroidColor.rgb(250, 128, 114),
        AndroidColor.rgb(255, 69, 0),
        AndroidColor.rgb(255, 140, 0),
        AndroidColor.rgb(255, 165, 0),
        AndroidColor.rgb(255, 215, 0),
        AndroidColor.rgb(240, 230, 140),
        AndroidColor.rgb(189, 183, 107),
        AndroidColor.rgb(218, 165, 32),
        AndroidColor.rgb(184, 134, 11),
        AndroidColor.rgb(205, 133, 63),
        AndroidColor.rgb(139, 69, 19),
        AndroidColor.rgb(160, 82, 45),
        AndroidColor.rgb(210, 105, 30),
        AndroidColor.rgb(244, 164, 96),
        AndroidColor.rgb(222, 184, 135),
        AndroidColor.rgb(210, 180, 140),
        AndroidColor.rgb(188, 143, 143),
        AndroidColor.rgb(244, 164, 96),
        AndroidColor.rgb(205, 92, 92),
        AndroidColor.rgb(178, 34, 34),
        AndroidColor.rgb(139, 0, 0),
        AndroidColor.rgb(255, 248, 220),
        AndroidColor.rgb(255, 235, 205),
        AndroidColor.rgb(255, 222, 173),
        AndroidColor.rgb(245, 245, 220),
        AndroidColor.rgb(255, 228, 196),
        AndroidColor.rgb(255, 240, 245),
        AndroidColor.rgb(240, 255, 255),
        AndroidColor.rgb(240, 248, 255),
        AndroidColor.rgb(230, 230, 250),
        AndroidColor.rgb(176, 224, 230),
        AndroidColor.rgb(173, 216, 230),
        AndroidColor.rgb(135, 206, 250),
        AndroidColor.rgb(0, 191, 255),
        AndroidColor.rgb(135, 206, 235),
        AndroidColor.rgb(72, 61, 139),
        AndroidColor.rgb(123, 104, 238),
        AndroidColor.rgb(106, 90, 205),
        AndroidColor.rgb(147, 112, 219),
        AndroidColor.rgb(138, 43, 226),
        AndroidColor.rgb(148, 0, 211),
        AndroidColor.rgb(139, 0, 139),
        AndroidColor.rgb(153, 50, 204),
        AndroidColor.rgb(186, 85, 211),
        AndroidColor.rgb(255, 0, 255),
        AndroidColor.rgb(218, 112, 214),
        AndroidColor.rgb(255, 192, 203),
        AndroidColor.rgb(221, 160, 221)
    ).shuffled()

    return if (count <= colorPool.size) {
        colorPool.take(count)
    } else {
        throw IllegalArgumentException("Requested count exceeds the number of available colors.")
    }
}
