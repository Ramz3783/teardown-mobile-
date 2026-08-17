package com.teardown.game

import android.content.Context
import org.json.JSONArray

/** Локальные (относительно точки хвата) вокселя молотка — читаются один раз при старте. */
data class HammerVoxelDef(
    val local: Vec3,
    val halfExtents: Vec3,
    val r: Float, val g: Float, val b: Float, val a: Float
)

object HammerModel {

    fun load(context: Context, assetName: String = "hammer_voxels.json"): List<HammerVoxelDef> {
        val json = context.assets.open(assetName).bufferedReader().use { it.readText() }
        val arr = JSONArray(json)
        val list = ArrayList<HammerVoxelDef>(arr.length())
        for (i in 0 until arr.length()) {
            val o = arr.getJSONObject(i)
            list.add(
                HammerVoxelDef(
                    local = Vec3(o.getDouble("x").toFloat(), o.getDouble("y").toFloat(), o.getDouble("z").toFloat()),
                    halfExtents = Vec3(o.getDouble("hx").toFloat(), o.getDouble("hy").toFloat(), o.getDouble("hz").toFloat()),
                    r = o.getDouble("r").toFloat(),
                    g = o.getDouble("g").toFloat(),
                    b = o.getDouble("b").toFloat(),
                    a = o.getDouble("a").toFloat()
                )
            )
        }
        return list
    }
}
