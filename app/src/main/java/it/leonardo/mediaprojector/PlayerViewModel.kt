package it.leonardo.mediaprojector

import android.app.Application
import android.content.Intent
import android.net.Uri
import android.provider.OpenableColumns
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.UUID

/**
 * Contiene il player (ExoPlayer), la lista e tutta la logica dei bottoni.
 * Sopravvive ai cambi di configurazione, quindi la riproduzione non si interrompe.
 */
class PlayerViewModel(app: Application) : AndroidViewModel(app) {

    private val store = PlaylistStore(app)

    val player: ExoPlayer = ExoPlayer.Builder(app).build().apply {
        setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(C.USAGE_MEDIA)
                .setContentType(C.AUDIO_CONTENT_TYPE_MOVIE)
                .build(),
            /* handleAudioFocus = */ true
        )
        repeatMode = Player.REPEAT_MODE_OFF
    }

    // ---------- Stato osservato dalla UI ----------

    var playlist by mutableStateOf(store.load())
        private set

    var selectedId by mutableStateOf<String?>(null)
        private set

    /** Id del file attualmente caricato nel player (null = fermo). */
    var nowPlayingId by mutableStateOf<String?>(null)
        private set

    var isPlaying by mutableStateOf(false)
        private set

    var positionMs by mutableLongStateOf(0L)
        private set

    var durationMs by mutableLongStateOf(0L)
        private set

    var autoAdvance by mutableStateOf(store.loadAutoAdvance())
        private set

    var errorMessage by mutableStateOf<String?>(null)
        private set

    var externalDisplayName by mutableStateOf<String?>(null)
        private set

    val selectedIndex: Int
        get() = playlist.indexOfFirst { it.id == selectedId }

    // ---------- Listener del player ----------

    private val listener = object : Player.Listener {
        override fun onIsPlayingChanged(isPlaying: Boolean) {
            this@PlayerViewModel.isPlaying = isPlaying
        }

        override fun onPlaybackStateChanged(playbackState: Int) {
            if (playbackState == Player.STATE_ENDED) onTrackEnded()
        }

        override fun onPlayerError(error: PlaybackException) {
            val name = playlist.firstOrNull { it.id == nowPlayingId }?.name ?: "file"
            errorMessage = "Impossibile riprodurre \"$name\" (${error.errorCodeName}). " +
                "Il file potrebbe essere stato spostato o cancellato."
            stop()
        }
    }

    init {
        selectedId = playlist.firstOrNull()?.id
        player.addListener(listener)

        // Aggiorna posizione/durata per la barra di avanzamento
        viewModelScope.launch {
            while (true) {
                if (player.mediaItemCount > 0) {
                    positionMs = player.currentPosition
                    val d = player.duration
                    durationMs = if (d == C.TIME_UNSET) 0L else d
                }
                delay(300)
            }
        }
    }

    // ---------- Comandi del pannello ----------

    fun play() {
        if (playlist.isEmpty()) {
            errorMessage = "La lista è vuota: premi «Aggiungi» per caricare dei file."
            return
        }
        if (selectedIndex < 0) selectedId = playlist.first().id

        val loadedIsSelected = player.mediaItemCount > 0 && nowPlayingId == selectedId
        when {
            loadedIsSelected && player.playbackState == Player.STATE_ENDED -> {
                player.seekTo(0)
                player.play()
            }
            loadedIsSelected -> player.play()          // riprende dalla pausa
            else -> loadAndPlay(selectedIndex)         // parte da capo col file selezionato
        }
    }

    fun pause() {
        if (player.mediaItemCount > 0) player.pause()
    }

    fun stop() {
        player.stop()
        player.clearMediaItems()   // lo schermo esterno torna nero
        nowPlayingId = null
        positionMs = 0L
        durationMs = 0L
    }

    fun next() = step(+1)

    fun previous() = step(-1)

    fun seekTo(ms: Long) {
        if (player.mediaItemCount > 0) player.seekTo(ms)
    }

    /** Avvia subito il file indicato (bottone ▶ sulla riga della lista). */
    fun playItem(id: String) {
        val index = playlist.indexOfFirst { it.id == id }
        if (index >= 0) loadAndPlay(index)
    }

    fun select(id: String) {
        selectedId = id
    }

    fun updateAutoAdvance(value: Boolean) {
        autoAdvance = value
        store.saveAutoAdvance(value)
    }

    fun clearError() {
        errorMessage = null
    }

    fun setExternalDisplay(name: String?) {
        externalDisplayName = name
    }

    // ---------- Modifica della lista ----------

    fun addUris(uris: List<Uri>) {
        val resolver = getApplication<Application>().contentResolver
        val added = mutableListOf<MediaEntry>()

        for (uri in uris) {
            // Permesso permanente: il file resta accessibile anche dopo il riavvio dell'app
            try {
                resolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            } catch (_: Exception) {
            }

            val mime = resolver.getType(uri).orEmpty()
            val name = queryName(uri) ?: uri.lastPathSegment ?: "file"
            val isVideo = when {
                mime.startsWith("video/") -> true
                mime.startsWith("audio/") -> false
                name.endsWith(".mp4", ignoreCase = true) -> true
                name.endsWith(".mp3", ignoreCase = true) -> false
                else -> continue   // formato non supportato: ignorato
            }
            added += MediaEntry(UUID.randomUUID().toString(), uri.toString(), name, isVideo)
        }

        if (added.isEmpty()) return
        playlist = playlist + added
        if (selectedId == null) selectedId = added.first().id
        persist()
    }

    fun remove(id: String) {
        val index = playlist.indexOfFirst { it.id == id }
        if (index < 0) return
        val removed = playlist[index]

        if (nowPlayingId == id) stop()

        playlist = playlist.toMutableList().also { it.removeAt(index) }
        if (selectedId == id) {
            selectedId = playlist.getOrNull(index)?.id ?: playlist.lastOrNull()?.id
        }
        releasePermissionIfUnused(removed.uri)
        persist()
    }

    fun move(id: String, delta: Int) {
        val from = playlist.indexOfFirst { it.id == id }
        val to = from + delta
        if (from < 0 || to !in playlist.indices) return
        val m = playlist.toMutableList()
        val item = m.removeAt(from)
        m.add(to, item)
        playlist = m
        persist()
    }

    fun clearAll() {
        stop()
        val old = playlist
        playlist = emptyList()
        selectedId = null
        old.forEach { releasePermissionIfUnused(it.uri) }
        persist()
    }

    // ---------- Interni ----------

    private fun loadAndPlay(index: Int) {
        val entry = playlist.getOrNull(index) ?: return
        errorMessage = null
        selectedId = entry.id
        nowPlayingId = entry.id
        positionMs = 0L
        durationMs = 0L
        player.setMediaItem(MediaItem.fromUri(entry.uri))
        player.prepare()
        player.play()
    }

    private fun step(delta: Int) {
        if (playlist.isEmpty()) return
        val base = selectedIndex
        if (base < 0) {
            selectedId = playlist.first().id
            return
        }
        val target = base + delta
        if (target !in playlist.indices) return

        if (player.isPlaying) {
            loadAndPlay(target)              // se sta suonando passa subito al file successivo/precedente
        } else {
            stop()                            // se è fermo/in pausa cambia solo la selezione
            selectedId = playlist[target].id
        }
    }

    private fun onTrackEnded() {
        if (!autoAdvance) return
        val index = playlist.indexOfFirst { it.id == nowPlayingId }
        if (index in 0 until playlist.lastIndex) loadAndPlay(index + 1)
    }

    private fun persist() = store.save(playlist)

    private fun queryName(uri: Uri): String? = try {
        getApplication<Application>().contentResolver
            .query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
            ?.use { c -> if (c.moveToFirst()) c.getString(0) else null }
    } catch (_: Exception) {
        null
    }

    private fun releasePermissionIfUnused(uri: String) {
        if (playlist.any { it.uri == uri }) return
        try {
            getApplication<Application>().contentResolver
                .releasePersistableUriPermission(Uri.parse(uri), Intent.FLAG_GRANT_READ_URI_PERMISSION)
        } catch (_: Exception) {
        }
    }

    override fun onCleared() {
        player.removeListener(listener)
        player.release()
        super.onCleared()
    }
}
