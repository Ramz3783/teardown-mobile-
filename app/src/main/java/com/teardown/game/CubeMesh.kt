package com.teardown.game

import android.opengl.GLES30
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer

/**
 * Единая геометрия куба [-0.5,0.5]^3, отрисовывается инстансированно:
 * на каждый воксель — одна запись (позиция, размер, поворот, цвет) в instance-буфере.
 */
class CubeMesh(maxInstances: Int) {

    companion object {
        // pos.xyz, normal.xyz  -> 6 float на вершину, 36 вершин (не индексировано, по 2 треугольника на грань)
        private val CUBE_VERTS: FloatArray = floatArrayOf(
            // -Z
            -0.5f,-0.5f,-0.5f, 0f,0f,-1f,  0.5f,-0.5f,-0.5f, 0f,0f,-1f,  0.5f,0.5f,-0.5f, 0f,0f,-1f,
            -0.5f,-0.5f,-0.5f, 0f,0f,-1f,  0.5f,0.5f,-0.5f, 0f,0f,-1f, -0.5f,0.5f,-0.5f, 0f,0f,-1f,
            // +Z
            -0.5f,-0.5f,0.5f, 0f,0f,1f,   0.5f,0.5f,0.5f, 0f,0f,1f,   0.5f,-0.5f,0.5f, 0f,0f,1f,
            -0.5f,-0.5f,0.5f, 0f,0f,1f,  -0.5f,0.5f,0.5f, 0f,0f,1f,    0.5f,0.5f,0.5f, 0f,0f,1f,
            // -X
            -0.5f,-0.5f,-0.5f,-1f,0f,0f,  -0.5f,0.5f,-0.5f,-1f,0f,0f,  -0.5f,0.5f,0.5f,-1f,0f,0f,
            -0.5f,-0.5f,-0.5f,-1f,0f,0f,  -0.5f,0.5f,0.5f,-1f,0f,0f,  -0.5f,-0.5f,0.5f,-1f,0f,0f,
            // +X
            0.5f,-0.5f,-0.5f, 1f,0f,0f,   0.5f,0.5f,0.5f, 1f,0f,0f,   0.5f,0.5f,-0.5f, 1f,0f,0f,
            0.5f,-0.5f,-0.5f, 1f,0f,0f,   0.5f,-0.5f,0.5f, 1f,0f,0f,  0.5f,0.5f,0.5f, 1f,0f,0f,
            // -Y
            -0.5f,-0.5f,-0.5f, 0f,-1f,0f,  0.5f,-0.5f,0.5f, 0f,-1f,0f,  0.5f,-0.5f,-0.5f, 0f,-1f,0f,
            -0.5f,-0.5f,-0.5f, 0f,-1f,0f,  -0.5f,-0.5f,0.5f, 0f,-1f,0f,  0.5f,-0.5f,0.5f, 0f,-1f,0f,
            // +Y
            -0.5f,0.5f,-0.5f, 0f,1f,0f,   0.5f,0.5f,-0.5f, 0f,1f,0f,   0.5f,0.5f,0.5f, 0f,1f,0f,
            -0.5f,0.5f,-0.5f, 0f,1f,0f,   0.5f,0.5f,0.5f, 0f,1f,0f,   -0.5f,0.5f,0.5f, 0f,1f,0f
        )
        const val FLOATS_PER_INSTANCE = 13 // pos(3) + scale(3) + rot(3) + color(4)
    }

    private var vbo = 0
    private var instanceVbo = 0
    private var vao = 0
    private val instanceData: FloatBuffer
    private val maxInst = maxInstances
    var instanceCount = 0
        private set

    init {
        val bb = ByteBuffer.allocateDirect(CUBE_VERTS.size * 4).order(ByteOrder.nativeOrder())
        val vertBuffer = bb.asFloatBuffer().apply { put(CUBE_VERTS); position(0) }

        val vaoArr = IntArray(1); GLES30.glGenVertexArrays(1, vaoArr, 0); vao = vaoArr[0]
        GLES30.glBindVertexArray(vao)

        val bufs = IntArray(2); GLES30.glGenBuffers(2, bufs, 0)
        vbo = bufs[0]; instanceVbo = bufs[1]

        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, vbo)
        GLES30.glBufferData(GLES30.GL_ARRAY_BUFFER, CUBE_VERTS.size * 4, vertBuffer, GLES30.GL_STATIC_DRAW)
        val stride = 6 * 4
        GLES30.glEnableVertexAttribArray(0)
        GLES30.glVertexAttribPointer(0, 3, GLES30.GL_FLOAT, false, stride, 0)
        GLES30.glEnableVertexAttribArray(1)
        GLES30.glVertexAttribPointer(1, 3, GLES30.GL_FLOAT, false, stride, 3 * 4)

        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, instanceVbo)
        GLES30.glBufferData(GLES30.GL_ARRAY_BUFFER, maxInst * FLOATS_PER_INSTANCE * 4, null, GLES30.GL_DYNAMIC_DRAW)
        val istride = FLOATS_PER_INSTANCE * 4
        // location 2: instPos (vec3)
        GLES30.glEnableVertexAttribArray(2)
        GLES30.glVertexAttribPointer(2, 3, GLES30.GL_FLOAT, false, istride, 0)
        GLES30.glVertexAttribDivisor(2, 1)
        // location 3: instScale (vec3)
        GLES30.glEnableVertexAttribArray(3)
        GLES30.glVertexAttribPointer(3, 3, GLES30.GL_FLOAT, false, istride, 3 * 4)
        GLES30.glVertexAttribDivisor(3, 1)
        // location 4: instRot (vec3)
        GLES30.glEnableVertexAttribArray(4)
        GLES30.glVertexAttribPointer(4, 3, GLES30.GL_FLOAT, false, istride, 6 * 4)
        GLES30.glVertexAttribDivisor(4, 1)
        // location 5: instColor (vec4)
        GLES30.glEnableVertexAttribArray(5)
        GLES30.glVertexAttribPointer(5, 4, GLES30.GL_FLOAT, false, istride, 9 * 4)
        GLES30.glVertexAttribDivisor(5, 1)

        GLES30.glBindVertexArray(0)

        val ibb = ByteBuffer.allocateDirect(maxInst * FLOATS_PER_INSTANCE * 4).order(ByteOrder.nativeOrder())
        instanceData = ibb.asFloatBuffer()
    }

    fun beginUpdate() {
        instanceData.clear()
        instanceCount = 0
    }

    fun addInstance(pos: Vec3, scale: Vec3, rotX: Float, rotY: Float, rotZ: Float, r: Float, g: Float, b: Float, a: Float) {
        if (instanceCount >= maxInst) return
        instanceData.put(pos.x); instanceData.put(pos.y); instanceData.put(pos.z)
        instanceData.put(scale.x); instanceData.put(scale.y); instanceData.put(scale.z)
        instanceData.put(rotX); instanceData.put(rotY); instanceData.put(rotZ)
        instanceData.put(r); instanceData.put(g); instanceData.put(b); instanceData.put(a)
        instanceCount++
    }

    fun endUpdateAndDraw() {
        if (instanceCount == 0) return
        instanceData.flip()
        GLES30.glBindVertexArray(vao)
        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, instanceVbo)
        GLES30.glBufferSubData(GLES30.GL_ARRAY_BUFFER, 0, instanceCount * FLOATS_PER_INSTANCE * 4, instanceData)
        GLES30.glDrawArraysInstanced(GLES30.GL_TRIANGLES, 0, 36, instanceCount)
        GLES30.glBindVertexArray(0)
    }
}
