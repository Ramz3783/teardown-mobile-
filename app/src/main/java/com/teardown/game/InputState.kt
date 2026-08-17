package com.teardown.game

/** Пишется из UI-потока (touch), читается из render-потока GLSurfaceView. */
class InputState {
    @Volatile var targetPitch: Float = 0f   // замах вперёд-вниз, радианы, 0..MAX_PITCH
    @Volatile var targetYaw: Float = 0f     // прицел влево-вправо, радианы
    @Volatile var dragging: Boolean = false

    companion object {
        const val MAX_PITCH = 2.0f   // ~115°
        const val MAX_YAW = 0.7f     // ~40°
    }
}
