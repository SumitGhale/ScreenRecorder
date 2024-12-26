package com.example.screenrecorder

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.util.Log
import android.view.Gravity
import android.view.MotionEvent
import android.view.WindowManager
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.ComposeView
import androidx.core.app.NotificationCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import kotlinx.coroutines.channels.Channel
import kotlin.math.abs

class ScreenRecordingService() : Service(), LifecycleOwner, SavedStateRegistryOwner {
    companion object{
        const val CHANNEL_ID = "ScreenRecordingServiceChannel"
        private const val ACTION_STOP_RECORDING = "STOP_RECORDING"
        private const val ACTION_TOGGLE_FLOATING_BUTTON = "TOGGLE_FLOATING_BUTTON"
    }

    private val lifecycleRegistry by lazy { LifecycleRegistry(this) }
    private val savedStateRegistryController by lazy { SavedStateRegistryController.create(this) }
    private var floatingButtonView: ComposeView? = null
    private lateinit var windowManager: WindowManager
    private var isFloatingButtonVisible = false

    override val lifecycle: Lifecycle
        get() = lifecycleRegistry

    override val savedStateRegistry: SavedStateRegistry
        get() = savedStateRegistryController.savedStateRegistry

    override fun onCreate() {
        super.onCreate()
        savedStateRegistryController.performRestore(null)
        lifecycleRegistry.currentState = Lifecycle.State.CREATED
        ViewModelStore.initialize(applicationContext)
        createNotificationChannel()
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
    }

    private fun showFloatingButotn() {
        if (floatingButtonView != null) return
        //  create and setup a floating compose view
        floatingButtonView = ComposeView(this).apply {
            setViewTreeLifecycleOwner(this@ScreenRecordingService)
            setViewTreeSavedStateRegistryOwner(this@ScreenRecordingService)

            setContent { FloatingButton() }
        }

        //  configure the layout parameters
        val layoutParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O){
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            }else{
                WindowManager.LayoutParams.TYPE_PHONE
            },
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        )
        layoutParams.gravity = Gravity.TOP or Gravity.START
        layoutParams.x = 100
        layoutParams.y = 100

        //  add the floating view to the window manager
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        windowManager.addView(floatingButtonView, layoutParams)

        //  setting touch listener to the floating button
        floatingButtonView?.let { view ->
            setupTouchListener(view, layoutParams)
        }
        isFloatingButtonVisible = true

        lifecycleRegistry.currentState = Lifecycle.State.RESUMED
    }

    private fun removeFloatingButton() {
        floatingButtonView?.let {
            windowManager.removeView(it)
            floatingButtonView = null
        }
        isFloatingButtonVisible = false

        lifecycleRegistry.currentState = Lifecycle.State.CREATED
    }

    private val handler = android.os.Handler()
    private var lastUpdateTime = 0L

    @SuppressLint("ClickableViewAccessibility")
    private fun setupTouchListener(view: ComposeView, layoutParams: WindowManager.LayoutParams) {
        val updateInterval = 16L // approximately 60fps
        var initialX = 0
        var initialY = 0
        var initialTouchX = 0f
        var initialTouchY = 0f
        var touchStartTime = 0L
        val tapThreshold = 200L

        view.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    initialX = layoutParams.x
                    initialY = layoutParams.y
                    initialTouchX = event.rawX
                    initialTouchY = event.rawY
                    touchStartTime = System.currentTimeMillis()
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    val currentTime = System.currentTimeMillis()
                    if (currentTime - lastUpdateTime > updateInterval) {
                        layoutParams.x = initialX + (event.rawX - initialTouchX).toInt()
                        layoutParams.y = initialY + (event.rawY - initialTouchY).toInt()
                        handler.post { windowManager.updateViewLayout(view, layoutParams) }
                        lastUpdateTime = currentTime
                    }
                    true
                }
                MotionEvent.ACTION_UP -> {
                    val touchEndTime = System.currentTimeMillis()
                    val isClick = touchEndTime - touchStartTime < tapThreshold
                            && abs(event.rawX - initialTouchX) < 10
                            && abs(event.rawY - initialTouchY) < 10

                    if (isClick){
                        ViewModelStore.getInstance().stopRecording()
                    }
                    true
                }
                else -> false
            }
        }
    }


    @RequiresApi(Build.VERSION_CODES.Q)
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when(intent?.action){
            "STOP_RECORDING" -> {
                ViewModelStore.getInstance().stopRecording()
                stopSelf()
                return START_NOT_STICKY
            }

            ACTION_TOGGLE_FLOATING_BUTTON->{
                if (isFloatingButtonVisible){
                    removeFloatingButton()
                }else{
                    showFloatingButotn()
                }
            }
        }

        val notification = createNotification()
        startForeground(1, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION)
        return START_STICKY
    }

    private fun createNotification(): Notification {

        // Intent to launch the app
        val openAppIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }

        //  Intent to stop screen recording
        val stopRecordingIntent = Intent(this, ScreenRecordingService::class.java).apply {
            action = "STOP_RECORDING"
        }

        //  Intent to start assistive touch
        val toggleFloatingButtonIntent = Intent(this, ScreenRecordingService::class.java).apply {
            action = ACTION_TOGGLE_FLOATING_BUTTON
        }


        val toggleFloatingButtonPendingIntent = PendingIntent.getService(
            this, 2, toggleFloatingButtonIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val stopRecordingPendingIntent = PendingIntent.getService(
            this,
            1,
            stopRecordingIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        //  pending intent nto handle notification click
        val openAppPendingIntent = PendingIntent.getActivity(
            this,
            0,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Screen Recording")
            .setContentText("Your screen is being recorded...")
            .addAction(
                R.drawable.ic_cancel,
                "Stop Recording",
                stopRecordingPendingIntent
            )
            .addAction(
                android.R.drawable.ic_menu_view,
                if (isFloatingButtonVisible) "Hide Button" else "Show Button",
                toggleFloatingButtonPendingIntent
            )
            .setContentIntent(openAppPendingIntent)
            .setSmallIcon(R.drawable.ic_display)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    private fun createNotificationChannel() {
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O){
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Screen Recording",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Screen recording is active"
            }
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager?.createNotificationChannel(channel)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        removeFloatingButton()
        lifecycleRegistry.currentState = Lifecycle.State.DESTROYED
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }
}