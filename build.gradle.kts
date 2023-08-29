plugins {
    kotlin("jvm") version "1.9.0"
    application
    `maven-publish`
}

group = "dev.energit"
version = "1.0.1"

repositories {
    mavenCentral()
    maven("https://hub.spigotmc.org/nexus/content/repositories/snapshots")
}

dependencies {
    compileOnly("org.spigotmc:spigot-api:1.20.1-R0.1-SNAPSHOT")
}

kotlin {
    jvmToolchain(17)
}

application {
    mainClass.set("MainKt")
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            groupId = "dev.energit"
            artifactId = "SimpleWorldManager"
            version = "1.0.1"

            from(components["java"])
        }
    }
}