package de.thecommcraft.ktge

import org.openrndr.Configuration
import org.openrndr.Program
import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.ColorBuffer
import org.openrndr.draw.Drawer
import org.openrndr.draw.FontMap
import org.openrndr.draw.loadImage
import org.openrndr.math.Vector2
import javax.sound.sampled.AudioSystem
import javax.sound.sampled.AudioInputStream
import javax.sound.sampled.Clip
import java.io.File

// General Types
interface Drawable {
    fun draw(program: Program)
}

typealias BuildFun<T> = T.() -> Unit
typealias InjectFun<T> = T.() -> Unit

interface Builder<T> {
    fun build(): T
}

fun <T, B : Builder<T>> build(initial: () -> B): (BuildFun<B>) -> T {
    return fun(init: BuildFun<B>): T {
        val t = initial()
        t.init()
        return t.build()
    }
}

interface AudioPlayable {
    fun play(): Unit
}

// Sprites
class Sprite(
    val costumes: List<Costume>,
    val costumeNames: Map<String, Int>,
    val stateFun: SpriteStateFun,
    initialState: SpriteState
) : Drawable {
    var state: SpriteState = initialState
    var currentCostumeNum: Int = 0 // initially, the first costume is selected

    fun update(program: Program) {
        state = program.stateFun(state)
    }

    override fun draw(program: Program) {
        costumes[currentCostumeNum].draw(state, program)
    }
}

data class SpriteState(val position: Vector2, val size: Double)

typealias SpriteStateFun = Program.(SpriteState) -> SpriteState

object StateFun {
    fun fixed(x: Double, y: Double, size: Double = 1.0): SpriteStateFun =
        { SpriteState(position = Vector2(x, y), size = size) }

    fun position(posFun: Program.(SpriteState) -> Vector2): SpriteStateFun =
        { SpriteState(position = posFun(it), size = it.size) }
}

class SpriteBuilder : Builder<Sprite> {
    private val _costumes: MutableList<Costume> = mutableListOf()
    private val _costumeNames: MutableMap<String, Int> = mutableMapOf()
    var stateFun: SpriteStateFun = { it }
    var initialState: SpriteState = SpriteState(Vector2.ZERO, 1.0)

    fun costume(c: Costume, name: String? = null) {
        name?.let { _costumeNames[it] = _costumes.size }
        _costumes.add(c)
    }

    override fun build(): Sprite =
        Sprite(costumes = _costumes, costumeNames = _costumeNames, stateFun = stateFun, initialState = initialState)
}

val sprite: (BuildFun<SpriteBuilder>) -> Sprite = build(::SpriteBuilder)

// Costumes
interface Costume {
    fun draw(state: SpriteState, program: Program)
}

class ImageCostume(val buffer: ColorBuffer) : Costume {
    override fun draw(state: SpriteState, program: Program) {
        program.drawer.image(
            buffer,
            position = state.position,
            width = buffer.width * state.size,
            height = buffer.height * state.size
        )
    }
}

class DrawerCostume(val drawCostume: Program.(SpriteState) -> Unit) : Costume {
    override fun draw(state: SpriteState, program: Program) {
        program.drawCostume(state)
    }
}

class TextCostume(val text: String, val font: Lazy<FontMap>, val preDraw: InjectFun<Drawer>) : Costume {
    override fun draw(state: SpriteState, program: Program) = with(program) {
        drawer.fontMap = font.value
        drawer.preDraw()
        drawer.text(text, state.position)
    }
}

// Backgrounds
interface Background : Drawable

class ImageBackground : Background {
    val bg: Lazy<ColorBuffer>
    val preDraw: InjectFun<Drawer>

    constructor(image: ColorBuffer, preDraw: InjectFun<Drawer>) {
        bg = lazy { image }
        this.preDraw = preDraw
    }

    constructor(path: String, preDraw: InjectFun<Drawer>) {
        bg = lazy { loadImage(path) }
        this.preDraw = preDraw
    }

    override fun draw(program: Program) {
        program.drawer.preDraw()
        program.drawer.image(bg.value)
    }
}

class SolidBackground(val color: ColorRGBa) : Background {
    override fun draw(program: Program) {
        program.drawer.clear(color)
    }
}

object NoBackground : Background {
    override fun draw(program: Program) {}
}

// Audio
class Audio(val file: File) : AudioPlayable {
    val audioInputStream: AudioInputStream = AudioSystem.getAudioInputStream(file)
    private val _audioClip: Clip = AudioSystem.getClip()
    init {
        _audioClip.open(audioInputStream)
    }
    override fun play() : Unit {
        _audioClip.start()
    }
}

class AudioGroup (
    val audios: List<Audio>,
    val audioNames: Map<String, Int>
) {
    fun selectAudio(audioName: String): Audio {
        return audios[audioNames[audioName]!!]
    }
    fun playAudio(audioName: String): Unit {
        selectAudio(audioName).play()
    }
    operator fun get(audioName: String): Audio {
        return selectAudio(audioName)
    }
}

class AudioGroupBuilder : Builder<AudioGroup> {
    private val _audios: MutableList<Audio> = mutableListOf()
    private val _audioNames: MutableMap<String, Int> = mutableMapOf()

    fun audio(alb : AudioLoader.() -> Unit): Unit { // Audios need a name
        val a = loadAudio(alb)
        val loadedAudio = a.loadAudio()
        val name = a.name!!
        _audioNames[name] = _audios.size
        _audios.add(loadedAudio)
    }

    override fun build(): AudioGroup =
        AudioGroup(audios = _audios, audioNames = _audioNames)
}

class AudioLoader : AudioPlayable {
    var filePath: String? = null
    var name: String? = null
    var loadedAudio : Audio? = null

    fun loadAudio(): Audio {
        if (loadedAudio != null) return loadedAudio!!
        loadedAudio = Audio(File(filePath))
        return loadedAudio!!
    }

    override fun play(): Unit {
        loadAudio()
        loadedAudio!!.play()
    }
}

fun loadAudio(audioFun : AudioLoader.() -> Unit): AudioLoader {
    val audioLoader = AudioLoader()
    audioLoader.audioFun()
    audioLoader.loadAudio()
    return audioLoader
}

val audioGroup : (BuildFun<AudioGroupBuilder>) -> AudioGroup = build(::AudioGroupBuilder)

// Main
fun ktge(sprites: List<Sprite>, config: InjectFun<Configuration> = {}, background: Background = NoBackground) {
    //application(demoBuilder) // runs demo application for now
    application {
        configure(config)
        program {
            extend {
                background.draw(this)
                for (spr in sprites) {
                    spr.update(this)
                    spr.draw(this)
                }
            }
        }
    }
}