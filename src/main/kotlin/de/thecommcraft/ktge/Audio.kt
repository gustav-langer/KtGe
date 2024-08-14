package de.thecommcraft.ktge

import java.io.File
import javax.sound.sampled.AudioSystem
import javax.sound.sampled.Clip

interface AudioPlayable {
    fun play()
}

class Audio(file: File) : AudioPlayable, AutoCloseable {
    private val _audioClip: Clip = AudioSystem.getClip()

    init {
        AudioSystem.getAudioInputStream(file).use {
            _audioClip.open(it)
        }
    }

    override fun play() {
        _audioClip.framePosition = 0
        _audioClip.start()
    }

    override fun close() {
        _audioClip.close()
    }
}

class AudioGroup(
    val audios: List<Audio>,
    val audioNames: Map<String, Int> // TODO replace this and the corresponding construct for Sprite Costumes with a better data type
) {
    fun selectAudio(audioName: String): Audio {
        return audios[audioNames[audioName]!!]
    }

    fun playAudio(audioName: String) {
        selectAudio(audioName).play()
    }

    operator fun get(audioName: String): Audio {
        return selectAudio(audioName)
    }
}

class AudioGroupBuilder {
    private val _audios: MutableList<Audio> = mutableListOf()
    private val _audioNames: MutableMap<String, Int> = mutableMapOf()

    fun audio(filePath: String, name: String) {
        val loadedAudio = Audio(File(filePath))
        _audioNames[name] = _audios.size
        _audios.add(loadedAudio)
    }

    fun build(): AudioGroup =
        AudioGroup(audios = _audios, audioNames = _audioNames)
}

fun audioGroup(init: BuildFun<AudioGroupBuilder>): AudioGroup {
    val builder = AudioGroupBuilder()
    builder.init()
    return builder.build()
}