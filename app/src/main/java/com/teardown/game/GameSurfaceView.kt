package com.teardown.game

import android.content.Context
import android.opengl.GLSurfaceView
import android.view.MotionEvent

class GameSurfaceView(context: Context) : GLSurfaceView(context) {

    val input = InputState()
    private val renderer: GameRenderer

    private var startX = 0f
    private var startY = 0f

    init {
        setEGLContextClientVersion(3)
        renderer = GameRenderer(context, input)
        setRenderer(renderer)
        renderMode = RENDERMODE_CONTINUOUSLY
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        val w = width.coerceAtLeast(1)
        val h = height.coerceAtLeast(1)
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                startX = event.x
                startY = event.y
                input.dragging = true
            }
            MotionEvent.ACTION_MOVE -> {
                val dx = (event.x - startX) / w
                val dy = (event.y - startY) / h
                // тянешь палец вниз по экрану -> замах молота вперёд/вниз (pitch растёт)
                input.targetPitch = (dy * 4.2f).coerceIn(0f, InputState.MAX_PITCH)
                input.targetYaw = (-dx * 2.6f).coerceIn(-InputState.MAX_YAW, InputState.MAX_YAW)
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                input.dragging = false
                // отпустил - молот возвращается в исходное положение (готов к новому замаху)
                input.targetPitch = 0f
                input.targetYaw = 0f
            }
        }
        return true
    }
}
