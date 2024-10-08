rootProject.name = "kstreamlined-backend"

pluginManagement {
    repositories {
        gradlePluginPortal {
            content {
                includeGroupByRegex("org.gradle.*")
                includeGroupByRegex("com.google.cloud.tools.*")
                includeGroup("com.netflix.dgs.codegen")
            }
        }
        mavenCentral()
    }

    val toolchainsResolverVersion = file("$rootDir/gradle/libs.versions.toml")
        .readLines()
        .first { it.contains("toolchainsResolver") }
        .substringAfter("=")
        .trim()
        .removeSurrounding("\"")

    plugins {
        id("org.gradle.toolchains.foojay-resolver-convention") version toolchainsResolverVersion
    }
}

@Suppress("UnstableApiUsage")
dependencyResolutionManagement {
    repositories {
        mavenCentral()
        google()
    }
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention")
}
