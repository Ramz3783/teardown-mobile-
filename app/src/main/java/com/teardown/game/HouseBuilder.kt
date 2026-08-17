package com.teardown.game

import kotlin.math.roundToInt

/**
 * Процедурно строит простой воксельный домик: стены, дверной проём,
 * два окна и двускатную крышу. Все вокселя статичные (isStatic = true),
 * пока по ним не ударит молоток.
 */
object HouseBuilder {

    // размер одного вокселя стены в метрах
    private const val VOX = 0.22f

    fun build(originX: Float = 0f, originZ: Float = 2.2f): MutableList<Voxel> {
        val voxels = ArrayList<Voxel>(1400)

        val width = 16          // вокселей по X (~3.5м)
        val depth = 14          // вокселей по Z (~3.1м)
        val height = 11         // вокселей по Y (~2.4м)

        val wallColorR = 0.72f; val wallColorG = 0.65f; val wallColorB = 0.52f
        val roofColorR = 0.45f; val roofColorG = 0.18f; val roofColorB = 0.14f

        fun place(ix: Int, iy: Int, iz: Int, r: Float, g: Float, b: Float) {
            val x = originX + (ix - width / 2f) * VOX
            val y = iy * VOX + VOX / 2f
            val z = originZ + (iz - depth / 2f) * VOX
            voxels.add(
                Voxel(
                    pos = Vec3(x, y, z),
                    halfExtents = Vec3(VOX / 2f, VOX / 2f, VOX / 2f),
                    r = r, g = g, b = b, a = 1f,
                    isStatic = true
                )
            )
        }

        // дверной проём: по центру передней стены (iz = 0), ширина 2 вокселя, высота 5
        val doorX0 = width / 2 - 1
        val doorX1 = width / 2

        // окна на боковых стенах
        val windowY0 = 4; val windowY1 = 7

        for (ix in 0 until width) {
            for (iz in 0 until depth) {
                val isPerimeter = ix == 0 || ix == width - 1 || iz == 0 || iz == depth - 1
                if (!isPerimeter) continue
                for (iy in 0 until height) {
                    // дверь на передней стене (iz == 0)
                    if (iz == 0 && ix in doorX0..doorX1 && iy < 5) continue
                    // окна на боковых стенах (ix == 0 или ix == width-1), не рядом с углами
                    val isSideWall = ix == 0 || ix == width - 1
                    val midZ = iz in (depth / 2 - 2)..(depth / 2 + 2)
                    if (isSideWall && midZ && iy in windowY0..windowY1) continue
                    place(ix, iy, iz, wallColorR, wallColorG, wallColorB)
                }
            }
        }

        // двускатная крыша
        val roofHeight = 5
        for (layer in 0 until roofHeight) {
            val inset = layer
            val y = height + layer
            for (ix in inset until width - inset) {
                for (iz in 0 until depth) {
                    // только край ската (пустая внутренняя часть не нужна - крыша тонкая, 1 слой)
                    val isEdge = ix == inset || ix == width - 1 - inset
                    if (!isEdge) continue
                    place(ix, y, iz, roofColorR, roofColorG, roofColorB)
                }
            }
        }

        return voxels
    }

    /** Плоская земля - декоративная сетка больших плиток вокруг дома (статичная, не разрушается). */
    fun buildGroundVoxels(): List<Voxel> {
        val list = ArrayList<Voxel>()
        val tile = 1.0f
        val half = 6
        for (ix in -half..half) {
            for (iz in -1..half) {
                val checker = (ix + iz) % 2 == 0
                val shade = if (checker) 0.30f else 0.26f
                list.add(
                    Voxel(
                        pos = Vec3(ix * tile, -0.05f, iz * tile),
                        halfExtents = Vec3(tile / 2f, 0.05f, tile / 2f),
                        r = shade, g = shade + 0.02f, b = shade,
                        a = 1f,
                        isStatic = true
                    )
                )
            }
        }
        return list
    }
}
