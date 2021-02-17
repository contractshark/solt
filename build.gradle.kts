import com.codingfeline.buildkonfig.compiler.FieldSpec
import com.codingfeline.buildkonfig.gradle.BuildKonfigExtension

plugins {
    kotlin("multiplatform") version "1.4.10"
    kotlin("plugin.serialization") version "1.4.10"
    id("com.codingfeline.buildkonfig") version "0.7.0"
}

group = "com.github.hjubb"
val versionString = "0.5.2"
version = versionString

val binomVersion = "0.1.22"
val ktor_version = "1.4.1"
val coroutineVersion = "1.3.9-native-mt-2"

repositories {
    mavenCentral()
    maven {
        setUrl("https://kotlin.bintray.com/kotlinx")
    }
    maven {
        setUrl("https://repo.binom.pw/releases")
    }
}
kotlin {
    macosX64 {
        binaries {
            executable {
                entryPoint = "com.github.hjubb.solt.main"
            }
        }
    }

    linuxX64 {
        binaries {
            executable {
                entryPoint = "com.github.hjubb.solt.main"
            }
        }
    }

    mingwX64 {
        binaries {
            executable {
                entryPoint = "com.github.hjubb.solt.main"
            }
        }
    }
    sourceSets {
        val macosX64Main by getting
        val mingwX64Main by getting
        val linuxX64Main by getting
        val commonMain by getting {
            dependencies {
                implementation("org.jetbrains.kotlinx:kotlinx-cli:0.3")
                implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.0.0")
                api("pw.binom.io:file:$binomVersion")
                api("pw.binom.io:core:$binomVersion")
                implementation("io.ktor:ktor-client-core:$ktor_version")
                implementation("io.ktor:ktor-client-curl:$ktor_version")
                implementation("io.ktor:ktor-client-json:$ktor_version")
                implementation("io.ktor:ktor-client-serialization:$ktor_version")
                api("org.jetbrains.kotlinx:kotlinx-coroutines-core:$coroutineVersion")
            }
        }
        val commonTest by getting
    }
}

configure<BuildKonfigExtension> {
    packageName = "com.github.hjubb.solt"

    val ether: String? by project
    val infura: String? by project

    defaultConfigs {
        buildConfigField(FieldSpec.Type.STRING, "version", versionString)
        buildConfigField(FieldSpec.Type.STRING, "ether", ether ?: "apiKey")
        buildConfigField(FieldSpec.Type.STRING, "infura", infura ?: "apiKey")
    }
}
