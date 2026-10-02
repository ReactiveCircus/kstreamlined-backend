rootProject.name = "kstreamlined-backend"

pluginManagement {
    repositories {
        gradlePluginPortal {
            content {
                includeGroupByRegex("com.gradle.*")
                includeGroupByRegex("org.gradle.*")
                includeGroupByRegex("com.google.cloud.tools.*")
                includeGroup("com.netflix.dgs.codegen")
            }
        }
        mavenCentral()
    }

    fun extractVersionFromCatalog(key: String) = file("$rootDir/gradle/libs.versions.toml")
        .readLines()
        .first { it.contains(key) }
        .substringAfter("=")
        .trim()
        .removeSurrounding("\"")

    plugins {
        id("com.gradle.develocity") version extractVersionFromCatalog("develocity")
        id("org.gradle.toolchains.foojay-resolver-convention") version extractVersionFromCatalog("toolchainsResolver")
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
    id("com.gradle.develocity")
    id("org.gradle.toolchains.foojay-resolver-convention")
}

develocity {
    buildScan {
        termsOfUseUrl = "https://gradle.com/help/legal-terms-of-use"
        termsOfUseAgree = "yes"
        publishing.onlyIf {
            System.getenv("CI") == "true"
        }
    }
}
