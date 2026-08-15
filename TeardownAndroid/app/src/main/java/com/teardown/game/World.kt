package com.teardown.game

import kotlin.math.abs
import kotlin.random.Random

/**
 * Хранит все вокселя мира (дом + земля + отлетевшие обломки),
 * симулирует простую физику (гравитация, отскок от земли),
 * и обрабатывает разрушение при ударе молотка.
 */
class World(hammerDefs: List<HammerVoxelDef>) {

    val houseVoxels: MutableList<Voxel> = HouseBuilder.build()
    val groundVoxels: List<Voxel> = HouseBuilder.buildGroundVoxels()

    // локальные границы молотка (для быстрого построения мирового AABB без перебора 236 вокселей каждый кадр)
    private val hammerLocalMin: Vec3
    private val hammerLocalMax: Vec3
    // "боевая" часть - голова молотка (верхние ~35% модели), именно она наносит урон
    private val headLocalMinY: Float

    init {
        var minX = Float.MAX_VALUE; var minY = Float.MAX_VALUE; var minZ = Float.MAX_VALUE
        var maxX = -Float.MAX_VALUE; var maxY = -Float.MAX_VALUE; var maxZ = -Float.MAX_VALUE
        for (d in hammerDefs) {
            minX = minOf(minX, d.local.x - d.halfExtents.x); maxX = maxOf(maxX, d.local.x + d.halfExtents.x)
            minY = minOf(minY, d.local.y - d.halfExtents.y); maxY = maxOf(maxY, d.local.y + d.halfExtents.y)
            minZ = minOf(minZ, d.local.z - d.halfExtents.z); maxZ = maxOf(maxZ, d.local.z + d.halfExtents.z)
        }
        hammerLocalMin = Vec3(minX, minY, minZ)
        hammerLocalMax = Vec3(maxX, maxY, maxZ)
        headLocalMinY = minY + (maxY - minY) * 0.65f
    }

    val groundY = 0f
    val gravity = 9.8f
    val swingSpeedThreshold = 1.6f   // м/с - скорость хвата, начиная с которой удар "засчитывается"
    var totalBroken = 0

    /**
     * Один шаг симуляции.
     * @param gripPos мировая позиция точки хвата молотка (низ рукояти)
     * @param gripRotY поворот молотка вокруг Y (рыскание), рад
     * @param gripRotX поворот молотка вокруг X (тангаж, замах вверх-вниз), рад
     * @param gripVelocity скорость движения точки хвата, м/с
     */
    fun step(
        dt: Float,
        gripPos: Vec3,
        gripRotX: Float,
        gripRotY: Float,
        gripVelocity: Vec3
    ) {
        stepDebrisPhysics(dt)
        handleHammerCollision(gripPos, gripRotX, gripRotY, gripVelocity)
    }

    private fun stepDebrisPhysics(dt: Float) {
        val it = houseVoxels.listIterator()
        while (it.hasNext()) {
            val v = it.next()
            if (v.isStatic || v.settled) continue

            v.velocity = v.velocity + Vec3(0f, -gravity * dt, 0f)
            v.pos = v.pos + v.velocity * dt
            v.rotX += v.angVelX * dt
            v.rotY += v.angVelY * dt
            v.rotZ += v.angVelZ * dt

            val floor = groundY + v.halfExtents.y
            if (v.pos.y <= floor) {
                v.pos.y = floor
                v.velocity = Vec3(v.velocity.x * 0.6f, -v.velocity.y * 0.25f, v.velocity.z * 0.6f)
                v.angVelX *= 0.5f; v.angVelY *= 0.5f; v.angVelZ *= 0.5f
                if (abs(v.velocity.y) < 0.6f && v.velocity.length() < 0.9f) {
                    v.velocity = Vec3.ZERO
                    v.angVelX = 0f; v.angVelY = 0f; v.angVelZ = 0f
                    v.settled = true
                }
            }

            if (v.settled) {
                v.life -= dt
                if (v.life <= 0f) it.remove()
            }
        }
    }

    private fun handleHammerCollision(
        gripPos: Vec3,
        gripRotX: Float,
        gripRotY: Float,
        gripVelocity: Vec3
    ) {
        val speed = gripVelocity.length()
        if (speed < swingSpeedThreshold) return

        // Строим приблизительный мировой AABB головы молотка: берём 8 углов локального
        // бокса верхней части, поворачиваем на текущий угол молотка и переносим в мир.
        val rot = Mat4.multiply(Mat4.rotateXYZ(gripRotX, gripRotY, 0f), Mat4.identity())
        val corners = arrayOf(
            Vec3(hammerLocalMin.x, headLocalMinY, hammerLocalMin.z),
            Vec3(hammerLocalMax.x, headLocalMinY, hammerLocalMin.z),
            Vec3(hammerLocalMin.x, hammerLocalMax.y, hammerLocalMin.z),
            Vec3(hammerLocalMax.x, hammerLocalMax.y, hammerLocalMin.z),
            Vec3(hammerLocalMin.x, headLocalMinY, hammerLocalMax.z),
            Vec3(hammerLocalMax.x, headLocalMinY, hammerLocalMax.z),
            Vec3(hammerLocalMin.x, hammerLocalMax.y, hammerLocalMax.z),
            Vec3(hammerLocalMax.x, hammerLocalMax.y, hammerLocalMax.z)
        )
        var minX = Float.MAX_VALUE; var minY = Float.MAX_VALUE; var minZ = Float.MAX_VALUE
        var maxX = -Float.MAX_VALUE; var maxY = -Float.MAX_VALUE; var maxZ = -Float.MAX_VALUE
        for (c in corners) {
            val w = Mat4.transformPoint(rot, c) + gripPos
            minX = minOf(minX, w.x); maxX = maxOf(maxX, w.x)
            minY = minOf(minY, w.y); maxY = maxOf(maxY, w.y)
            minZ = minOf(minZ, w.z); maxZ = maxOf(maxZ, w.z)
        }
        val headMin = Vec3(minX, minY, minZ)
        val headMax = Vec3(maxX, maxY, maxZ)

        val impulseDir = gripVelocity.normalized()
        var brokenThisSwing = 0
        for (v in houseVoxels) {
            if (!v.isStatic) continue
            if (brokenThisSwing >= 14) break // за один удар отваливается не больше 14 вокселей
            if (v.intersectsAabb(headMin, headMax)) {
                v.isStatic = false
                val kick = 1.2f + speed * 0.35f
                v.velocity = Vec3(
                    impulseDir.x * kick + (Random.nextFloat() - 0.5f) * 1.5f,
                    Random.nextFloat() * 2.0f + 1.0f,
                    impulseDir.z * kick + (Random.nextFloat() - 0.5f) * 1.5f
                )
                v.angVelX = (Random.nextFloat() - 0.5f) * 10f
                v.angVelY = (Random.nextFloat() - 0.5f) * 10f
                v.angVelZ = (Random.nextFloat() - 0.5f) * 10f
                brokenThisSwing++
                totalBroken++
            }
        }
    }
}
