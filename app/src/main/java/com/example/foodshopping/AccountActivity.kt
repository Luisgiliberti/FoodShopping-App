package com.example.foodshopping

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.example.foodshopping.ui.theme.FoodShoppingTheme
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class AccountActivity : ComponentActivity() {
    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        setContent {
            FoodShoppingTheme {
                AccountScreen(
                    onLogout = {
                        auth.signOut()
                        redirectToMainActivity()
                    },
                    onDeleteAccount = { password -> deleteUserAccount(password) },
                    onNavigateToUserList = { navigateToUserListManagement() },
                    onNavigateToSettings = { navigateToSettings() }
                )
            }
        }
    }

    private fun deleteUserAccount(password: String) {
        val user = auth.currentUser ?: return
        val userId = user.uid

        val credential = EmailAuthProvider.getCredential(user.email!!, password)
        user.reauthenticate(credential).addOnCompleteListener { reauthTask ->
            if (reauthTask.isSuccessful) {
                db.collection("User").document(userId).delete()
                    .addOnSuccessListener {
                        user.delete().addOnCompleteListener { deleteTask ->
                            if (deleteTask.isSuccessful) {
                                Toast.makeText(this, "Account deleted successfully", Toast.LENGTH_SHORT).show()
                                redirectToMainActivity()
                            } else {
                                Log.e("AccountActivity", "Account deletion failed", deleteTask.exception)
                                Toast.makeText(this, "Account deletion failed", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                    .addOnFailureListener { e ->
                        Log.e("AccountActivity", "Failed to delete user document", e)
                        Toast.makeText(this, "Failed to delete account data", Toast.LENGTH_SHORT).show()
                    }
            } else {
                Log.e("AccountActivity", "Reauthentication failed", reauthTask.exception)
                Toast.makeText(this, "Reauthentication failed: Incorrect or expired credentials", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun redirectToMainActivity() {
        val intent = Intent(this, MainActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        startActivity(intent)
        finish()
    }

    private fun navigateToUserListManagement() {
        val intent = Intent(this, FriendsManagementActivity::class.java)
        startActivity(intent)
        overridePendingTransition(0, 0)
    }

    private fun navigateToSettings() {
        val intent = Intent(this, SettingsActivity::class.java)
        startActivity(intent)
        overridePendingTransition(0, 0)
    }
}