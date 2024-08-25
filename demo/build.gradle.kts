import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

group = properties["group"] ?: ""
version = properties["version"] ?: "0.0.0"

plugins {
    kotlin("jvm")
    application
    alias(libs.plugins.shadow)
    alias(libs.plugins.runtime)
}

repositories {
    mavenCentral()
    mavenLocal()
}

dependencies {
    implementation(project(":lib"))
    testImplementation(kotlin("test"))
}

application {
    mainClass = "de.thecommcraft.builttoscale.MainKt"
}

tasks.test {
    useJUnitPlatform()
}

kotlin {
    jvmToolchain(11)
}

// ------------------------------------------------------------------------------------------------------------------ //

tasks { // TODO find a way for any project to automatically add these tasks (once they are fixed)
    named<ShadowJar>("shadowJar") { // TODO fix: images (or anything used from ./data) needs to be included in the jar
        manifest {
            attributes["Main-Class"] = application.mainClass
            attributes["Implementation-Version"] = project.version
        }
        minimize {
            exclude(dependency("org.openrndr:openrndr-gl3:.*"))
            exclude(dependency("org.jetbrains.kotlin:kotlin-reflect:.*"))
            exclude(dependency("org.slf4j:slf4j-simple:.*"))
            exclude(dependency("org.apache.logging.log4j:log4j-slf4j2-impl:.*"))
            exclude(dependency("com.fasterxml.jackson.core:jackson-databind:.*"))
            exclude(dependency("com.fasterxml.jackson.dataformat:jackson-dataformat-yaml:.*"))
        }
    }
    named<org.beryx.runtime.JPackageTask>("jpackage") { // TODO fix this, just doesn't work
        doLast {
            val destPath = "build/jpackage/openrndr-application/data"

            copy {
                from("data") {
                    include("**/*")
                }
                into(destPath)
            }
        }
    }
    register<Zip>("jpackageZip") { // TODO depends on jpackage, so it is currently broken too
        archiveFileName = "ktgeDemo-application.zip"
        from("${layout.buildDirectory.get()}/jpackage") {
            include("**/*")
        }
        dependsOn("jpackage")
    }
}

runtime { // TODO figure out what exactly this task does, then decide if it is needed
    jpackage {
        imageName = "openrndr-application"
        skipInstaller = true
        /*if (OperatingSystem.current().isMacOsX) {
            jvmArgs.add("-XstartOnFirstThread")
            jvmArgs.add("-Duser.dir=${"$"}APPDIR/../Resources")
        }*/
    }
    options.set(listOf("--strip-debug", "--compress", "1", "--no-header-files", "--no-man-pages"))
    modules.set(listOf("jdk.unsupported", "java.management", "java.desktop"))
}
