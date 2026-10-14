rootProject.name = "kstreamlined-backend"

@Suppress("UnstableApiUsage")
pluginManagement {
    repositories {
        gradlePluginPortal {
            content {
                includeGroupAndSubgroups("com.gradle")
                includeGroupAndSubgroups("org.gradle")
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
