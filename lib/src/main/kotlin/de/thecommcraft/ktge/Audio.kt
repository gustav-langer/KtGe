package de.thecommcraft.ktge

import java.io.File
import javax.sound.sampled.AudioSystem
import javax.sound.sampled.Clip

interface AudioPlayable {
    fun play()
    fun stop()
}

class Audio(file: File) : AudioPlayable, AutoCloseable {
    private val _audioClip: Clip = AudioSystem.getClip()

    init {
        AudioSystem.getAudioInputStream(file).use {
            _audioClip.open(it)
        }
    }

    override fun play() {
        _audioClip.stop()
        _audioClip.framePosition = 0
        _audioClip.start()
    }

    override fun stop() {
        _audioClip.stop()
    }

    override fun close() {
        _audioClip.close()
    }
}

class AudioGroup(
    val audios: List<AudioPlayable>,
    val audioNames: Map<String, Int> // TODO replace this with a NamedList
) : AudioPlayable {
    private var selected: String? = null

    fun selectAudio(name: String) {
        selected = name
    }

    fun playAudio(audioName: String) {
        selected = audioName
        this[audioName].play()
    }

    override fun play() {
        selected?.let { this[it].play() }
    }

    override fun stop() {
        selected?.let { this[it].stop() }
    }

    operator fun get(audioName: String): AudioPlayable {
        return audios[audioNames[audioName]!!]
    }
}

class AudioGroupBuilder {
    private val _audios: MutableList<AudioPlayable> = mutableListOf()
    private val _audioNames: MutableMap<String, Int> = mutableMapOf()

    fun audioFile(filePath: String, name: String) {
        val loadedAudio = Audio(File(filePath))
        _audioNames[name] = _audios.size
        _audios.add(loadedAudio)
    }

    fun audio(audio: AudioPlayable, name: String) {
        _audioNames[name] = _audios.size
        _audios.add(audio)
    }

    fun build(): AudioGroup =
        AudioGroup(audios = _audios, audioNames = _audioNames)
}

fun audioGroup(init: BuildFun<AudioGroupBuilder>): AudioGroup {
    val builder = AudioGroupBuilder()
    builder.init()
    return builder.build()
}
