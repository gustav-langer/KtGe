import de.thecommcraft.ktge.*
import org.openrndr.KEY_ESCAPE
import org.openrndr.KEY_SPACEBAR
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.loadImage
import org.openrndr.math.Vector2
import kotlin.random.Random

val cursor = sprite {
    val color = ColorRGBa.WHITE
    costume(DrawerCostume {
        drawer.fill = color
        drawer.circle(mouse.position, 50.0)
    }, "mouseDown")
    costume(DrawerCostume {
        drawer.fill = color
        drawer.circle(mouse.position, 10.0)
    }, "mouseUp")

    init {
        costumeName = "mouseUp"
    }

    frame {
        position = mouse.position
    }

    on(mouse.buttonDown) {
        costumeName = "mouseDown"
        createSprite(clickSprite(it.position))
    }

    on(mouse.buttonUp) {
        costumeName = "mouseUp"
    }
}

fun clickSprite(position: Vector2) = sprite {
    costume(DrawerCostume {
        drawer.fill = ColorRGBa.MAGENTA
        drawer.stroke = null
        drawer.circle(position, 15.0)
    })
}

val global = sprite {
    on(keyboard.keyDown) {
        if (it.key == KEY_ESCAPE) application.exit()
        if (it.key == KEY_SPACEBAR) testAudioGroup.playAudio("hit")
        if (it.name == "q") testAudioGroup2.playAudio("hit")
        if (it.name == "a") testAudioGroup2.playAudio("group")
    }
}

val testAudioGroup: AudioGroup = audioGroup {
    audioFile(
        filePath = "data/sounds/hit.wav",
        name = "hit"
    )
}

val testAudioGroup2: AudioGroup = audioGroup {
    audioFile(
        filePath = "data/sounds/hit.wav",
        name = "hit"
    )
    audio(testAudioGroup, name = "group")
}

val tileGridTest = tileGrid {
    tileType(0) {
        costume(
            ImageCostume(
                loadImage("data/images/tile0.png"),
                drawerConfig = {},
                4
            ), "tile0"
        )
    }
    tileType(1) {
        costume(
            ImageCostume(
                loadImage("data/images/tile1.png"),
                drawerConfig = {},
                4
            ), "tile1"
        )
    }
    tileSize = 64
    gridWidth = 10
    gridHeight = 10
    frame {
        val tileX = Random.nextInt(0, width)
        val tileY = Random.nextInt(0, height)
        tiles[tileX][tileY] = Random.nextInt(2)
        val sprite = getDynamicSpriteState(getSpriteId(tileX, tileY))
        sprite.determineCostumes()
        sprite.drawToParent(app.program)
    }
}

fun main() = ktge(
    sprites = listOf(global, tileGridTest, cursor),
    config = {
        width = 640
        height = 640
        title = "KtGe Demo"
        hideCursor = true
    }
)