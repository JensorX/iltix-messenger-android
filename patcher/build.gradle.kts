plugins {
    kotlin("jvm") version "2.1.20"
    application
}

group = "de.iltix"
version = "1.0.0"

repositories {
    mavenCentral()
}

kotlin {
    jvmToolchain(17)
}

application {
    mainClass.set("de.iltix.patcher.IltixPatcherKt")
}

tasks.jar {
    manifest {
        attributes["Main-Class"] = "de.iltix.patcher.IltixPatcherKt"
    }
    from(configurations.runtimeClasspath.get().map { if (it.isDirectory) it else zipTree(it) })
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
}
