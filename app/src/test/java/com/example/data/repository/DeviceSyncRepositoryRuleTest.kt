package com.example.data.repository

import com.example.base.FirestoreEmulatorTestBase
import com.example.data.DeviceSyncRepository
import com.example.data.PairedDevice
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.UUID

class DeviceSyncRepositoryRuleTest : FirestoreEmulatorTestBase() {

    @Test
    fun upsertDevice_authenticatedOwner_createsAndObservesPairedDevice() = runBlocking {
        val uid = signInTestUser(ALICE_EMAIL)
        val repository = DeviceSyncRepository(firestore)
        val deviceId = "dev_${UUID.randomUUID().toString().replace("-", "").take(12)}"

        withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.upsertDevice(
                userId = uid,
                deviceId = deviceId,
                deviceName = "Galaxy Tab S9",
                role = PairedDevice.ROLE_RECEIVER,
                localIp = "192.168.1.50",
                port = 38472,
                sessionToken = "secret_token_xyz",
                isOnline = true,
                accessibilityReady = true
            )
        }

        val emitted = withTimeout(FLOW_TIMEOUT_MS) {
            repository.observeUserDevices(uid).first { list -> list.any { it.id == deviceId } }
        }
        val fetched = emitted.firstOrNull { it.id == deviceId }
        assertNotNull(fetched)
        assertEquals("Galaxy Tab S9", fetched?.deviceName)
        assertEquals(PairedDevice.ROLE_RECEIVER, fetched?.role)
        assertEquals(true, fetched?.isOnline)
        assertEquals(true, fetched?.accessibilityReady)

        // Update existing document while preserving createdAt
        withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.upsertDevice(
                userId = uid,
                deviceId = deviceId,
                deviceName = "Galaxy Tab S9 Ultra",
                role = PairedDevice.ROLE_RECEIVER,
                localIp = "192.168.1.51",
                port = 38472,
                sessionToken = "secret_token_xyz",
                isOnline = false,
                accessibilityReady = true
            )
        }
        val updated = withTimeout(DEFAULT_TIMEOUT_MS) { repository.getDevice(uid, deviceId) }
        assertNotNull(updated)
        assertEquals("Galaxy Tab S9 Ultra", updated?.deviceName)
        assertEquals(false, updated?.isOnline)

        // Clean delete
        withTimeout(DEFAULT_TIMEOUT_MS) { repository.deleteDevice(uid, deviceId) }
    }

    @Test
    fun getDevice_crossUserAccess_failsWithPermissionDenied() = runBlocking {
        val aliceUid = signInTestUser(ALICE_EMAIL)
        val aliceRepo = DeviceSyncRepository(firestore)
        val deviceId = "dev_${UUID.randomUUID().toString().replace("-", "").take(12)}"

        withTimeout(DEFAULT_TIMEOUT_MS) {
            aliceRepo.upsertDevice(
                userId = aliceUid,
                deviceId = deviceId,
                deviceName = "Alice Tablet",
                role = PairedDevice.ROLE_RECEIVER,
                localIp = "10.0.0.5",
                port = 38472,
                sessionToken = "alice_token_123",
                isOnline = true,
                accessibilityReady = false
            )
        }

        signInTestUser(BOB_EMAIL)
        val bobRepo = DeviceSyncRepository(firestore)

        var caught: FirebaseFirestoreException? = null
        try {
            withTimeout(DEFAULT_TIMEOUT_MS) {
                bobRepo.getDevice(aliceUid, deviceId)
            }
        } catch (e: Exception) {
            caught = e.unwrapFirestoreException()
        }
        assertNotNull("Expected FirebaseFirestoreException for cross-user read", caught)
        assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, caught?.code)
    }

    @Test
    fun observeUserDevices_unauthenticatedUser_failsWithPermissionDenied() = runBlocking {
        val aliceUid = signInTestUser(ALICE_EMAIL)
        auth.signOut()
        val repository = DeviceSyncRepository(firestore)

        var caught: FirebaseFirestoreException? = null
        try {
            withTimeout(FLOW_TIMEOUT_MS) {
                repository.observeUserDevices(aliceUid).first()
            }
        } catch (e: Exception) {
            caught = e.unwrapFirestoreException()
        }
        assertNotNull("Expected nested FirebaseFirestoreException on unauthenticated Flow", caught)
        assertTrue(caught?.code == FirebaseFirestoreException.Code.PERMISSION_DENIED)
    }

    private companion object {
        const val ALICE_EMAIL = "alice_tabremote@test.com"
        const val BOB_EMAIL = "bob_tabremote@test.com"
        const val DEFAULT_TIMEOUT_MS = 5000L
        const val FLOW_TIMEOUT_MS = 3000L
    }
}
