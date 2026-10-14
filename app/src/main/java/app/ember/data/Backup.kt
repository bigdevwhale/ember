package app.ember.data

import org.json.JSONArray
import org.json.JSONObject

/** Plain JSON backup: everything stays on the device unless the user exports it. */
object Backup {
    private const val VERSION = 1

    fun encode(s: Snapshot): String = JSONObject()
        .put("app", "ember")
        .put("version", VERSION)
        .put("fasts", JSONArray(s.fasts.map {
            JSONObject().put("id", it.id).put("start", it.start).put("end", it.end ?: JSONObject.NULL)
                .put("goalMinutes", it.goalMinutes).put("plan", it.plan).put("mood", it.mood ?: JSONObject.NULL).put("note", it.note)
        }))
        .put("weights", JSONArray(s.weights.map { JSONObject().put("id", it.id).put("at", it.at).put("kg", it.kg) }))
        .put("water", JSONArray(s.water.map { JSONObject().put("day", it.day).put("glasses", it.glasses) }))
        .put("checkIns", JSONArray(s.checkIns.map {
            JSONObject().put("id", it.id).put("at", it.at).put("hunger", it.hunger).put("energy", it.energy).put("mood", it.mood).put("note", it.note)
        }))
        .toString(2)

    /** Throws [IllegalArgumentException] when the text is not an Ember backup. */
    fun decode(text: String): Snapshot {
        val root = runCatching { JSONObject(text) }.getOrNull()
        require(root != null && root.optString("app") == "ember") { "Not an Ember backup" }
        return Snapshot(
            fasts = root.list("fasts") {
                Fast(
                    getLong("id"), getLong("start"), if (isNull("end")) null else getLong("end"), getInt("goalMinutes"), getString("plan"),
                    if (isNull("mood")) null else getInt("mood"), optString("note"),
                )
            },
            weights = root.list("weights") { WeightEntry(getLong("id"), getLong("at"), getDouble("kg")) },
            water = root.list("water") { WaterDay(getLong("day"), getInt("glasses")) },
            checkIns = root.list("checkIns") { CheckIn(getLong("id"), getLong("at"), getInt("hunger"), getInt("energy"), getInt("mood"), optString("note")) },
        )
    }

    private fun <T> JSONObject.list(key: String, read: JSONObject.() -> T): List<T> {
        val arr = optJSONArray(key) ?: return emptyList()
        return (0 until arr.length()).map { arr.getJSONObject(it).read() }
    }
}
