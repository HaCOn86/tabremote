package com.example.accessibility

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioManager
import android.media.MediaMetadata
import android.media.session.MediaController
import android.media.session.MediaSession
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import android.os.Build
import android.os.SystemClock
import android.provider.Settings
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.view.KeyEvent
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class MediaSessionSnapshot(
    val title: String = "",
    val artist: String = "",
    val appName: String = "",
    val packageName: String = "",
    val isPlaying: Boolean = false,
    val positionMs: Long = 0L,
    val durationMs: Long = 0L,
    val playbackSpeed: Float = 1f,
    val positionUpdateElapsedMs: Long = 0L,
    val updatedAtMs: Long = 0L
) {
    fun resolveCurrentPositionMs(): Long {
        val base = positionMs.coerceAtLeast(0L)
        if (!isPlaying || positionUpdateElapsedMs <= 0L) {
            return if (durationMs > 0L) base.coerceIn(0L, durationMs) else base
        }
        val delta = ((SystemClock.elapsedRealtime() - positionUpdateElapsedMs).coerceAtLeast(0L) *
            playbackSpeed.coerceIn(0.25f, 3f)).toLong()
        val live = base + delta
        return if (durationMs > 0L) live.coerceIn(0L, durationMs) else live
    }
}

class RemoteMediaNotificationService : NotificationListenerService() {

    private var sessionManager: MediaSessionManager? = null
    private var activeController: MediaController? = null

    private val controllerCallback = object : MediaController.Callback() {
        override fun onPlaybackStateChanged(state: PlaybackState?) {
            refreshFromSessions()
        }

        override fun onMetadataChanged(metadata: MediaMetadata?) {
            refreshFromSessions()
        }

        override fun onSessionDestroyed() {
            refreshFromSessions()
        }
    }

    private val sessionsListener = MediaSessionManager.OnActiveSessionsChangedListener { controllers ->
        bindAndEmit(controllers)
    }

    override fun onListenerConnected() {
        super.onListenerConnected()
        _isListenerConnected.value = true
        try {
            val mgr = getSystemService(Context.MEDIA_SESSION_SERVICE) as? MediaSessionManager
            sessionManager = mgr
            val component = ComponentName(this, RemoteMediaNotificationService::class.java)
            mgr?.addOnActiveSessionsChangedListener(sessionsListener, component)
            bindAndEmit(mgr?.getActiveSessions(component))
        } catch (_: SecurityException) {
            refreshFromActiveNotifications()
        } catch (_: Exception) {
        }
    }

    override fun onListenerDisconnected() {
        super.onListenerDisconnected()
        _isListenerConnected.value = false
        try {
            activeController?.unregisterCallback(controllerCallback)
        } catch (_: Exception) {}
        activeController = null
        try {
            sessionManager?.removeOnActiveSessionsChangedListener(sessionsListener)
        } catch (_: Exception) {}
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        refreshFromSessions()
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?) {
        refreshFromSessions()
    }

    private fun refreshFromSessions() {
        try {
            val component = ComponentName(this, RemoteMediaNotificationService::class.java)
            val list = sessionManager?.getActiveSessions(component)
            bindAndEmit(list)
        } catch (_: Exception) {
            refreshFromActiveNotifications()
        }
    }

    private fun bindAndEmit(controllers: List<MediaController>?) {
        if (controllers.isNullOrEmpty()) {
            try {
                activeController?.unregisterCallback(controllerCallback)
            } catch (_: Exception) {}
            activeController = null
            refreshFromActiveNotifications()
            return
        }

        val chosen = controllers.firstOrNull {
            it.playbackState?.state == PlaybackState.STATE_PLAYING
        } ?: controllers.firstOrNull {
            !it.metadata?.getString(MediaMetadata.METADATA_KEY_TITLE).isNullOrBlank()
        } ?: controllers.first()

        if (activeController?.sessionToken != chosen.sessionToken) {
            try {
                activeController?.unregisterCallback(controllerCallback)
            } catch (_: Exception) {}
            activeController = chosen
            try {
                chosen.registerCallback(controllerCallback)
            } catch (_: Exception) {}
        }

        val meta = chosen.metadata
        val pb = chosen.playbackState
        val title = meta?.getString(MediaMetadata.METADATA_KEY_TITLE).orEmpty()
        val artist = (meta?.getString(MediaMetadata.METADATA_KEY_ARTIST)
            ?: meta?.getString(MediaMetadata.METADATA_KEY_ALBUM_ARTIST)).orEmpty()
        val playing = pb?.state == PlaybackState.STATE_PLAYING
        val durationMs = meta?.getLong(MediaMetadata.METADATA_KEY_DURATION)?.coerceAtLeast(0L) ?: 0L
        val rawPos = pb?.position?.coerceAtLeast(0L) ?: 0L
        val speed = pb?.playbackSpeed?.takeIf { it > 0f } ?: 1f
        val posElapsed = pb?.lastPositionUpdateTime?.takeIf { it > 0L } ?: SystemClock.elapsedRealtime()
        val pkg = chosen.packageName.orEmpty()
        val appName = resolveAppLabel(this, pkg)

        if (title.isBlank() && !playing) {
            refreshFromActiveNotifications()
            return
        }

        _snapshot.value = MediaSessionSnapshot(
            title = title,
            artist = artist,
            appName = appName,
            packageName = pkg,
            isPlaying = playing,
            positionMs = rawPos,
            durationMs = durationMs,
            playbackSpeed = speed,
            positionUpdateElapsedMs = posElapsed,
            updatedAtMs = System.currentTimeMillis()
        )
    }

    private fun refreshFromActiveNotifications() {
        try {
            val notifications = activeNotifications.orEmpty()
            val mediaSbn = notifications.firstOrNull { sbn ->
                val extras = sbn.notification.extras
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    extras.getParcelable(android.app.Notification.EXTRA_MEDIA_SESSION, MediaSession.Token::class.java) != null
                } else {
                    @Suppress("DEPRECATION")
                    extras.getParcelable<MediaSession.Token>(android.app.Notification.EXTRA_MEDIA_SESSION) != null
                }
            }
            if (mediaSbn != null) {
                val extras = mediaSbn.notification.extras
                val title = extras.getCharSequence(android.app.Notification.EXTRA_TITLE)?.toString().orEmpty()
                val artist = extras.getCharSequence(android.app.Notification.EXTRA_TEXT)?.toString().orEmpty()
                val pkg = mediaSbn.packageName.orEmpty()
                _snapshot.value = MediaSessionSnapshot(
                    title = title,
                    artist = artist,
                    appName = resolveAppLabel(this, pkg),
                    packageName = pkg,
                    isPlaying = _snapshot.value.isPlaying,
                    positionMs = _snapshot.value.positionMs,
                    durationMs = _snapshot.value.durationMs,
                    playbackSpeed = _snapshot.value.playbackSpeed,
                    positionUpdateElapsedMs = _snapshot.value.positionUpdateElapsedMs,
                    updatedAtMs = System.currentTimeMillis()
                )
            } else {
                _snapshot.value = MediaSessionSnapshot(updatedAtMs = System.currentTimeMillis())
            }
        } catch (_: Exception) {
        }
    }

    companion object {
        private val _isListenerConnected = MutableStateFlow(false)
        val isListenerConnected: StateFlow<Boolean> = _isListenerConnected.asStateFlow()

        private val _snapshot = MutableStateFlow(MediaSessionSnapshot())
        val snapshot: StateFlow<MediaSessionSnapshot> = _snapshot.asStateFlow()

        fun isNotificationListenerEnabled(context: Context): Boolean {
            if (_isListenerConnected.value) return true
            val expected = ComponentName(context, RemoteMediaNotificationService::class.java).flattenToString()
            val flat = Settings.Secure.getString(
                context.contentResolver,
                "enabled_notification_listeners"
            ) ?: return false
            return flat.split(':').any { it.equals(expected, ignoreCase = true) }
        }

        fun dispatchMediaControl(context: Context, action: String) {
            val keyCode = when (action) {
                "NEXT" -> KeyEvent.KEYCODE_MEDIA_NEXT
                "PREV" -> KeyEvent.KEYCODE_MEDIA_PREVIOUS
                else -> KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE
            }
            val am = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager ?: return
            am.dispatchMediaKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, keyCode))
            am.dispatchMediaKeyEvent(KeyEvent(KeyEvent.ACTION_UP, keyCode))
        }

        fun resolveAppLabel(context: Context, packageName: String): String {
            if (packageName.isBlank()) return ""
            return try {
                val pm = context.packageManager
                val appInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    pm.getApplicationInfo(packageName, PackageManager.ApplicationInfoFlags.of(0))
                } else {
                    @Suppress("DEPRECATION")
                    pm.getApplicationInfo(packageName, 0)
                }
                pm.getApplicationLabel(appInfo)?.toString().orEmpty()
            } catch (_: Exception) {
                packageName.substringAfterLast('.').replaceFirstChar { it.uppercase() }
            }
        }
    }
}
