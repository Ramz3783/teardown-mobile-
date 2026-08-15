package com.teardown.game

import android.content.Context
import android.opengl.GLES30
import android.opengl.GLSurfaceView
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

class GameRenderer(private val context: Context, val input: InputState) : GLSurfaceView.Renderer {

    private val VERTEX_SHADER = """
        #version 300 es
        layout(location=0) in vec3 aPos;
        layout(location=1) in vec3 aNormal;
        layout(location=2) in vec3 aInstPos;
        layout(location=3) in vec3 aInstScale;
        layout(location=4) in vec3 aInstRot;
        layout(location=5) in vec4 aInstColor;

        uniform mat4 uViewProj;

        out vec4 vColor;
        out vec3 vNormal;

        mat3 rotMat(vec3 r) {
            float cx = cos(r.x); float sx = sin(r.x);
            float cy = cos(r.y); float sy = sin(r.y);
            float cz = cos(r.z); float sz = sin(r.z);
            mat3 rx = mat3(1.0,0.0,0.0,  0.0,cx,sx,  0.0,-sx,cx);
            mat3 ry = mat3(cy,0.0,-sy,   0.0,1.0,0.0, sy,0.0,cy);
            mat3 rz = mat3(cz,sz,0.0,   -sz,cz,0.0,  0.0,0.0,1.0);
            return rz * ry * rx;
        }

        void main() {
            mat3 rot = rotMat(aInstRot);
            vec3 world = rot * (aPos * aInstScale) + aInstPos;
            gl_Position = uViewProj * vec4(world, 1.0);
            vNormal = rot * aNormal;
            vColor = aInstColor;
        }
    """.trimIndent()

    private val FRAGMENT_SHADER = """
        #version 300 es
        precision mediump float;
        in vec4 vColor;
        in vec3 vNormal;
        out vec4 fragColor;
        void main() {
            vec3 lightDir = normalize(vec3(0.4, 1.0, 0.35));
            float diff = max(dot(normalize(vNormal), lightDir), 0.0);
            vec3 col = vColor.rgb * (0.38 + 0.62 * diff);
            fragColor = vec4(col, vColor.a);
        }
    """.trimIndent()

    private var program = 0
    private var uViewProjLoc = 0
    private lateinit var cubeMesh: CubeMesh
    private lateinit var hammerDefs: List<HammerVoxelDef>
    private lateinit var world: World

    private var viewportW = 1
    private var viewportH = 1
    private var lastFrameNanos = 0L

    // текущие (сглаженные) углы молота
    private var pitch = 0f
    private var yaw = 0f
    private var prevHeadWorldPos = Vec3.ZERO

    // точка хвата молота в мире (позиция, не меняется - только поворот)
    private val gripBase = Vec3(0.18f, 1.30f, -0.20f)

    // камера
    private val eye = Vec3(0f, 1.55f, -0.95f)
    private val center = Vec3(0f, 1.15f, 2.3f)
    private val up = Vec3(0f, 1f, 0f)

    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
        GLES30.glClearColor(0.53f, 0.72f, 0.86f, 1f)
        GLES30.glEnable(GLES30.GL_DEPTH_TEST)

        program = ShaderUtil.buildProgram(VERTEX_SHADER, FRAGMENT_SHADER)
        uViewProjLoc = GLES30.glGetUniformLocation(program, "uViewProj")

        hammerDefs = HammerModel.load(context)
        world = World(hammerDefs)

        val maxInstances = world.houseVoxels.size + world.groundVoxels.size + hammerDefs.size + 64
        cubeMesh = CubeMesh(maxInstances)

        val headLocal = Vec3(0f, 0.75f, 0f)
        prevHeadWorldPos = gripBase + headLocal
        lastFrameNanos = System.nanoTime()
    }

    override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
        viewportW = width; viewportH = height
        GLES30.glViewport(0, 0, width, height)
    }

    override fun onDrawFrame(gl: GL10?) {
        val now = System.nanoTime()
        var dt = (now - lastFrameNanos) / 1_000_000_000f
        lastFrameNanos = now
        if (dt > 0.05f) dt = 0.05f // защита от скачков (например, после паузы приложения)
        if (dt <= 0f) dt = 0.0001f

        // сглаживание угла молота к целевому (быстрее вперёд, чтобы удар ощущался резким)
        val followSpeed = 14f
        pitch += (input.targetPitch - pitch) * minOf(1f, dt * followSpeed)
        yaw += (input.targetYaw - yaw) * minOf(1f, dt * followSpeed)

        val rot = Mat4.rotateXYZ(pitch, yaw, 0f)
        val headLocal = Vec3(0f, 0.75f, 0f)
        val headWorldPos = Mat4.transformPoint(rot, headLocal) + gripBase
        val headVelocity = (headWorldPos - prevHeadWorldPos) * (1f / dt)
        prevHeadWorldPos = headWorldPos

        world.step(dt, gripBase, pitch, yaw, headVelocity)

        GLES30.glClear(GLES30.GL_COLOR_BUFFER_BIT or GLES30.GL_DEPTH_BUFFER_BIT)
        GLES30.glUseProgram(program)

        val aspect = viewportW.toFloat() / viewportH.toFloat()
        val proj = Mat4.perspective(60f, aspect, 0.05f, 40f)
        val view = Mat4.lookAt(eye, center, up)
        val viewProj = Mat4.multiply(proj, view)
        GLES30.glUniformMatrix4fv(uViewProjLoc, 1, false, viewProj, 0)

        cubeMesh.beginUpdate()

        for (v in world.groundVoxels) {
            cubeMesh.addInstance(v.pos, v.halfExtents * 2f, 0f, 0f, 0f, v.r, v.g, v.b, v.a)
        }
        for (v in world.houseVoxels) {
            cubeMesh.addInstance(v.pos, v.halfExtents * 2f, v.rotX, v.rotY, v.rotZ, v.r, v.g, v.b, v.a)
        }
        for (d in hammerDefs) {
            val worldPos = Mat4.transformPoint(rot, d.local) + gripBase
            cubeMesh.addInstance(worldPos, d.halfExtents * 2f, pitch, yaw, 0f, d.r, d.g, d.b, d.a)
        }

        cubeMesh.endUpdateAndDraw()
    }
}
