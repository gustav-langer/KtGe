package de.thecommcraft.ktge.docs

import kotlin.io.path.Path
import kotlin.io.path.div
import kotlin.io.path.writeLines
import kotlin.io.path.writeText


fun main() {
    val web = website {
        generateIndexPage()
    }
    web.walk().forEach { (name, content) ->
        (Path("docs") / name).writeText(content)
    }
}