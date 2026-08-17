package com.teardown.game

import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.math.tan

/** Простой 3D вектор с базовой арифметикой. */
data class Vec3(var x: Float = 0f, var y: Float = 0f, var z: Float = 0f) {
    operator fun plus(o: Vec3) = Vec3(x + o.x, y + o.y, z + o.z)
    operator fun minus(o: Vec3) = Vec3(x - o.x, y - o.y, z - o.z)
    operator fun times(s: Float) = Vec3(x * s, y * s, z * s)
    fun length(): Float = sqrt(x * x + y * y + z * z)
    fun normalized(): Vec3 {
        val l = length()
        return if (l < 1e-6f) Vec3(0f, 0f, 0f) else Vec3(x / l, y / l, z / l)
    }
    fun dot(o: Vec3): Float = x * o.x + y * o.y + z * o.z
    fun cross(o: Vec3): Vec3 = Vec3(
        y * o.z - z * o.y,
        z * o.x - x * o.z,
        x * o.y - y * o.x
    )
    companion object {
        val ZERO = Vec3(0f, 0f, 0f)
    }
}

/** 4x4 матрица, хранится column-major (совместимо с OpenGL). */
object Mat4 {

    fun identity(): FloatArray = floatArrayOf(
        1f, 0f, 0f, 0f,
        0f, 1f, 0f, 0f,
        0f, 0f, 1f, 0f,
        0f, 0f, 0f, 1f
    )

    fun multiply(a: FloatArray, b: FloatArray): FloatArray {
        val r = FloatArray(16)
        for (col in 0 until 4) {
            for (row in 0 until 4) {
                var sum = 0f
                for (k in 0 until 4) {
                    sum += a[k * 4 + row] * b[col * 4 + k]
                }
                r[col * 4 + row] = sum
            }
        }
        return r
    }

    fun translate(t: Vec3): FloatArray {
        val m = identity()
        m[12] = t.x; m[13] = t.y; m[14] = t.z
        return m
    }

    fun scale(s: Vec3): FloatArray {
        val m = identity()
        m[0] = s.x; m[5] = s.y; m[10] = s.z
        return m
    }

    fun rotateXYZ(rx: Float, ry: Float, rz: Float): FloatArray {
        val cx = cos(rx); val sx = sin(rx)
        val cy = cos(ry); val sy = sin(ry)
        val cz = cos(rz); val sz = sin(rz)
        // Rz * Ry * Rx combined, column-major
        val m = FloatArray(16)
        m[0] = cy * cz
        m[1] = cy * sz
        m[2] = -sy
        m[3] = 0f
        m[4] = sx * sy * cz - cx * sz
        m[5] = sx * sy * sz + cx * cz
        m[6] = sx * cy
        m[7] = 0f
        m[8] = cx * sy * cz + sx * sz
        m[9] = cx * sy * sz - sx * cz
        m[10] = cx * cy
        m[11] = 0f
        m[12] = 0f; m[13] = 0f; m[14] = 0f; m[15] = 1f
        return m
    }

    fun perspective(fovYDeg: Float, aspect: Float, near: Float, far: Float): FloatArray {
        val f = 1f / tan(Math.toRadians(fovYDeg.toDouble() / 2.0)).toFloat()
        val m = FloatArray(16)
        m[0] = f / aspect
        m[5] = f
        m[10] = (far + near) / (near - far)
        m[11] = -1f
        m[14] = (2 * far * near) / (near - far)
        return m
    }

    fun lookAt(eye: Vec3, center: Vec3, up: Vec3): FloatArray {
        val f = (center - eye).normalized()
        val s = f.cross(up).normalized()
        val u = s.cross(f)
        val m = identity()
        m[0] = s.x; m[4] = s.y; m[8] = s.z
        m[1] = u.x; m[5] = u.y; m[9] = u.z
        m[2] = -f.x; m[6] = -f.y; m[10] = -f.z
        m[12] = -s.dot(eye)
        m[13] = -u.dot(eye)
        m[14] = f.dot(eye)
        return m
    }

    fun transformPoint(m: FloatArray, p: Vec3): Vec3 {
        return Vec3(
            m[0] * p.x + m[4] * p.y + m[8] * p.z + m[12],
            m[1] * p.x + m[5] * p.y + m[9] * p.z + m[13],
            m[2] * p.x + m[6] * p.y + m[10] * p.z + m[14]
        )
    }
}
