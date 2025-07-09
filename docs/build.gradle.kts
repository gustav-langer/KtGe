plugins {
    kotlin("jvm")
    `java-library`
    `maven-publish`
    application
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.jetbrains.kotlinx:kotlinx-html-jvm:0.8.1")
}

application {
    mainClass.set("SiteKt") // Only needed if using a `main()` function from `src`
}
