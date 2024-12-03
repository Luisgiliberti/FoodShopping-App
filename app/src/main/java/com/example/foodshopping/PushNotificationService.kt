package com.example.foodshopping

import android.app.Notification
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import timber.log.Timber

class PushNotificationService : FirebaseMessagingService() {

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Timber.tag(TAG).d("New FCM Token: $token")

        // Save the token to Firestore under the user's document
        val userId = FirebaseAuth.getInstance().currentUser?.uid
        userId?.let {
            FirebaseFirestore.getInstance()
                .collection("User")
                .document(it)
                .update("fcmToken", token)
                .addOnSuccessListener { Timber.tag(TAG).d("Token updated in Firestore") }
                .addOnFailureListener { Timber.tag(TAG).w(it, "Failed to update token") }
        }
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)

        // Handle the FCM message payload
        remoteMessage.notification?.let { notification ->
            val title = notification.title ?: "Shopping List Update"
            val body = notification.body ?: "Your shopping list has been updated!"
            Timber.tag(TAG).d("Notification received - Title: $title, Body: $body")

            // Check for notification permission
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                if (checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                    Timber.tag(TAG).w("Notification permission not granted. Cannot show notification.")
                    return
                }
            }

            // Show the notification to the user
            NotificationManagerCompat.from(this).notify(0, createNotification(title, body))
        }
    }

    private fun createNotification(title: String, body: String): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(body)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()
    }

    companion object {
        private const val TAG = "PushNotificationService"
        private const val CHANNEL_ID = "shopping_list_updates"
    }
}
