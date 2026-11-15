package com.example.accessibility

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.content.ComponentName
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PixelFormat
import android.media.AudioManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.util.Log
import android.view.Gravity
import android.view.KeyEvent
import android.view.View
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import com.example.bluetooth.HidConstants
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.lang.ref.WeakReference
import kotlin.math.hypot
import kotlin.math.roundToInt

class RemoteAccessibilityService : AccessibilityService() {

    private val tag = "RemoteAccessibilitySvc"
    private val mainHandler = Handler(Looper.getMainLooper())

    private var windowManager: WindowManager? = null
    private var cursorView: PointerOverlayView? = null
    private var cursorParams: WindowManager.LayoutParams? = null

    private var screenWidth = 1920f
    private var screenHeight = 1200f
    private var cursorX = 960f
    private var cursorY = 600f

    private var dragActive = false
    private var dragStartX = 0f
    private var dragStartY = 0f

    private val hideCursorRunnable = Runnable {
        cursorView?.animate()?.alpha(0f)?.setDuration(420L)?.start()
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        instanceRef = WeakReference(this)
        _isServiceConnected.value = true
        setupCursorOverlay()
        Log.d(tag, "RemoteAccessibilityService connected & cursor overlay attached")
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // Focus & window tracking handled on demand when text/nav arrives
    }

    override fun onInterrupt() {
        // Keep idle cleanly
    }

    override fun onDestroy() {
        super.onDestroy()
        mainHandler.removeCallbacks(hideCursorRunnable)
        removeCursorOverlay()
        _isServiceConnected.value = false
        if (instanceRef?.get() === this) {
            instanceRef = null
        }
    }

    private fun updateScreenBounds() {
        val dm = resources.displayMetrics
        val wm = windowManager ?: (getSystemService(Context.WINDOW_SERVICE) as? WindowManager)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && wm != null) {
            val bounds = wm.currentWindowMetrics.bounds
            screenWidth = bounds.width().coerceAtLeast(100).toFloat()
            screenHeight = bounds.height().coerceAtLeast(100).toFloat()
        } else {
            screenWidth = dm.widthPixels.coerceAtLeast(100).toFloat()
            screenHeight = dm.heightPixels.coerceAtLeast(100).toFloat()
        }
    }

    private fun setupCursorOverlay() {
        mainHandler.post {
            try {
                val wm = getSystemService(Context.WINDOW_SERVICE) as? WindowManager ?: return@post
                windowManager = wm
                updateScreenBounds()
                cursorX = screenWidth / 2f
                cursorY = screenHeight / 2f

                val view = PointerOverlayView(this)
                view.alpha = 0f

                val params = WindowManager.LayoutParams(
                    view.cursorSizePx,
                    view.cursorSizePx,
                    WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
                    WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                        WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                        WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                        WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                    PixelFormat.TRANSLUCENT
                ).apply {
                    gravity = Gravity.TOP or Gravity.START
                    x = cursorX.roundToInt()
                    y = cursorY.roundToInt()
                }

                wm.addView(view, params)
                cursorView = view
                cursorParams = params
                wakeCursor()
            } catch (e: Exception) {
                Log.e(tag, "Failed to attach cursor overlay", e)
            }
        }
    }

    private fun removeCursorOverlay() {
        mainHandler.post {
            try {
                cursorView?.let { windowManager?.removeView(it) }
            } catch (_: Exception) {
            } finally {
                cursorView = null
                cursorParams = null
            }
        }
    }

    private fun wakeCursor() {
        val view = cursorView ?: return
        if (view.alpha < 0.99f) {
            view.animate().cancel()
            view.animate().alpha(1f).setDuration(110L).start()
        }
        mainHandler.removeCallbacks(hideCursorRunnable)
        mainHandler.postDelayed(hideCursorRunnable, 5500L)
    }

    fun moveCursor(dx: Float, dy: Float) {
        mainHandler.post {
            updateScreenBounds()
            cursorX = (cursorX + dx).coerceIn(0f, screenWidth - 2f)
            cursorY = (cursorY + dy).coerceIn(0f, screenHeight - 2f)

            val params = cursorParams
            val view = cursorView
            if (params != null && view != null) {
                params.x = cursorX.roundToInt()
                params.y = cursorY.roundToInt()
                try {
                    windowManager?.updateViewLayout(view, params)
                } catch (_: Exception) {}
            }
            wakeCursor()
        }
    }

    fun startMouseHold(button: String) {
        mainHandler.post {
            wakeCursor()
            if (button == "LEFT") {
                dragActive = true
                dragStartX = cursorX
                dragStartY = cursorY
                cursorView?.triggerClickPulse(holding = true)
            }
        }
    }

    fun endMouseHold(button: String) {
        mainHandler.post {
            wakeCursor()
            if (button == "LEFT" && dragActive) {
                dragActive = false
                cursorView?.triggerClickPulse(holding = false)
                val dist = hypot((cursorX - dragStartX).toDouble(), (cursorY - dragStartY).toDouble())
                if (dist > 14.0) {
                    dispatchSwipe(dragStartX, dragStartY, cursorX, cursorY, 260L)
                }
            }
        }
    }

    fun clickAtCursor(button: String) {
        mainHandler.post {
            wakeCursor()
            cursorView?.triggerClickPulse(holding = false)
            when (button) {
                "RIGHT" -> dispatchLongPress(cursorX, cursorY)
                "BUTTON_4" -> performGlobalAction(GLOBAL_ACTION_BACK)
                "BUTTON_5" -> performGlobalAction(GLOBAL_ACTION_RECENTS)
                else -> dispatchTap(cursorX, cursorY)
            }
        }
    }

    fun scrollAtCursor(verticalTicks: Int, horizontalTicks: Int) {
        mainHandler.post {
            wakeCursor()
            val cx = cursorX.coerceIn(80f, screenWidth - 80f)
            val cy = cursorY.coerceIn(140f, screenHeight - 140f)
            val dy = (verticalTicks * 95f).coerceIn(-450f, 450f)
            val dx = (horizontalTicks * 120f).coerceIn(-480f, 480f)
            if (dx != 0f || dy != 0f) {
                val endX = (cx + dx).coerceIn(20f, screenWidth - 20f)
                val endY = (cy + dy).coerceIn(20f, screenHeight - 20f)
                dispatchSwipe(cx, cy, endX, endY, 170L)
            }
        }
    }

    fun performNavAction(action: String) {
        mainHandler.post {
            when (action) {
                "BACK" -> performGlobalAction(GLOBAL_ACTION_BACK)
                "HOME" -> performGlobalAction(GLOBAL_ACTION_HOME)
                "RECENTS" -> performGlobalAction(GLOBAL_ACTION_RECENTS)
                "NOTIFICATIONS" -> performGlobalAction(GLOBAL_ACTION_NOTIFICATIONS)
            }
        }
    }

    fun handleConsumerUsage(usageCode: Int) {
        mainHandler.post {
            when (usageCode) {
                HidConstants.CONSUMER_HOME -> performGlobalAction(GLOBAL_ACTION_HOME)
                HidConstants.CONSUMER_BACK -> performGlobalAction(GLOBAL_ACTION_BACK)
                HidConstants.CONSUMER_AL_TASK_MANAGER -> performGlobalAction(GLOBAL_ACTION_RECENTS)
                HidConstants.CONSUMER_VOLUME_UP -> adjustVolume(AudioManager.ADJUST_RAISE)
                HidConstants.CONSUMER_VOLUME_DOWN -> adjustVolume(AudioManager.ADJUST_LOWER)
                HidConstants.CONSUMER_MUTE -> adjustVolume(AudioManager.ADJUST_TOGGLE_MUTE)
                HidConstants.CONSUMER_PLAY_PAUSE -> sendMediaKey(KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE)
                HidConstants.CONSUMER_NEXT_TRACK -> sendMediaKey(KeyEvent.KEYCODE_MEDIA_NEXT)
                HidConstants.CONSUMER_PREV_TRACK -> sendMediaKey(KeyEvent.KEYCODE_MEDIA_PREVIOUS)
            }
        }
    }

    fun handleText(text: String) {
        if (text.isEmpty()) return
        mainHandler.post {
            val focused = rootInActiveWindow?.findFocus(AccessibilityNodeInfo.FOCUS_INPUT) ?: return@post
            try {
                val currentText = focused.text?.toString()
                val hint = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) focused.hintText?.toString() else null
                val effective = if (currentText != null && currentText != hint) currentText else ""
                val updated = effective + text
                val args = Bundle().apply {
                    putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, updated)
                }
                focused.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, args)
            } catch (e: Exception) {
                Log.w(tag, "Text injection error", e)
            } finally {
                @Suppress("DEPRECATION")
                focused.recycle()
            }
        }
    }

    fun handleHidKey(hidCode: Int, modifier: Int) {
        mainHandler.post {
            when (hidCode) {
                HidConstants.KEY_BACKSPACE -> {
                    val focused = rootInActiveWindow?.findFocus(AccessibilityNodeInfo.FOCUS_INPUT)
                    if (focused != null) {
                        try {
                            val curr = focused.text?.toString().orEmpty()
                            if (curr.isNotEmpty()) {
                                val args = Bundle().apply {
                                    putCharSequence(
                                        AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE,
                                        curr.dropLast(1)
                                    )
                                }
                                focused.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, args)
                            }
                        } finally {
                            @Suppress("DEPRECATION")
                            focused.recycle()
                        }
                    }
                }
                HidConstants.KEY_ENTER -> {
                    val focused = rootInActiveWindow?.findFocus(AccessibilityNodeInfo.FOCUS_INPUT)
                    if (focused != null) {
                        try {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                                focused.performAction(AccessibilityNodeInfo.AccessibilityAction.ACTION_IME_ENTER.id)
                            }
                        } finally {
                            @Suppress("DEPRECATION")
                            focused.recycle()
                        }
                    }
                }
                HidConstants.KEY_ESC -> performGlobalAction(GLOBAL_ACTION_BACK)
                HidConstants.KEY_LEFT -> scrollAtCursor(0, 2)
                HidConstants.KEY_RIGHT -> scrollAtCursor(0, -2)
                HidConstants.KEY_UP -> scrollAtCursor(2, 0)
                HidConstants.KEY_DOWN -> scrollAtCursor(-2, 0)
                else -> {
                    val ch = hidKeyToSimpleChar(hidCode, (modifier and HidConstants.MOD_LSHIFT) != 0)
                    if (ch != null) {
                        handleText(ch)
                    }
                }
            }
        }
    }

    private fun adjustVolume(direction: Int) {
        val am = getSystemService(Context.AUDIO_SERVICE) as? AudioManager ?: return
        am.adjustStreamVolume(AudioManager.STREAM_MUSIC, direction, AudioManager.FLAG_SHOW_UI)
    }

    private fun sendMediaKey(keyCode: Int) {
        val am = getSystemService(Context.AUDIO_SERVICE) as? AudioManager ?: return
        am.dispatchMediaKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, keyCode))
        am.dispatchMediaKeyEvent(KeyEvent(KeyEvent.ACTION_UP, keyCode))
    }

    private fun dispatchTap(x: Float, y: Float) {
        val path = Path().apply { moveTo(x, y) }
        val stroke = GestureDescription.StrokeDescription(path, 0L, 40L)
        dispatchGesture(GestureDescription.Builder().addStroke(stroke).build(), null, null)
    }

    private fun dispatchLongPress(x: Float, y: Float) {
        val path = Path().apply { moveTo(x, y) }
        val stroke = GestureDescription.StrokeDescription(path, 0L, 620L)
        dispatchGesture(GestureDescription.Builder().addStroke(stroke).build(), null, null)
    }

    private fun dispatchSwipe(x1: Float, y1: Float, x2: Float, y2: Float, durationMs: Long) {
        val path = Path().apply {
            moveTo(x1, y1)
            lineTo(x2, y2)
        }
        val stroke = GestureDescription.StrokeDescription(path, 0L, durationMs.coerceAtLeast(60L))
        dispatchGesture(GestureDescription.Builder().addStroke(stroke).build(), null, null)
    }

    private fun hidKeyToSimpleChar(hidCode: Int, shift: Boolean): String? {
        if (hidCode in HidConstants.KEY_A..HidConstants.KEY_Z) {
            val base = ('a' + (hidCode - HidConstants.KEY_A))
            return if (shift) base.uppercase() else base.toString()
        }
        if (hidCode in HidConstants.KEY_1..HidConstants.KEY_9) {
            return ('1' + (hidCode - HidConstants.KEY_1)).toString()
        }
        return when (hidCode) {
            HidConstants.KEY_0 -> "0"
            HidConstants.KEY_SPACE -> " "
            else -> null
        }
    }

    private class PointerOverlayView(context: Context) : View(context) {
        private val density = context.resources.displayMetrics.density
        val cursorSizePx: Int = (38f * density).roundToInt()

        private var pulseRadius = 0f
        private var pulseAlpha = 0f
        private var isHolding = false

        private val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#FF2A4D")
            style = Paint.Style.FILL
        }
        private val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            style = Paint.Style.STROKE
            strokeWidth = 2.0f * density
            strokeJoin = Paint.Join.ROUND
        }
        private val pulsePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#FF4D6D")
            style = Paint.Style.STROKE
            strokeWidth = 2.2f * density
        }

        private val arrowPath = Path().apply {
            val s = density
            moveTo(2f * s, 2f * s)
            lineTo(2f * s, 25f * s)
            lineTo(8.5f * s, 19.5f * s)
            lineTo(13.5f * s, 29f * s)
            lineTo(17f * s, 27.3f * s)
            lineTo(12f * s, 17.8f * s)
            lineTo(20f * s, 17.8f * s)
            close()
        }

        fun triggerClickPulse(holding: Boolean) {
            isHolding = holding
            pulseRadius = 4f * density
            pulseAlpha = 230f
            invalidate()
        }

        override fun onDraw(canvas: Canvas) {
            super.onDraw(canvas)
            if (pulseAlpha > 4f || isHolding) {
                pulsePaint.alpha = if (isHolding) 200 else pulseAlpha.roundToInt().coerceIn(0, 255)
                val r = if (isHolding) 10f * density else pulseRadius
                canvas.drawCircle(3f * density, 3f * density, r, pulsePaint)
                if (!isHolding) {
                    pulseRadius += 1.6f * density
                    pulseAlpha *= 0.80f
                    postInvalidateOnAnimation()
                }
            }
            canvas.drawPath(arrowPath, fillPaint)
            canvas.drawPath(arrowPath, borderPaint)
        }
    }

    companion object {
        private var instanceRef: WeakReference<RemoteAccessibilityService>? = null

        private val _isServiceConnected = MutableStateFlow(false)
        val isServiceConnected: StateFlow<Boolean> = _isServiceConnected.asStateFlow()

        fun getInstance(): RemoteAccessibilityService? = instanceRef?.get()

        fun isAccessibilitySettingEnabled(context: Context): Boolean {
            val expected = ComponentName(context, RemoteAccessibilityService::class.java).flattenToString()
            val enabledServices = Settings.Secure.getString(
                context.contentResolver,
                Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
            ) ?: return false
            return enabledServices.split(':').any { it.equals(expected, ignoreCase = true) }
        }
    }
}
