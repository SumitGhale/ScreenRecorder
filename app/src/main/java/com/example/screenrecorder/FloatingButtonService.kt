package com.example.screenrecorder

import android.annotation.SuppressLint
import android.app.Service
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import androidx.compose.ui.platform.ComposeView


class FloatingButtonService: Service() {

    private lateinit var floatingButtonView: ComposeView
    private lateinit var windowManager: WindowManager

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate() {
        super.onCreate()

        //  create and setup a floating compose view
        floatingButtonView = ComposeView(this).apply {
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

        //  add touch behaviour to the floating button
        floatingButtonView.setOnTouchListener(object : View.OnTouchListener{
            private var initialX = 0
            private var initialY = 0
            private var initialTouchX = 0f
            private var initialTouchY = 0f

            override fun onTouch(v: View, event: MotionEvent): Boolean {
                when(event.action){
                    MotionEvent.ACTION_DOWN -> {
                        initialX = layoutParams.x
                        initialY = layoutParams.y
                        initialTouchX = event.rawX
                        initialTouchY = event.rawY
                        return true
                    }
                    MotionEvent.ACTION_MOVE -> {
                        layoutParams.x = initialX + (event.rawX - initialTouchX).toInt()
                        layoutParams.y = initialY + (event.rawY - initialTouchY).toInt()
                        windowManager.updateViewLayout(floatingButtonView, layoutParams)
                        return true
                    }
                }
                return false
            }
        })
    }

    override fun onBind(intent: Intent?): IBinder?  = null

    override fun onDestroy() {
        super.onDestroy()
        windowManager.removeView(floatingButtonView)
    }

}