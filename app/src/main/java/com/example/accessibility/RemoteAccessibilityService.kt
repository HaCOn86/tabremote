package com.example.accessibility

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.content.ClipData
import android.content.ClipboardManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PixelFormat
import android.media.AudioManager
import android.os.BatteryManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.provider.Settings
import android.util.Log
import android.view.Display
import android.view.Gravity
import android.view.KeyEvent
import android.view.View
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import androidx.core.graphics.ColorUtils
import com.example.bluetooth.HidConstants
import com.example.model.CursorContrastMode
import com.example.model.ReceiverTelemetry
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import java.lang.ref.WeakReference
import kotlin.math.abs
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

    // Track last non-transient foreground app
    private var activeAppPackage: String = ""
    private var activeAppName: String = ""

    // Throttled luminance sampling for auto-contrast cursor (API 30+)
    private var lastSampleTimeMs = 0L
    private var isSamplingScreenshot = false
    private var lastSampledLuminance: Float? = null

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private val hideCursorRunnable = Runnable {
        cursorView?.animate()?.alpha(0f)?.setDuration(420L)?.start()
    }

    // Continuous multi-stroke gesture engines (API 26+)
    private var scrollChainActive = false
    private var scrollStrokeInFlight = false
    private var scrollEndRequested = false
    private var scrollStroke: GestureDescription.StrokeDescription? = null
    private var scrollAnchorX = 0f
    private var scrollAnchorY = 0f
    private var scrollPendingDx = 0f
    private var scrollPendingDy = 0f

    private var pinchChainActive = false
    private var pinchStrokeInFlight = false
    private var pinchEndRequested = false
    private var pinchStroke1: GestureDescription.StrokeDescription? = null
    private var pinchStroke2: GestureDescription.StrokeDescription? = null
    private var pinchCenterX = 0f
    private var pinchCenterY = 0f
    private var pinchRadius = 140f
    private var pinchPendingDelta = 0f

    private var holdChainActive = false
    private var holdStrokeInFlight = false
    private var holdEndRequested = false
    private var holdStroke: GestureDescription.StrokeDescription? = null
    private var holdLastX = 0f
    private var holdLastY = 0f

    private val scrollTimeoutRunnable = Runnable { endContinuousScroll() }
    private val pinchTimeoutRunnable = Runnable { endContinuousPinch() }

    override fun onServiceConnected() {
        super.onServiceConnected()
        instanceRef = WeakReference(this)
        _isServiceConnected.value = true
        setupCursorOverlay()
        serviceScope.launch {
            combine(
                cursorScaleFlow,
                cursorColorFlow,
                cursorBorderColorFlow,
                cursorContrastModeFlow
            ) { scale, fill, border, mode ->
                CursorAppearanceState(scale, fill, border, mode)
            }.collect { state ->
                applyCursorAppearance(state)
            }
        }
        Log.d(tag, "RemoteAccessibilityService connected & cursor overlay attached")
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return
        if (event.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            val pkg = event.packageName?.toString().orEmpty()
            if (pkg.isNotBlank() &&
                pkg != packageName &&
                !pkg.contains("inputmethod", ignoreCase = true)
            ) {
                if (pkg != activeAppPackage) {
                    activeAppPackage = pkg
                    activeAppName = RemoteMediaNotificationService.resolveAppLabel(this, pkg)
                }
            }
        }
    }

    override fun onInterrupt() {
        cancelAllContinuousChains()
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
        mainHandler.removeCallbacksAndMessages(null)
        cancelAllContinuousChains()
        removeCursorOverlay()
        _isServiceConnected.value = false
        if (instanceRef?.get() === this) {
            instanceRef = null
        }
    }

    private fun cancelAllContinuousChains() {
        scrollChainActive = false
        scrollStrokeInFlight = false
        scrollStroke = null
        pinchChainActive = false
        pinchStrokeInFlight = false
        pinchStroke1 = null
        pinchStroke2 = null
        holdChainActive = false
        holdStrokeInFlight = false
        holdStroke = null
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
                view.updateAppearance(
                    scale = cursorScaleFlow.value,
                    fillArgb = cursorColorFlow.value,
                    borderArgb = cursorBorderColorFlow.value,
                    contrastMode = cursorContrastModeFlow.value,
                    sampledLuminance = lastSampledLuminance
                )

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

    private fun applyCursorAppearance(state: CursorAppearanceState) {
        val view = cursorView ?: return
        val params = cursorParams ?: return
        view.updateAppearance(
            scale = state.scale,
            fillArgb = state.fillArgb,
            borderArgb = state.borderArgb,
            contrastMode = state.contrastMode,
            sampledLuminance = lastSampledLuminance
        )
        params.width = view.cursorSizePx
        params.height = view.cursorSizePx
        try {
            windowManager?.updateViewLayout(view, params)
        } catch (_: Exception) {}
        wakeCursor()
        if (state.contrastMode != CursorContrastMode.MANUAL) {
            maybeSampleBackgroundLuminance(force = true)
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
        if (cursorContrastModeFlow.value != CursorContrastMode.MANUAL) {
            maybeSampleBackgroundLuminance(force = false)
        }
    }

    private fun maybeSampleBackgroundLuminance(force: Boolean) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) return
        val mode = cursorContrastModeFlow.value
        if (mode == CursorContrastMode.MANUAL) return
        val now = SystemClock.uptimeMillis()
        if (isSamplingScreenshot) return
        if (!force && now - lastSampleTimeMs < 460L) return

        isSamplingScreenshot = true
        lastSampleTimeMs = now
        try {
            takeScreenshot(
                Display.DEFAULT_DISPLAY,
                mainExecutor,
                object : TakeScreenshotCallback {
                    override fun onSuccess(screenshot: ScreenshotResult) {
                        var hwBitmap: Bitmap? = null
                        var softBitmap: Bitmap? = null
                        try {
                            val hb = screenshot.hardwareBuffer
                            hwBitmap = Bitmap.wrapHardwareBuffer(hb, screenshot.colorSpace)
                            hb.close()
                            if (hwBitmap != null) {
                                // Downscale aggressively or copy small slice? Copying to small 32x32 scaled bitmap is super fast
                                softBitmap = hwBitmap.copy(Bitmap.Config.ARGB_8888, false)
                                if (softBitmap != null && softBitmap.width > 0 && softBitmap.height > 0) {
                                    val px = ((cursorX + 4f) / screenWidth * softBitmap.width)
                                        .roundToInt().coerceIn(0, softBitmap.width - 1)
                                    val py = ((cursorY + 4f) / screenHeight * softBitmap.height)
                                        .roundToInt().coerceIn(0, softBitmap.height - 1)
                                    var lumSum = 0.0
                                    var count = 0
                                    for (ox in -6..6 step 6) {
                                        for (oy in -6..6 step 6) {
                                            val sx = (px + ox).coerceIn(0, softBitmap.width - 1)
                                            val sy = (py + oy).coerceIn(0, softBitmap.height - 1)
                                            val pixel = softBitmap.getPixel(sx, sy)
                                            lumSum += ColorUtils.calculateLuminance(pixel)
                                            count++
                                        }
                                    }
                                    val avgLum = (lumSum / count.coerceAtLeast(1)).toFloat().coerceIn(0f, 1f)
                                    lastSampledLuminance = avgLum
                                    cursorView?.updateAppearance(
                                        scale = cursorScaleFlow.value,
                                        fillArgb = cursorColorFlow.value,
                                        borderArgb = cursorBorderColorFlow.value,
                                        contrastMode = cursorContrastModeFlow.value,
                                        sampledLuminance = avgLum
                                    )
                                }
                            }
                        } catch (_: Exception) {
                        } finally {
                            softBitmap?.recycle()
                            hwBitmap?.recycle()
                            isSamplingScreenshot = false
                        }
                    }

                    override fun onFailure(errorCode: Int) {
                        isSamplingScreenshot = false
                    }
                }
            )
        } catch (_: Exception) {
            isSamplingScreenshot = false
        }
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

    // ========================================================
    // Continuous Press-and-Hold & Double-Tap-Drag (API 26+)
    // ========================================================

    fun startMouseHold(button: String) {
        mainHandler.post {
            wakeCursor()
            if (button == "LEFT") {
                cursorView?.triggerClickPulse(holding = true)
                startLiveHoldChain()
            } else if (button == "RIGHT") {
                dispatchLongPress(cursorX, cursorY)
            }
        }
    }

    fun endMouseHold(button: String) {
        mainHandler.post {
            wakeCursor()
            if (button == "LEFT") {
                cursorView?.triggerClickPulse(holding = false)
                endLiveHoldChain()
            }
        }
    }

    fun handleHoldTouch(phase: String) {
        mainHandler.post {
            wakeCursor()
            when (phase) {
                "START" -> {
                    cursorView?.triggerClickPulse(holding = true)
                    startLiveHoldChain()
                }
                "END" -> {
                    cursorView?.triggerClickPulse(holding = false)
                    endLiveHoldChain()
                }
            }
        }
    }

    private fun startLiveHoldChain() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            holdLastX = cursorX
            holdLastY = cursorY
            holdChainActive = true
            return
        }
        if (holdChainActive) {
            holdEndRequested = false
            return
        }
        holdChainActive = true
        holdEndRequested = false
        holdLastX = cursorX.coerceIn(4f, screenWidth - 4f)
        holdLastY = cursorY.coerceIn(4f, screenHeight - 4f)

        val path = Path().apply { moveTo(holdLastX, holdLastY) }
        val stroke = GestureDescription.StrokeDescription(path, 0L, 110L, true)
        holdStroke = stroke
        holdStrokeInFlight = true
        dispatchGesture(
            GestureDescription.Builder().addStroke(stroke).build(),
            object : GestureResultCallback() {
                override fun onCompleted(gestureDescription: GestureDescription?) {
                    mainHandler.post { continueLiveHoldStep() }
                }

                override fun onCancelled(gestureDescription: GestureDescription?) {
                    mainHandler.post {
                        holdChainActive = false
                        holdStrokeInFlight = false
                        holdStroke = null
                    }
                }
            },
            mainHandler
        )
    }

    private fun continueLiveHoldStep() {
        if (!holdChainActive) {
            holdStrokeInFlight = false
            return
        }
        val prev = holdStroke
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O || prev == null) {
            holdChainActive = false
            holdStrokeInFlight = false
            return
        }
        val targetX = cursorX.coerceIn(4f, screenWidth - 4f)
        val targetY = cursorY.coerceIn(4f, screenHeight - 4f)
        val path = Path().apply {
            moveTo(holdLastX, holdLastY)
            if (hypot((targetX - holdLastX).toDouble(), (targetY - holdLastY).toDouble()) > 0.5) {
                lineTo(targetX, targetY)
            }
        }
        holdLastX = targetX
        holdLastY = targetY

        val finishNow = holdEndRequested
        val next = prev.continueStroke(path, 0L, if (finishNow) 24L else 110L, !finishNow)
        if (finishNow) {
            holdChainActive = false
            holdStrokeInFlight = false
            holdStroke = null
            dispatchGesture(GestureDescription.Builder().addStroke(next).build(), null, mainHandler)
            return
        }
        holdStroke = next
        dispatchGesture(
            GestureDescription.Builder().addStroke(next).build(),
            object : GestureResultCallback() {
                override fun onCompleted(gestureDescription: GestureDescription?) {
                    mainHandler.post { continueLiveHoldStep() }
                }

                override fun onCancelled(gestureDescription: GestureDescription?) {
                    mainHandler.post {
                        holdChainActive = false
                        holdStrokeInFlight = false
                        holdStroke = null
                    }
                }
            },
            mainHandler
        )
    }

    private fun endLiveHoldChain() {
        if (!holdChainActive) return
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            holdChainActive = false
            val dist = hypot((cursorX - holdLastX).toDouble(), (cursorY - holdLastY).toDouble())
            if (dist > 14.0) {
                dispatchSwipe(holdLastX, holdLastY, cursorX, cursorY, 240L)
            }
            return
        }
        holdEndRequested = true
    }

    fun clickAtCursor(button: String) {
        mainHandler.post {
            wakeCursor()
            updateScreenBounds()
            cursorView?.triggerClickPulse(holding = false)
            if (holdChainActive) {
                endLiveHoldChain()
            }
            val tapX = (cursorX + 4f).coerceIn(2f, screenWidth - 2f)
            val tapY = (cursorY + 4f).coerceIn(2f, screenHeight - 2f)
            when (button) {
                "RIGHT" -> dispatchLongPress(tapX, tapY)
                "BUTTON_4" -> performGlobalAction(GLOBAL_ACTION_BACK)
                "BUTTON_5" -> performGlobalAction(GLOBAL_ACTION_RECENTS)
                else -> dispatchTap(tapX, tapY)
            }
        }
    }

    // ========================================================
    // Smooth Continuous 2-Finger Scroll & Pinch-to-Zoom
    // ========================================================

    fun scrollAtCursor(verticalTicks: Int, horizontalTicks: Int) {
        val dy = (verticalTicks * 95f).coerceIn(-450f, 450f)
        val dx = (horizontalTicks * 120f).coerceIn(-480f, 480f)
        if (dx == 0f && dy == 0f) return
        handleContinuousScroll("MOVE", dx, dy)
    }

    fun handleContinuousScroll(phase: String, dx: Float, dy: Float) {
        mainHandler.post {
            wakeCursor()
            updateScreenBounds()
            mainHandler.removeCallbacks(scrollTimeoutRunnable)

            if (phase == "END") {
                endContinuousScroll()
                return@post
            }

            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
                val cx = cursorX.coerceIn(80f, screenWidth - 80f)
                val cy = cursorY.coerceIn(140f, screenHeight - 140f)
                dispatchSwipe(cx, cy, (cx + dx).coerceIn(20f, screenWidth - 20f), (cy + dy).coerceIn(20f, screenHeight - 20f), 110L)
                return@post
            }

            mainHandler.postDelayed(scrollTimeoutRunnable, 150L)

            if (!scrollChainActive) {
                scrollChainActive = true
                scrollEndRequested = false
                scrollAnchorX = cursorX.coerceIn(120f, screenWidth - 120f)
                scrollAnchorY = cursorY.coerceIn(180f, screenHeight - 180f)
                scrollPendingDx = 0f
                scrollPendingDy = 0f

                val nextX = (scrollAnchorX + dx).coerceIn(32f, screenWidth - 32f)
                val nextY = (scrollAnchorY + dy).coerceIn(32f, screenHeight - 32f)
                val path = Path().apply {
                    moveTo(scrollAnchorX, scrollAnchorY)
                    if (abs(nextX - scrollAnchorX) > 0.5f || abs(nextY - scrollAnchorY) > 0.5f) {
                        lineTo(nextX, nextY)
                    }
                }
                scrollAnchorX = nextX
                scrollAnchorY = nextY
                val stroke = GestureDescription.StrokeDescription(path, 0L, 34L, true)
                scrollStroke = stroke
                scrollStrokeInFlight = true
                dispatchGesture(
                    GestureDescription.Builder().addStroke(stroke).build(),
                    object : GestureResultCallback() {
                        override fun onCompleted(gestureDescription: GestureDescription?) {
                            mainHandler.post { continueScrollStep() }
                        }

                        override fun onCancelled(gestureDescription: GestureDescription?) {
                            mainHandler.post {
                                scrollChainActive = false
                                scrollStrokeInFlight = false
                                scrollStroke = null
                            }
                        }
                    },
                    mainHandler
                )
            } else {
                scrollEndRequested = false
                scrollPendingDx += dx
                scrollPendingDy += dy
            }
        }
    }

    private fun continueScrollStep() {
        if (!scrollChainActive || Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            scrollStrokeInFlight = false
            return
        }
        val prev = scrollStroke ?: run {
            scrollChainActive = false
            scrollStrokeInFlight = false
            return
        }

        val nearEdge = scrollAnchorY < 96f || scrollAnchorY > screenHeight - 96f ||
            scrollAnchorX < 64f || scrollAnchorX > screenWidth - 64f

        if (scrollEndRequested || nearEdge) {
            val endPath = Path().apply { moveTo(scrollAnchorX, scrollAnchorY) }
            val finalStroke = prev.continueStroke(endPath, 0L, 20L, false)
            scrollChainActive = false
            scrollStrokeInFlight = false
            scrollStroke = null
            dispatchGesture(GestureDescription.Builder().addStroke(finalStroke).build(), null, mainHandler)
            // If interrupted only by hitting screen edge while user still scrolling, re-anchor immediately
            if (nearEdge && !scrollEndRequested && (abs(scrollPendingDx) > 1f || abs(scrollPendingDy) > 1f)) {
                val carryX = scrollPendingDx
                val carryY = scrollPendingDy
                scrollPendingDx = 0f
                scrollPendingDy = 0f
                handleContinuousScroll("START", carryX, carryY)
            }
            return
        }

        val stepDx = scrollPendingDx
        val stepDy = scrollPendingDy
        scrollPendingDx = 0f
        scrollPendingDy = 0f

        val nextX = (scrollAnchorX + stepDx).coerceIn(32f, screenWidth - 32f)
        val nextY = (scrollAnchorY + stepDy).coerceIn(32f, screenHeight - 32f)
        val path = Path().apply {
            moveTo(scrollAnchorX, scrollAnchorY)
            if (abs(nextX - scrollAnchorX) > 0.5f || abs(nextY - scrollAnchorY) > 0.5f) {
                lineTo(nextX, nextY)
            }
        }
        scrollAnchorX = nextX
        scrollAnchorY = nextY
        val next = prev.continueStroke(path, 0L, 34L, true)
        scrollStroke = next
        dispatchGesture(
            GestureDescription.Builder().addStroke(next).build(),
            object : GestureResultCallback() {
                override fun onCompleted(gestureDescription: GestureDescription?) {
                    mainHandler.post { continueScrollStep() }
                }

                override fun onCancelled(gestureDescription: GestureDescription?) {
                    mainHandler.post {
                        scrollChainActive = false
                        scrollStrokeInFlight = false
                        scrollStroke = null
                    }
                }
            },
            mainHandler
        )
    }

    private fun endContinuousScroll() {
        mainHandler.removeCallbacks(scrollTimeoutRunnable)
        if (!scrollChainActive) return
        scrollEndRequested = true
    }

    fun handleContinuousPinch(phase: String, delta: Float) {
        mainHandler.post {
            wakeCursor()
            updateScreenBounds()
            mainHandler.removeCallbacks(pinchTimeoutRunnable)

            if (phase == "END") {
                endContinuousPinch()
                return@post
            }

            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return@post

            mainHandler.postDelayed(pinchTimeoutRunnable, 170L)

            val maxR = (minOf(screenWidth, screenHeight) * 0.40f).coerceAtLeast(140f)
            if (!pinchChainActive) {
                pinchChainActive = true
                pinchEndRequested = false
                pinchCenterX = cursorX.coerceIn(140f, screenWidth - 140f)
                pinchCenterY = cursorY.coerceIn(160f, screenHeight - 160f)
                // Start wider if zooming out (delta < 0), tighter if zooming in (delta >= 0)
                val initialR = if (delta < 0f) (maxR * 0.72f) else 96f
                pinchRadius = initialR
                pinchPendingDelta = 0f

                val nextR = (initialR + delta).coerceIn(32f, maxR)
                val p1 = Path().apply {
                    moveTo(pinchCenterX - initialR * 0.75f, pinchCenterY - initialR * 0.45f)
                    lineTo(pinchCenterX - nextR * 0.75f, pinchCenterY - nextR * 0.45f)
                }
                val p2 = Path().apply {
                    moveTo(pinchCenterX + initialR * 0.75f, pinchCenterY + initialR * 0.45f)
                    lineTo(pinchCenterX + nextR * 0.75f, pinchCenterY + nextR * 0.45f)
                }
                pinchRadius = nextR

                val s1 = GestureDescription.StrokeDescription(p1, 0L, 38L, true)
                val s2 = GestureDescription.StrokeDescription(p2, 0L, 38L, true)
                pinchStroke1 = s1
                pinchStroke2 = s2
                pinchStrokeInFlight = true
                dispatchGesture(
                    GestureDescription.Builder().addStroke(s1).addStroke(s2).build(),
                    object : GestureResultCallback() {
                        override fun onCompleted(gestureDescription: GestureDescription?) {
                            mainHandler.post { continuePinchStep(maxR) }
                        }

                        override fun onCancelled(gestureDescription: GestureDescription?) {
                            mainHandler.post {
                                pinchChainActive = false
                                pinchStrokeInFlight = false
                                pinchStroke1 = null
                                pinchStroke2 = null
                            }
                        }
                    },
                    mainHandler
                )
            } else {
                pinchEndRequested = false
                pinchPendingDelta += delta
            }
        }
    }

    private fun continuePinchStep(maxR: Float) {
        if (!pinchChainActive || Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            pinchStrokeInFlight = false
            return
        }
        val prev1 = pinchStroke1
        val prev2 = pinchStroke2
        if (prev1 == null || prev2 == null) {
            pinchChainActive = false
            pinchStrokeInFlight = false
            return
        }

        if (pinchEndRequested) {
            val end1 = Path().apply { moveTo(pinchCenterX - pinchRadius * 0.75f, pinchCenterY - pinchRadius * 0.45f) }
            val end2 = Path().apply { moveTo(pinchCenterX + pinchRadius * 0.75f, pinchCenterY + pinchRadius * 0.45f) }
            val fin1 = prev1.continueStroke(end1, 0L, 20L, false)
            val fin2 = prev2.continueStroke(end2, 0L, 20L, false)
            pinchChainActive = false
            pinchStrokeInFlight = false
            pinchStroke1 = null
            pinchStroke2 = null
            dispatchGesture(GestureDescription.Builder().addStroke(fin1).addStroke(fin2).build(), null, mainHandler)
            return
        }

        val stepDelta = pinchPendingDelta
        pinchPendingDelta = 0f
        val nextR = (pinchRadius + stepDelta).coerceIn(32f, maxR)

        val p1 = Path().apply {
            moveTo(pinchCenterX - pinchRadius * 0.75f, pinchCenterY - pinchRadius * 0.45f)
            lineTo(pinchCenterX - nextR * 0.75f, pinchCenterY - nextR * 0.45f)
        }
        val p2 = Path().apply {
            moveTo(pinchCenterX + pinchRadius * 0.75f, pinchCenterY + pinchRadius * 0.45f)
            lineTo(pinchCenterX + nextR * 0.75f, pinchCenterY + nextR * 0.45f)
        }
        pinchRadius = nextR
        val n1 = prev1.continueStroke(p1, 0L, 38L, true)
        val n2 = prev2.continueStroke(p2, 0L, 38L, true)
        pinchStroke1 = n1
        pinchStroke2 = n2
        dispatchGesture(
            GestureDescription.Builder().addStroke(n1).addStroke(n2).build(),
            object : GestureResultCallback() {
                override fun onCompleted(gestureDescription: GestureDescription?) {
                    mainHandler.post { continuePinchStep(maxR) }
                }

                override fun onCancelled(gestureDescription: GestureDescription?) {
                    mainHandler.post {
                        pinchChainActive = false
                        pinchStrokeInFlight = false
                        pinchStroke1 = null
                        pinchStroke2 = null
                    }
                }
            },
            mainHandler
        )
    }

    private fun endContinuousPinch() {
        mainHandler.removeCallbacks(pinchTimeoutRunnable)
        if (!pinchChainActive) return
        pinchEndRequested = true
    }

    // ========================================================
    // Navigation, System Panels, Volume & Media Actions
    // ========================================================

    fun performNavAction(action: String) {
        mainHandler.post {
            when (action) {
                "BACK" -> performGlobalAction(GLOBAL_ACTION_BACK)
                "HOME" -> performGlobalAction(GLOBAL_ACTION_HOME)
                "RECENTS" -> performGlobalAction(GLOBAL_ACTION_RECENTS)
                "NOTIFICATIONS" -> performGlobalAction(GLOBAL_ACTION_NOTIFICATIONS)
                "QUICK_SETTINGS" -> performGlobalAction(GLOBAL_ACTION_QUICK_SETTINGS)
                "SCREENSHOT" -> {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                        performGlobalAction(GLOBAL_ACTION_TAKE_SCREENSHOT)
                    }
                }
                "LOCK_SCREEN" -> {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                        performGlobalAction(GLOBAL_ACTION_LOCK_SCREEN)
                    }
                }
            }
        }
    }

    fun setVolumePercent(percent: Int) {
        mainHandler.post {
            val am = getSystemService(Context.AUDIO_SERVICE) as? AudioManager ?: return@post
            val max = am.getStreamMaxVolume(AudioManager.STREAM_MUSIC).coerceAtLeast(1)
            val target = ((percent.coerceIn(0, 100) / 100f) * max).roundToInt().coerceIn(0, max)
            am.setStreamVolume(AudioManager.STREAM_MUSIC, target, AudioManager.FLAG_SHOW_UI)
        }
    }

    fun handleConsumerUsage(usageCode: Int) {
        mainHandler.post {
            when (usageCode) {
                HidConstants.CONSUMER_HOME -> performGlobalAction(GLOBAL_ACTION_HOME)
                HidConstants.CONSUMER_BACK -> performGlobalAction(GLOBAL_ACTION_BACK)
                HidConstants.CONSUMER_AL_TASK_MANAGER -> performGlobalAction(GLOBAL_ACTION_RECENTS)
                HidConstants.CONSUMER_MENU -> performGlobalAction(GLOBAL_ACTION_QUICK_SETTINGS)
                HidConstants.CONSUMER_POWER -> {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                        performGlobalAction(GLOBAL_ACTION_LOCK_SCREEN)
                    }
                }
                HidConstants.CONSUMER_MENU_UP, HidConstants.CONSUMER_CH_UP -> scrollAtCursor(2, 0)
                HidConstants.CONSUMER_MENU_DOWN, HidConstants.CONSUMER_CH_DOWN -> scrollAtCursor(-2, 0)
                HidConstants.CONSUMER_MENU_LEFT -> scrollAtCursor(0, 2)
                HidConstants.CONSUMER_MENU_RIGHT -> scrollAtCursor(0, -2)
                HidConstants.CONSUMER_MENU_PICK -> clickAtCursor("LEFT")
                HidConstants.CONSUMER_VOLUME_UP -> adjustVolume(AudioManager.ADJUST_RAISE)
                HidConstants.CONSUMER_VOLUME_DOWN -> adjustVolume(AudioManager.ADJUST_LOWER)
                HidConstants.CONSUMER_MUTE -> adjustVolume(AudioManager.ADJUST_TOGGLE_MUTE)
                HidConstants.CONSUMER_PLAY_PAUSE -> sendMediaKey(KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE)
                HidConstants.CONSUMER_NEXT_TRACK -> sendMediaKey(KeyEvent.KEYCODE_MEDIA_NEXT)
                HidConstants.CONSUMER_PREV_TRACK -> sendMediaKey(KeyEvent.KEYCODE_MEDIA_PREVIOUS)
                HidConstants.CONSUMER_STOP -> sendMediaKey(KeyEvent.KEYCODE_MEDIA_STOP)
                HidConstants.CONSUMER_FAST_FORWARD -> sendMediaKey(KeyEvent.KEYCODE_MEDIA_FAST_FORWARD)
                HidConstants.CONSUMER_REWIND -> sendMediaKey(KeyEvent.KEYCODE_MEDIA_REWIND)
            }
        }
    }

    fun buildCurrentTelemetry(): ReceiverTelemetry {
        val am = getSystemService(Context.AUDIO_SERVICE) as? AudioManager
        val maxVol = am?.getStreamMaxVolume(AudioManager.STREAM_MUSIC)?.coerceAtLeast(1) ?: 15
        val curVol = am?.getStreamVolume(AudioManager.STREAM_MUSIC) ?: 7
        val volPct = ((curVol.toFloat() / maxVol) * 100f).roundToInt().coerceIn(0, 100)
        val isMuted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            am?.isStreamMute(AudioManager.STREAM_MUSIC) == true || curVol == 0
        } else {
            curVol == 0
        }
        val (batPct, isCharging) = readBatterySnapshot(this)
        val media = RemoteMediaNotificationService.snapshot.value
        val listenerEnabled = RemoteMediaNotificationService.isNotificationListenerEnabled(this)

        return ReceiverTelemetry(
            activeAppPackage = activeAppPackage,
            activeAppName = activeAppName,
            mediaTitle = media.title,
            mediaArtist = media.artist,
            mediaAppName = media.appName,
            isMediaPlaying = media.isPlaying,
            mediaPositionMs = media.resolveCurrentPositionMs(),
            mediaDurationMs = media.durationMs,
            volumePercent = volPct,
            isMuted = isMuted,
            batteryPercent = batPct,
            isBatteryCharging = isCharging,
            notificationListenerEnabled = listenerEnabled,
            updatedAtMs = System.currentTimeMillis()
        )
    }

    // ========================================================
    // Multi-Window Editable Node Search & Text Injection
    // ========================================================

    fun handleText(text: String) {
        if (text.isEmpty()) return
        mainHandler.post {
            val focused = findTargetEditableNode() ?: return@post
            try {
                injectTextIntoNode(focused, text)
            } catch (e: Exception) {
                Log.w(tag, "Text injection error", e)
            } finally {
                @Suppress("DEPRECATION")
                focused.recycle()
            }
        }
    }

    private fun injectTextIntoNode(node: AccessibilityNodeInfo, insertText: String) {
        val currentText = node.text?.toString()
        val hint = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) node.hintText?.toString() else null
        val effective = if (currentText != null && currentText != hint) currentText else ""

        val selStart = node.textSelectionStart
        val selEnd = node.textSelectionEnd
        val updated = if (selStart in 0..effective.length && selEnd in selStart..effective.length) {
            effective.substring(0, selStart) + insertText + effective.substring(selEnd)
        } else {
            effective + insertText
        }

        val args = Bundle().apply {
            putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, updated)
        }
        val applied = node.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, args)
        if (applied) {
            val newCursorPos = if (selStart in 0..effective.length) selStart + insertText.length else updated.length
            val selArgs = Bundle().apply {
                putInt(AccessibilityNodeInfo.ACTION_ARGUMENT_SELECTION_START_INT, newCursorPos)
                putInt(AccessibilityNodeInfo.ACTION_ARGUMENT_SELECTION_END_INT, newCursorPos)
            }
            node.performAction(AccessibilityNodeInfo.ACTION_SET_SELECTION, selArgs)
            return
        }

        // Clipboard + Paste fallback for WebViews / Chromium / custom renderers that ignore ACTION_SET_TEXT
        try {
            val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager ?: return
            val prevClip = clipboard.primaryClip
            clipboard.setPrimaryClip(ClipData.newPlainText("tabremote_input", insertText))
            val pasted = node.performAction(AccessibilityNodeInfo.ACTION_PASTE)
            if (pasted && prevClip != null) {
                mainHandler.postDelayed({
                    try {
                        clipboard.setPrimaryClip(prevClip)
                    } catch (_: Exception) {}
                }, 220L)
            }
        } catch (e: Exception) {
            Log.w(tag, "Clipboard paste fallback error", e)
        }
    }

    private fun findTargetEditableNode(): AccessibilityNodeInfo? {
        rootInActiveWindow?.findFocus(AccessibilityNodeInfo.FOCUS_INPUT)?.let { return it }

        val windowList = try {
            windows.orEmpty().sortedByDescending { win ->
                when {
                    win.isFocused -> 2
                    win.isActive -> 1
                    else -> 0
                }
            }
        } catch (_: Exception) {
            emptyList()
        }

        for (win in windowList) {
            val root = win.root ?: continue
            root.findFocus(AccessibilityNodeInfo.FOCUS_INPUT)?.let { focused ->
                @Suppress("DEPRECATION")
                root.recycle()
                return focused
            }
            val found = findEditableRecursive(root)
            if (found != null) {
                if (found !== root) {
                    @Suppress("DEPRECATION")
                    root.recycle()
                }
                return found
            }
            @Suppress("DEPRECATION")
            root.recycle()
        }

        val activeRoot = rootInActiveWindow ?: return null
        val fallback = findEditableRecursive(activeRoot)
        if (fallback !== activeRoot) {
            @Suppress("DEPRECATION")
            activeRoot.recycle()
        }
        return fallback
    }

    private fun findEditableRecursive(root: AccessibilityNodeInfo): AccessibilityNodeInfo? {
        val stack = ArrayDeque<AccessibilityNodeInfo>()
        stack.addLast(root)
        while (stack.isNotEmpty()) {
            val cur = stack.removeFirst()
            if (cur.isEditable && (cur.isFocused || cur.isFocusable)) {
                return cur
            }
            for (i in 0 until cur.childCount) {
                cur.getChild(i)?.let { stack.addLast(it) }
            }
            if (cur !== root) {
                @Suppress("DEPRECATION")
                cur.recycle()
            }
        }
        return null
    }

    fun handleHidKey(hidCode: Int, modifier: Int) {
        mainHandler.post {
            val ctrl = (modifier and (HidConstants.MOD_LCTRL or HidConstants.MOD_RCTRL)) != 0
            val shift = (modifier and (HidConstants.MOD_LSHIFT or HidConstants.MOD_RSHIFT)) != 0

            if (ctrl) {
                when (hidCode) {
                    HidConstants.KEY_EQUAL -> {
                        // Zoom in visually around cursor via real 2-finger spread
                        handleContinuousPinch("START", 48f)
                        mainHandler.postDelayed({ handleContinuousPinch("END", 0f) }, 80L)
                        return@post
                    }
                    HidConstants.KEY_MINUS -> {
                        handleContinuousPinch("START", -48f)
                        mainHandler.postDelayed({ handleContinuousPinch("END", 0f) }, 80L)
                        return@post
                    }
                    HidConstants.KEY_C -> {
                        findTargetEditableNode()?.let { node ->
                            node.performAction(AccessibilityNodeInfo.ACTION_COPY)
                            @Suppress("DEPRECATION")
                            node.recycle()
                        }
                        return@post
                    }
                    HidConstants.KEY_X -> {
                        findTargetEditableNode()?.let { node ->
                            node.performAction(AccessibilityNodeInfo.ACTION_CUT)
                            @Suppress("DEPRECATION")
                            node.recycle()
                        }
                        return@post
                    }
                    HidConstants.KEY_V -> {
                        findTargetEditableNode()?.let { node ->
                            node.performAction(AccessibilityNodeInfo.ACTION_PASTE)
                            @Suppress("DEPRECATION")
                            node.recycle()
                        }
                        return@post
                    }
                    HidConstants.KEY_A -> {
                        findTargetEditableNode()?.let { node ->
                            val len = node.text?.length ?: 0
                            if (len > 0) {
                                val args = Bundle().apply {
                                    putInt(AccessibilityNodeInfo.ACTION_ARGUMENT_SELECTION_START_INT, 0)
                                    putInt(AccessibilityNodeInfo.ACTION_ARGUMENT_SELECTION_END_INT, len)
                                }
                                node.performAction(AccessibilityNodeInfo.ACTION_SET_SELECTION, args)
                            }
                            @Suppress("DEPRECATION")
                            node.recycle()
                        }
                        return@post
                    }
                }
            }

            when (hidCode) {
                HidConstants.KEY_BACKSPACE -> {
                    val focused = findTargetEditableNode()
                    if (focused != null) {
                        try {
                            val curr = focused.text?.toString().orEmpty()
                            val hint = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) focused.hintText?.toString() else null
                            if (curr.isNotEmpty() && curr != hint) {
                                val selStart = focused.textSelectionStart
                                val selEnd = focused.textSelectionEnd
                                val updated = if (selStart in 0..curr.length && selEnd in selStart..curr.length) {
                                    if (selEnd > selStart) curr.removeRange(selStart, selEnd)
                                    else if (selStart > 0) curr.removeRange(selStart - 1, selStart)
                                    else curr
                                } else {
                                    curr.dropLast(1)
                                }
                                val args = Bundle().apply {
                                    putCharSequence(
                                        AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE,
                                        updated
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
                HidConstants.KEY_DELETE -> {
                    val focused = findTargetEditableNode()
                    if (focused != null) {
                        try {
                            val curr = focused.text?.toString().orEmpty()
                            val selStart = focused.textSelectionStart
                            val selEnd = focused.textSelectionEnd
                            if (curr.isNotEmpty() && selStart in 0..curr.length) {
                                val updated = when {
                                    selEnd > selStart -> curr.removeRange(selStart, selEnd)
                                    selStart < curr.length -> curr.removeRange(selStart, selStart + 1)
                                    else -> curr
                                }
                                val args = Bundle().apply {
                                    putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, updated)
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
                    val focused = findTargetEditableNode()
                    if (focused != null) {
                        try {
                            var sent = false
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                                sent = focused.performAction(AccessibilityNodeInfo.AccessibilityAction.ACTION_IME_ENTER.id)
                            }
                            if (!sent) {
                                injectTextIntoNode(focused, "\n")
                            }
                        } finally {
                            @Suppress("DEPRECATION")
                            focused.recycle()
                        }
                    }
                }
                HidConstants.KEY_TAB -> {
                    findTargetEditableNode()?.let { node ->
                        node.focusSearch(View.FOCUS_FORWARD)?.performAction(AccessibilityNodeInfo.ACTION_FOCUS)
                        @Suppress("DEPRECATION")
                        node.recycle()
                    }
                }
                HidConstants.KEY_HOME -> {
                    findTargetEditableNode()?.let { node ->
                        val args = Bundle().apply {
                            putInt(AccessibilityNodeInfo.ACTION_ARGUMENT_SELECTION_START_INT, 0)
                            putInt(AccessibilityNodeInfo.ACTION_ARGUMENT_SELECTION_END_INT, 0)
                        }
                        node.performAction(AccessibilityNodeInfo.ACTION_SET_SELECTION, args)
                        @Suppress("DEPRECATION")
                        node.recycle()
                    }
                }
                HidConstants.KEY_END -> {
                    findTargetEditableNode()?.let { node ->
                        val len = node.text?.length ?: 0
                        val args = Bundle().apply {
                            putInt(AccessibilityNodeInfo.ACTION_ARGUMENT_SELECTION_START_INT, len)
                            putInt(AccessibilityNodeInfo.ACTION_ARGUMENT_SELECTION_END_INT, len)
                        }
                        node.performAction(AccessibilityNodeInfo.ACTION_SET_SELECTION, args)
                        @Suppress("DEPRECATION")
                        node.recycle()
                    }
                }
                HidConstants.KEY_ESC -> performGlobalAction(GLOBAL_ACTION_BACK)
                HidConstants.KEY_MENU -> performGlobalAction(GLOBAL_ACTION_QUICK_SETTINGS)
                HidConstants.KEY_POWER -> {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                        performGlobalAction(GLOBAL_ACTION_LOCK_SCREEN)
                    }
                }
                HidConstants.KEY_LEFT -> scrollAtCursor(0, 2)
                HidConstants.KEY_RIGHT -> scrollAtCursor(0, -2)
                HidConstants.KEY_UP -> scrollAtCursor(2, 0)
                HidConstants.KEY_DOWN -> scrollAtCursor(-2, 0)
                else -> {
                    val ch = hidKeyToChar(hidCode, shift)
                    if (ch != null) {
                        handleText(ch)
                    }
                }
            }
        }
    }

    private var lastGamepadButtonsMask = 0
    private var lastGamepadHat = 8

    fun handleGamepadInput(
        buttonsMask: Int,
        hat: Int,
        lx: Int,
        ly: Int,
        rx: Int,
        ry: Int
    ) {
        mainHandler.post {
            wakeCursor()
            // Right analog stick controls pointer smoothly; fallback to left stick if right is centered
            val moveX = if (abs(rx) > 12) rx else if (abs(lx) > 20) lx else 0
            val moveY = if (abs(ry) > 12) ry else if (abs(ly) > 20) ly else 0
            if (moveX != 0 || moveY != 0) {
                moveCursor(moveX * 0.24f, moveY * 0.24f)
            }

            if (hat != lastGamepadHat) {
                lastGamepadHat = hat
                when (hat) {
                    0 -> scrollAtCursor(2, 0) // Up
                    2 -> scrollAtCursor(0, -2) // Right
                    4 -> scrollAtCursor(-2, 0) // Down
                    6 -> scrollAtCursor(0, 2) // Left
                }
            }

            val pressed = buttonsMask and lastGamepadButtonsMask.inv()
            val released = lastGamepadButtonsMask and buttonsMask.inv()
            lastGamepadButtonsMask = buttonsMask

            if ((pressed and (1 shl 0)) != 0) clickAtCursor("LEFT") // A / Cross
            if ((pressed and (1 shl 1)) != 0) performGlobalAction(GLOBAL_ACTION_BACK) // B / Circle
            if ((pressed and (1 shl 2)) != 0) clickAtCursor("RIGHT") // X / Square
            if ((pressed and (1 shl 3)) != 0) sendMediaKey(KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE) // Y / Triangle
            if ((pressed and (1 shl 4)) != 0) adjustVolume(AudioManager.ADJUST_LOWER) // L1
            if ((pressed and (1 shl 5)) != 0) adjustVolume(AudioManager.ADJUST_RAISE) // R1
            if ((pressed and (1 shl 8)) != 0) performGlobalAction(GLOBAL_ACTION_RECENTS) // Select / Share
            if ((pressed and (1 shl 9)) != 0) performGlobalAction(GLOBAL_ACTION_HOME) // Start / Options
            if ((released and (1 shl 0)) != 0 && holdChainActive) endLiveHoldChain()
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
        val stroke = GestureDescription.StrokeDescription(path, 0L, 55L)
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
        val stroke = GestureDescription.StrokeDescription(path, 0L, durationMs.coerceAtLeast(45L))
        dispatchGesture(GestureDescription.Builder().addStroke(stroke).build(), null, null)
    }

    private fun hidKeyToChar(hidCode: Int, shift: Boolean): String? {
        if (hidCode in HidConstants.KEY_A..HidConstants.KEY_Z) {
            val base = ('a' + (hidCode - HidConstants.KEY_A))
            return if (shift) base.uppercase() else base.toString()
        }
        if (hidCode in HidConstants.KEY_1..HidConstants.KEY_9) {
            val num = ('1' + (hidCode - HidConstants.KEY_1))
            if (!shift) return num.toString()
            return when (hidCode) {
                HidConstants.KEY_1 -> "!"
                HidConstants.KEY_2 -> "'"
                HidConstants.KEY_3 -> "^"
                HidConstants.KEY_4 -> "+"
                HidConstants.KEY_5 -> "%"
                HidConstants.KEY_6 -> "&"
                HidConstants.KEY_7 -> "/"
                HidConstants.KEY_8 -> "("
                HidConstants.KEY_9 -> ")"
                else -> num.toString()
            }
        }
        return when (hidCode) {
            HidConstants.KEY_0 -> if (shift) "=" else "0"
            HidConstants.KEY_SPACE -> " "
            HidConstants.KEY_MINUS -> if (shift) "_" else "-"
            HidConstants.KEY_EQUAL -> if (shift) "+" else "="
            HidConstants.KEY_LEFTBRACE -> if (shift) "Ğ" else "ğ"
            HidConstants.KEY_RIGHTBRACE -> if (shift) "Ü" else "ü"
            HidConstants.KEY_SEMICOLON -> if (shift) "Ş" else "ş"
            HidConstants.KEY_APOSTROPHE -> if (shift) "İ" else "i"
            HidConstants.KEY_BACKSLASH -> if (shift) ";" else ","
            HidConstants.KEY_COMMA -> if (shift) "Ö" else "ö"
            HidConstants.KEY_DOT -> if (shift) "Ç" else "ç"
            HidConstants.KEY_SLASH -> if (shift) ":" else "."
            else -> null
        }
    }

    private data class CursorAppearanceState(
        val scale: Float,
        val fillArgb: Int,
        val borderArgb: Int,
        val contrastMode: CursorContrastMode
    )

    private class PointerOverlayView(context: Context) : View(context) {
        private val density = context.resources.displayMetrics.density
        private var currentScale = 1.0f
        private var currentFillArgb = 0xFFFF2A4D.toInt()
        private var currentBorderArgb = Color.WHITE

        var cursorSizePx: Int = (36f * density).roundToInt()
            private set

        private var pulseRadius = 0f
        private var pulseAlpha = 0f
        private var isHolding = false

        private val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = currentFillArgb
            style = Paint.Style.FILL
        }
        private val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            style = Paint.Style.STROKE
            strokeWidth = 1.8f * density
            strokeJoin = Paint.Join.ROUND
            strokeCap = Paint.Cap.ROUND
        }
        private val pulsePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = currentFillArgb
            style = Paint.Style.STROKE
            strokeWidth = 2.0f * density
        }

        private val arrowPath = Path()

        init {
            rebuildPath()
        }

        fun updateAppearance(
            scale: Float,
            fillArgb: Int,
            borderArgb: Int,
            contrastMode: CursorContrastMode,
            sampledLuminance: Float?
        ) {
            currentScale = scale.coerceIn(0.6f, 2.2f)
            cursorSizePx = (36f * density * currentScale).roundToInt().coerceAtLeast(24)

            val resolvedPair = resolveEffectiveCursorColors(
                manualFillArgb = fillArgb,
                manualBorderArgb = borderArgb,
                mode = contrastMode,
                backgroundLuminance = sampledLuminance
            )
            currentFillArgb = resolvedPair.first
            currentBorderArgb = resolvedPair.second

            fillPaint.color = currentFillArgb
            pulsePaint.color = currentFillArgb
            borderPaint.color = currentBorderArgb
            borderPaint.strokeWidth = 1.7f * density * currentScale.coerceIn(0.85f, 1.35f)

            rebuildPath()
            invalidate()
        }

        private fun rebuildPath() {
            val s = density * currentScale
            arrowPath.reset()
            arrowPath.moveTo(2.0f * s, 2.0f * s)
            arrowPath.lineTo(2.0f * s, 20.5f * s)
            arrowPath.quadTo(2.0f * s, 22.0f * s, 3.6f * s, 21.1f * s)
            arrowPath.lineTo(8.2f * s, 17.8f * s)
            arrowPath.lineTo(14.8f * s, 16.5f * s)
            arrowPath.quadTo(16.6f * s, 16.1f * s, 15.3f * s, 14.8f * s)
            arrowPath.close()
        }

        fun triggerClickPulse(holding: Boolean) {
            isHolding = holding
            pulseRadius = 4f * density * currentScale
            pulseAlpha = 230f
            invalidate()
        }

        override fun onDraw(canvas: Canvas) {
            super.onDraw(canvas)
            val s = density * currentScale
            if (pulseAlpha > 4f || isHolding) {
                pulsePaint.alpha = if (isHolding) 200 else pulseAlpha.roundToInt().coerceIn(0, 255)
                val r = if (isHolding) 8f * s else pulseRadius
                canvas.drawCircle(2.5f * s, 2.5f * s, r, pulsePaint)
                if (!isHolding) {
                    pulseRadius += 1.5f * density
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

        val cursorScaleFlow = MutableStateFlow(1.0f)
        val cursorColorFlow = MutableStateFlow(0xFFFF2A4D.toInt())
        val cursorBorderColorFlow = MutableStateFlow(Color.WHITE)
        val cursorContrastModeFlow = MutableStateFlow(CursorContrastMode.MANUAL)

        fun updateCursorAppearance(
            scale: Float,
            colorArgb: Int,
            borderColorArgb: Int = Color.WHITE,
            contrastMode: CursorContrastMode = CursorContrastMode.MANUAL
        ) {
            cursorScaleFlow.value = scale.coerceIn(0.6f, 2.2f)
            cursorColorFlow.value = colorArgb
            cursorBorderColorFlow.value = borderColorArgb
            cursorContrastModeFlow.value = contrastMode
        }

        fun resolveEffectiveCursorColors(
            manualFillArgb: Int,
            manualBorderArgb: Int,
            mode: CursorContrastMode,
            backgroundLuminance: Float?
        ): Pair<Int, Int> {
            val lum = backgroundLuminance?.coerceIn(0f, 1f) ?: return manualFillArgb to manualBorderArgb
            return when (mode) {
                CursorContrastMode.MANUAL -> manualFillArgb to manualBorderArgb
                CursorContrastMode.INSTANT_BW -> {
                    if (lum > 0.50f) {
                        0xFF14161B.toInt() to Color.WHITE
                    } else {
                        Color.WHITE to 0xFF14161B.toInt()
                    }
                }
                CursorContrastMode.GRADUAL_ADAPTIVE -> {
                    val dark = 0xFF14161B.toInt()
                    val light = Color.WHITE
                    val fill = ColorUtils.blendARGB(light, dark, lum)
                    val border = ColorUtils.blendARGB(dark, light, lum)
                    fill to border
                }
            }
        }

        fun getInstance(): RemoteAccessibilityService? = instanceRef?.get()

        fun readBatterySnapshot(context: Context): Pair<Int, Boolean> {
            return try {
                val batIntent = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
                val level = batIntent?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
                val scale = batIntent?.getIntExtra(BatteryManager.EXTRA_SCALE, 100)?.takeIf { it > 0 } ?: 100
                val pct = if (level >= 0) ((level * 100f) / scale).roundToInt().coerceIn(0, 100) else -1
                val status = batIntent?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
                val charging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                    status == BatteryManager.BATTERY_STATUS_FULL
                pct to charging
            } catch (_: Exception) {
                -1 to false
            }
        }

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
