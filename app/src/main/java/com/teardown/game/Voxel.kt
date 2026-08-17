package com.teardown.game

/**
 * Один воксель мира: либо часть статичной структуры (дом),
 * либо свободно падающий обломок после разрушения.
 */
class Voxel(
    var pos: Vec3,
    val halfExtents: Vec3,
    val r: Float,
    val g: Float,
    val b: Float,
    val a: Float,
    var isStatic: Boolean = true,
    var health: Float = 1f
) {
    var velocity: Vec3 = Vec3.ZERO
    var angVelX: Float = 0f
    var angVelY: Float = 0f
    var angVelZ: Float = 0f
    var rotX: Float = 0f
    var rotY: Float = 0f
    var rotZ: Float = 0f
    var settled: Boolean = false
    var life: Float = 12f // сек, через сколько обломок исчезает после остановки

    fun aabbMin() = Vec3(pos.x - halfExtents.x, pos.y - halfExtents.y, pos.z - halfExtents.z)
    fun aabbMax() = Vec3(pos.x + halfExtents.x, pos.y + halfExtents.y, pos.z + halfExtents.z)

    fun intersectsAabb(otherMin: Vec3, otherMax: Vec3): Boolean {
        val mn = aabbMin(); val mx = aabbMax()
        return mn.x <= otherMax.x && mx.x >= otherMin.x &&
               mn.y <= otherMax.y && mx.y >= otherMin.y &&
               mn.z <= otherMax.z && mx.z >= otherMin.z
    }
}
