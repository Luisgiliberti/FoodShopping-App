package com.example.foodshopping

import android.content.Context
import org.json.JSONArray

data class Product(val name: String, val category: String)

object ProductList {
    fun getProducts(context: Context): List<Product> {
        val ingredients = mutableListOf<Product>()
        try {
            // Open the JSON file from the assets folder
            val inputStream = context.assets.open("Products.json")
            val jsonString = inputStream.bufferedReader().use { it.readText() }
            val jsonArray = JSONArray(jsonString)

            // Parse JSON array into a list of Ingredient objects
            for (i in 0 until jsonArray.length()) {
                val jsonObject = jsonArray.getJSONObject(i)
                val name = jsonObject.getString("name")
                val category = jsonObject.getString("category")
                ingredients.add(Product(name, category))
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return ingredients
    }
}