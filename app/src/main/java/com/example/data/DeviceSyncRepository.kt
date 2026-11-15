package com.example.data

import android.content.Context
import com.example.R
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

class DeviceSyncRepository(
    private val db: FirebaseFirestore
) {
    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private fun devicesCollection(userId: String) =
        db.collection("users").document(userId).collection("devices")

    fun observeUserDevices(userId: String): Flow<List<PairedDevice>> {
        val path = "users/$userId/devices"
        return devicesCollection(userId)
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(
                    PairedDevice::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                )
            }
            .catch { error ->
                val firestoreError = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (firestoreError == null && error is CancellationException) throw error
                val currentUid = try {
                    Firebase.auth.currentUser?.uid
                } catch (_: Exception) {
                    null
                }
                val exceptionToLog = firestoreError ?: (error as? Exception)
                if (exceptionToLog != null && currentUid == userId) {
                    handleFirestoreError(exceptionToLog, OperationType.LIST, path)
                }
                throw error
            }
    }

    suspend fun getDevice(userId: String, deviceId: String): PairedDevice? {
        val docRef = devicesCollection(userId).document(deviceId)
        return try {
            docRef.get().await().toObject(
                PairedDevice::class.java,
                DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.GET, docRef.path)
            throw e
        }
    }

    suspend fun upsertDevice(
        userId: String,
        deviceId: String,
        deviceName: String,
        role: String,
        localIp: String,
        port: Int,
        sessionToken: String,
        isOnline: Boolean,
        accessibilityReady: Boolean
    ) {
        val docRef = devicesCollection(userId).document(deviceId)
        val safeName = deviceName.trim().ifBlank { "TabRemote Cihaz" }.take(60)
        val safeToken = if (sessionToken.length >= 6) sessionToken.take(128) else "tabremote_token"
        val safeRole = if (role == PairedDevice.ROLE_RECEIVER) {
            PairedDevice.ROLE_RECEIVER
        } else {
            PairedDevice.ROLE_CONTROLLER
        }

        val existing = try {
            docRef.get().await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.GET, docRef.path)
            throw e
        }

        if (existing.exists()) {
            val currentModel = existing.toObject(
                PairedDevice::class.java,
                DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
            )
            if (currentModel?.createdAt != null) {
                val updatedModel = currentModel.copy(
                    id = deviceId,
                    userId = userId,
                    deviceName = safeName,
                    role = safeRole,
                    localIp = localIp.take(64),
                    port = port.coerceIn(0, 65535).toLong(),
                    sessionToken = safeToken,
                    isOnline = isOnline,
                    accessibilityReady = accessibilityReady,
                    updatedAt = null
                )
                try {
                    docRef.set(updatedModel).await()
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    handleFirestoreError(e, OperationType.UPDATE, docRef.path)
                    throw e
                }
                return
            }
        }

        val createPayload = mapOf(
            "userId" to userId,
            "deviceName" to safeName,
            "role" to safeRole,
            "localIp" to localIp.take(64),
            "port" to port.coerceIn(0, 65535),
            "sessionToken" to safeToken,
            "isOnline" to isOnline,
            "accessibilityReady" to accessibilityReady,
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        try {
            docRef.set(createPayload).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            throw e
        }
    }

    suspend fun markOffline(userId: String, deviceId: String) {
        val docRef = devicesCollection(userId).document(deviceId)
        try {
            val snap = docRef.get().await()
            if (!snap.exists()) return
            docRef.update(
                mapOf(
                    "isOnline" to false,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            throw e
        }
    }

    suspend fun deleteDevice(userId: String, deviceId: String) {
        val docRef = devicesCollection(userId).document(deviceId)
        try {
            docRef.delete().await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docRef.path)
            throw e
        }
    }
}
