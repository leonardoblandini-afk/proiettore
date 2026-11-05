package it.leonardo.mediaprojector

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

/** Un elemento della lista: un file video (MP4) o audio (MP3) scelto dall'utente. */
data class MediaEntry(
    val id: String,
    val uri: String,
    val name: String,
    val isVideo: Boolean
)

/** Salva/carica la lista in SharedPreferences (JSON), così sopravvive alla chiusura dell'app. */
class PlaylistStore(context: Context) {

    private val prefs = context.getSharedPreferences("playlist", Context.MODE_PRIVATE)

    fun load(): List<MediaEntry> {
        val raw = prefs.getString(KEY_ITEMS, null) ?: return emptyList()
        return try {
            val arr = JSONArray(raw)
            List(arr.length()) { i ->
                val o = arr.getJSONObject(i)
                MediaEntry(
                    id = o.getString("id"),
                    uri = o.getString("uri"),
                    name = o.getString("name"),
                    isVideo = o.getBoolean("video")
                )
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun save(items: List<MediaEntry>) {
        val arr = JSONArray()
        items.forEach {
            arr.put(
                JSONObject()
                    .put("id", it.id)
                    .put("uri", it.uri)
                    .put("name", it.name)
                    .put("video", it.isVideo)
            )
        }
        prefs.edit().putString(KEY_ITEMS, arr.toString()).apply()
    }

    fun loadAutoAdvance(): Boolean = prefs.getBoolean(KEY_AUTO, false)

    fun saveAutoAdvance(value: Boolean) {
        prefs.edit().putBoolean(KEY_AUTO, value).apply()
    }

    private companion object {
        const val KEY_ITEMS = "items"
        const val KEY_AUTO = "auto_advance"
    }
}
