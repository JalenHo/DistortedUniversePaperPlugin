plugins {
    java
    id("io.papermc.paperweight.userdev") version "2.0.0-beta.21"
}

group = "dev.distorteduniverse"
version = "0.9.5.1-dev"

val paperApiVersion: String by rootProject.extra
val paperDevBundle: String = paperApiVersion
val javaVersion: Int by rootProject.extra

dependencies {
    paperweight.paperDevBundle(paperDevBundle)
    testImplementation("org.junit.jupiter:junit-jupiter:5.11.4")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(javaVersion))
    }
}

tasks {
    withType<JavaCompile>().configureEach {
        options.encoding = "UTF-8"
        options.release.set(javaVersion)
    }

    processResources {
        val props = mapOf("version" to project.version)
        inputs.properties(props)
        filesMatching("plugin.yml") {
            expand(props)
        }
    }

    test {
        useJUnitPlatform()
    }

    jar {
        archiveBaseName.set("DistortedUniverseFakePlayer")
    }

    reobfJar {
        isEnabled = false
    }
}
