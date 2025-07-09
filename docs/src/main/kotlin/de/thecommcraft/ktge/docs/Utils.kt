package de.thecommcraft.ktge.docs
import kotlinx.html.*
import kotlinx.html.stream.createHTML
import java.nio.file.Path
import java.util.ArrayDeque
import java.util.Queue
import kotlin.io.path.Path
import kotlin.io.path.div

interface WebsiteDir {
    val currentFiles: Map<String, String>
    val currentSubDirs: Map<String, WebsiteDir>
    fun file(name: String, builder: TagConsumer<String>.() -> String)
    fun subDir(name: String, builder: WebsiteDir.() -> Unit)
}

fun WebsiteDir.walk() = sequence<Pair<Path, String>> {
    val queue = ArrayDeque(listOf(Pair(this@walk, Path(""))))
    while (!queue.isEmpty()) {
        val (currentDir, currentPath) = queue.removeFirst()
        currentDir.currentFiles.forEach { (name, content) ->
            yield(Pair(currentPath / name, content))
        }
        currentDir.currentSubDirs.forEach { (name, dir) ->
            queue.add(Pair(dir, currentPath / name))
        }
    }
}

open class Directory() : WebsiteDir {
    protected open val files: MutableMap<String, String> = mutableMapOf()
    protected open val subDirs: MutableMap<String, WebsiteDir> = mutableMapOf()
    override val currentFiles: Map<String, String>
        get() = files.toMap()
    override val currentSubDirs: Map<String, WebsiteDir>
        get() = subDirs.toMap()
    override fun file(name: String, builder: TagConsumer<String>.() -> String) {
        files[name] = createHTML().builder()
    }
    override fun subDir(name: String, builder: WebsiteDir.() -> Unit) {
        subDirs[name] = Directory()
        subDirs[name]?.builder()
    }
}

fun website(builder: WebsiteDir.() -> Unit): WebsiteDir {
    val root = Directory()
    root.builder()
    return root
}

fun FlowContent.container(block: FlowContent.() -> Unit) {
    div(classes = "container") {
        block()
    }
}

fun FlowContent.header(text: String) {
    div(classes = "header") {
        h1 {
            +text
        }
    }
}

fun FlowContent.subHeader(text: String) {
    div(classes = "subheader") {
        h2 {
            +text
        }
    }
}

fun FlowContent.ordinaryParagraph(text: String) {
    p {
        +text
    }
}

fun FlowContent.code(text: String, lang: String = "kotlin") {
    pre {
        code(classes = "language-${lang}") {
            +text
        }
    }
}