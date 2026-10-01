@file:Suppress("UnstableApiUsage")

import com.netflix.graphql.dgs.codegen.gradle.GenerateJavaTask
import dev.detekt.gradle.Detekt
import dev.detekt.gradle.plugin.getSupportedKotlinVersion
import org.graalvm.buildtools.gradle.tasks.BuildNativeImageTask
import org.graalvm.buildtools.gradle.tasks.GenerateResourcesConfigFile
import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.spring)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.kotlin.powerAssert)
    alias(libs.plugins.kotlin.noArg)
    alias(libs.plugins.spring.boot)
    alias(libs.plugins.spring.dependencyManagement)
    alias(libs.plugins.dgsCodegen)
    alias(libs.plugins.detekt)
    alias(libs.plugins.graalvmNative)
}

group = "io.github.reactivecircus.kstreamlined.backend"
version = "0.0.1-SNAPSHOT"

noArg {
    annotation("io.github.reactivecircus.kstreamlined.backend.store.FirestoreModel")
}

dependencyManagement {
    imports {
        mavenBom(libs.dgs.bom.get().toString())
        mavenBom(libs.kotlinx.coroutines.bom.get().toString())
        mavenBom(libs.kotlinx.serialization.bom.get().toString())
    }
}

graalvmNative {
    metadataRepository {
        enabled.set(true)
    }
    binaries.configureEach {
        javaLauncher = javaToolchains.launcherFor {
            languageVersion.set(JavaLanguageVersion.of(25))
            vendor.set(JvmVendorSpec.GRAAL_VM)
        }
        resources.autodetect()
        buildArgs(
            "-R:MaxHeapSize=100m",
            "-J-Xmx12g"
        )
    }
}

tasks.withType<BuildNativeImageTask>().configureEach {
    notCompatibleWithConfigurationCache("GraalVM plugin is not compatible with configuration cache.")
}

tasks.withType<GenerateResourcesConfigFile>().configureEach {
    notCompatibleWithConfigurationCache("GraalVM plugin is not compatible with configuration cache.")
}

tasks.bootRun {
    environment(
        envVar("KS_REDIS_REST_URL"),
        envVar("KS_REDIS_REST_TOKEN"),
        envVar("KS_GCLOUD_PROJECT_ID"),
        envVar("KS_CF_BASE_URL"),
        envVar("KS_CF_ACCOUNT_ID"),
        envVar("KS_CF_API_TOKEN"),
    )
}

tasks.named<UpdateDaemonJvm>("updateDaemonJvm") {
    languageVersion.set(JavaLanguageVersion.of(25))
}

fun envVar(name: String): Pair<String, String> {
    return name to (providers.environmentVariable(name).orElse(providers.gradleProperty(name)).orNull
        ?: error("Missing environment variable or Gradle property: $name"))
}

tasks.withType<GenerateJavaTask>().configureEach {
    packageName = "io.github.reactivecircus.kstreamlined.backend.schema.generated"
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_21)
        freeCompilerArgs.addAll(
            "-Xjsr305=strict",
        )
    }
}

java {
    sourceCompatibility = JavaVersion.VERSION_21
    targetCompatibility = JavaVersion.VERSION_21
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
    maxParallelForks = (Runtime.getRuntime().availableProcessors() / 2).coerceAtLeast(1)
}

testing {
    suites {
        register<JvmTestSuite>("integrationTest") {
            useJUnitJupiter(dependencyManagement.importedProperties.getValue("junit-jupiter.version"))
            sources.resources.srcDir("src/test/resources")
            dependencies {
                implementation(project())
                implementation(libs.kotlin.test.junit5)
                implementation(libs.spring.boot.starter.graphql.test)
                implementation(libs.mockwebserver)
                implementation(libs.testcontainers.gcloud)
                implementation(libs.testcontainers.junit.jupiter)
                implementation(libs.gcloud.firestore)
                implementation(libs.kotlinx.serialization.json)
            }
            targets.configureEach {
                testTask.configure {
                    description = "Runs integration tests against a local Firestore emulator and local HTTP stubs."
                }
            }
        }
    }
}

configurations.matching { it.name == "detekt" }.configureEach {
    resolutionStrategy.eachDependency {
        if (requested.group == "org.jetbrains.kotlin") {
            useVersion(getSupportedKotlinVersion())
        }
    }
}

detekt {
    source.setFrom(files("src/"))
    config.setFrom(files("${project.rootDir}/detekt.yml"))
    buildUponDefaultConfig = true
}
tasks.withType<Detekt>().configureEach {
    jvmTarget = JvmTarget.JVM_21.target
    reports {
        checkstyle.required.set(false)
        sarif.required.set(false)
        markdown.required.set(false)
        html.outputLocation.set(file("build/reports/detekt/${project.name}.html"))
    }
}
dependencies.add("detektPlugins", libs.detektKtlintWrapper)

@OptIn(ExperimentalKotlinGradlePluginApi::class)
powerAssert {
    compilationFilter.set {
        it.name.endsWith("test", ignoreCase = true)
    }
    functions.set(
        listOf(
            "kotlin.assert",
            "kotlin.test.assertEquals",
            "kotlin.test.assertTrue",
            "kotlin.test.assertFalse",
            "kotlin.test.assertNull",
        )
    )
}

dependencies {
    implementation(libs.spring.boot.starter)
    implementation(libs.dgs.starter)
    implementation(libs.ktor.client.core)
    implementation(libs.ktor.client.okhttp)
    implementation(libs.ktor.client.contentNegotiation)
    implementation(libs.ktor.serialization.json)
    implementation(libs.ktor.serialization.xml)
    implementation(libs.gcloud.firestore)
    implementation(libs.caffeine)
    implementation(libs.scrapeit)
    implementation(libs.ksoup)

    testImplementation(kotlin("test"))
    testImplementation(libs.spring.boot.starter.test)
    testImplementation(libs.ktor.client.mock)
}
