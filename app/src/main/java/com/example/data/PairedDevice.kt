package com.example.data

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.PropertyName
import com.google.firebase.firestore.ServerTimestamp

@IgnoreExtraProperties
data class PairedDevice(
    @DocumentId val id: String = "",
    val userId: String = "",
    val deviceName: String = "",
    val role: String = ROLE_CONTROLLER,
    val localIp: String = "",
    val port: Long = 38472L,
    val sessionToken: String = "",
    @get:PropertyName("isOnline")
    @field:PropertyName("isOnline")
    val isOnline: Boolean = false,
    val accessibilityReady: Boolean = false,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    companion object {
        const val ROLE_CONTROLLER = "CONTROLLER"
        const val ROLE_RECEIVER = "RECEIVER"
    }
}
