package com.example.jarvis.assistant

import android.Manifest
import android.content.ContentUris
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.ToneGenerator
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.core.content.ContextCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class Track(
    val id: Long,
    val title: String,
    val artist: String,
    val durationMs: Long,
    val uri: Uri,
    /** Álbum del dispositivo (a veces está vacío). */
    val album: String = "",
    /** Género de la pista según MediaStore (Android 11 o superior). */
    val genre: String = "",
    /** Año de publicación (0 si el sistema no lo sabe). */
    val year: Int = 0,
) {
    /** Versión sin dependencias de Android, para buscar y probar. */
    fun toSong(): Song = Song(
        id = id,
        title = title,
        artist = artist,
        album = album,
        genre = genre,
        year = year,
    )
}

/**
 * Canción de la biblioteca sin nada de Android: la búsqueda musical trabaja
 * con este modelo, así se puede probar en tests unitarios.
 */
data class Song(
    val id: Long,
    val title: String,
    val artist: String,
    val album: String = "",
    val genre: String = "",
    val year: Int = 0,
)

data class PlayerState(
    val trackTitle: String = "",
    val trackArtist: String = "",
    val isPlaying: Boolean = false,
    val positionMs: Long = 0,
    val durationMs: Long = 0,
    val volume: Float = 0.8f,
    val shuffle: Boolean = false,
    val repeat: Boolean = false,
    val tracks: List<Track> = emptyList(),
    val message: String? = null,
)

/**
 * Reproductor de música: usa la biblioteca del propio dispositivo y expone
 * un estado para la interfaz. También puede sonar un tono de demostración.
 */
class MusicPlayer(private val context: Context) {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private val _state = MutableStateFlow(PlayerState())
    val state: StateFlow<PlayerState> = _state.asStateFlow()

    private var mediaPlayer: MediaPlayer? = null
    private var playlist: List<Track> = emptyList()
    private var index = 0
    private var toneGenerator: ToneGenerator? = null

    /** true mientras la música está bajada para escuchar una orden. */
    private var ducked = false

    private companion object {
        /** Volumen mientras se escucha una orden: 12 %. */
        const val DUCK_VOLUME = 0.12f
    }

    fun hasAudioPermission(): Boolean {
        val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Manifest.permission.READ_MEDIA_AUDIO
        } else {
            Manifest.permission.READ_EXTERNAL_STORAGE
        }
        return ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
    }

    /** Recorre la biblioteca musical del dispositivo. */
    fun scanLibrary(): List<Track> {
        if (!hasAudioPermission()) {
            _state.value = _state.value.copy(
                message = "Necesito permiso para leer tu música.",
            )
            return emptyList()
        }
        val collection = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        // El género solo existe desde Android 11; en el resto de versiones se
        // deja vacío y la búsqueda se apoya en el título, el artista y el álbum.
        val withGenre = Build.VERSION.SDK_INT >= Build.VERSION_CODES.R
        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.ALBUM,
            MediaStore.Audio.Media.YEAR,
        ) + if (withGenre) arrayOf(MediaStore.Audio.Media.GENRE) else emptyArray()
        val tracks = mutableListOf<Track>()
        runCatching {
            context.contentResolver.query(collection, projection, null, null, null)?.use { cursor ->
                val idIndex = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
                val titleIndex = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
                val artistIndex = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
                val durationIndex = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
                val albumIndex = cursor.getColumnIndex(MediaStore.Audio.Media.ALBUM)
                val yearIndex = cursor.getColumnIndex(MediaStore.Audio.Media.YEAR)
                val genreIndex =
                    if (withGenre) cursor.getColumnIndex(MediaStore.Audio.Media.GENRE) else -1
                while (cursor.moveToNext()) {
                    val id = cursor.getLong(idIndex)
                    val title = cursor.getString(titleIndex) ?: "Sin título"
                    val artist = cursor.getString(artistIndex) ?: "Desconocido"
                    val duration = cursor.getLong(durationIndex)
                    val album = if (albumIndex >= 0) cursor.getString(albumIndex).orEmpty() else ""
                    val year = if (yearIndex >= 0) cursor.getInt(yearIndex) else 0
                    val genre = if (genreIndex >= 0) cursor.getString(genreIndex).orEmpty() else ""
                    tracks.add(
                        Track(
                            id = id,
                            title = title,
                            artist = artist,
                            durationMs = duration,
                            uri = ContentUris.withAppendedId(collection, id),
                            album = album,
                            genre = cleanGenre(genre),
                            year = year,
                        )
                    )
                }
            }
        }
        playlist = tracks
        _state.value = _state.value.copy(
            tracks = tracks,
            message = if (tracks.isEmpty()) "No hay música en el dispositivo." else null,
        )
        return tracks
    }

    /** Los valores genre que no sirven se dejan vacíos. */
    private fun cleanGenre(value: String): String {
        val clean = value.trim()
        if (clean.isBlank() || clean.equals("<unknown>", ignoreCase = true)) return ""
        return clean
    }

    fun play(track: Track) {
        // Abrir el archivo (MediaStore, tarjeta SD o nube) puede tardar
        // bastante. Si se hiciera en el hilo principal, Android mostraría
        // «Jarvis no responde» al poner música.
        val player = runCatching { MediaPlayer() }.getOrNull() ?: run {
            _state.value = _state.value.copy(message = "No se pudo reproducir el archivo.")
            return
        }
        val previous = mediaPlayer
        mediaPlayer = player
        index = playlist.indexOfFirst { it.id == track.id }.coerceAtLeast(0)
        val startVolume = if (ducked) DUCK_VOLUME else _state.value.volume

        player.setOnPreparedListener { prepared ->
            prepared.start()
            _state.value = _state.value.copy(
                isPlaying = true,
                trackTitle = track.title,
                trackArtist = track.artist,
                durationMs = track.durationMs,
                positionMs = 0,
            )
        }
        player.setOnCompletionListener {
            _state.value = _state.value.copy(isPlaying = false)
            if (_state.value.repeat) {
                play(track)
            } else {
                next()
            }
        }
        player.setOnErrorListener { _, _, _ ->
            _state.value = _state.value.copy(
                isPlaying = false,
                message = "No se pudo reproducir ${track.title}.",
            )
            true
        }

        scope.launch {
            val ok = withContext(Dispatchers.IO) {
                runCatching {
                    player.setDataSource(context, track.uri)
                    // Si la música está bajada para escuchar una orden, la
                    // pista nueva también empieza bajada.
                    player.setVolume(startVolume, startVolume)
                    player.prepareAsync()
                }.isSuccess
            }
            if (!ok) {
                _state.value = _state.value.copy(message = "No se pudo reproducir el archivo.")
            }
            // Liberar el reproductor anterior también puede bloquear.
            if (previous != null && previous !== player) {
                withContext(Dispatchers.IO) { releaseQuietly(previous) }
            }
        }
    }

    fun playRandom() {
        val tracks = playlist
        if (tracks.isEmpty()) {
            // La consulta de MediaStore puede tardar: fuera del hilo principal.
            scope.launch {
                val scanned = withContext(Dispatchers.IO) { scanLibrary() }
                if (scanned.isNotEmpty()) play(scanned.random())
            }
            return
        }
        play(tracks.random())
    }

    fun next() {
        val tracks = playlist
        if (tracks.isEmpty()) {
            playRandom()
            return
        }
        val nextIndex = if (_state.value.shuffle) {
            tracks.indices.random()
        } else {
            (index + 1) % tracks.size
        }
        index = nextIndex
        play(tracks[nextIndex])
    }

    fun previous() {
        val tracks = playlist
        if (tracks.isEmpty()) {
            playRandom()
            return
        }
        val previousIndex = if (index - 1 < 0) tracks.size - 1 else index - 1
        index = previousIndex
        play(tracks[previousIndex])
    }

    fun toggle(): Boolean {
        val player = mediaPlayer ?: return false
        return if (player.isPlaying) {
            player.pause()
            _state.value = _state.value.copy(isPlaying = false)
            false
        } else {
            player.start()
            _state.value = _state.value.copy(isPlaying = true)
            true
        }
    }

    fun repeatCurrent(): Boolean {
        val player = mediaPlayer ?: return false
        player.seekTo(0)
        player.start()
        _state.value = _state.value.copy(isPlaying = true, positionMs = 0)
        return true
    }

    fun stop() {
        val player = mediaPlayer
        mediaPlayer = null
        ducked = false
        _state.value = _state.value.copy(isPlaying = false, positionMs = 0)
        if (player != null) {
            scope.launch { withContext(Dispatchers.IO) { releaseQuietly(player) } }
        }
    }

    fun setVolume(value: Float) {
        val volume = value.coerceIn(0f, 1f)
        if (!ducked) mediaPlayer?.setVolume(volume, volume)
        _state.value = _state.value.copy(volume = volume)
    }

    /** Baja la música mientras se escucha una orden, sin perder el volumen elegido. */
    fun duckForWakeWord() {
        if (ducked) return
        ducked = true
        mediaPlayer?.setVolume(DUCK_VOLUME, DUCK_VOLUME)
    }

    /** Devuelve la música al volumen que eligió el usuario. */
    fun restoreAfterWakeWord() {
        if (!ducked) return
        ducked = false
        val volume = _state.value.volume
        mediaPlayer?.setVolume(volume, volume)
    }

    fun setShuffle(enabled: Boolean) {
        _state.value = _state.value.copy(shuffle = enabled)
    }

    fun setRepeat(enabled: Boolean) {
        _state.value = _state.value.copy(repeat = enabled)
    }

    /** Tono corto de prueba, útil cuando el dispositivo no tiene música. */
    fun playDemoTone() {
        toneGenerator?.release()
        toneGenerator = ToneGenerator(AudioManager.STREAM_MUSIC, 90)
        toneGenerator?.startTone(ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD, 1200)
        _state.value = _state.value.copy(message = "Tono de prueba emitido.")
    }

    private fun releaseQuietly(player: MediaPlayer) {
        runCatching {
            if (player.isPlaying) player.stop()
            player.reset()
            player.release()
        }
    }

    init {
        scope.launch {
            while (isActive) {
                runCatching {
                    val player = mediaPlayer
                    if (player != null && player.isPlaying) {
                        _state.value = _state.value.copy(
                            positionMs = player.currentPosition.toLong(),
                            durationMs = if (player.duration > 0) {
                                player.duration.toLong()
                            } else {
                                _state.value.durationMs
                            },
                        )
                    }
                }
                delay(500)
            }
        }
    }
}
