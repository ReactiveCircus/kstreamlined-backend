@file:Suppress("UnstableApiUsage", "DSL_SCOPE_VIOLATION")

import com.google.cloud.tools.jib.gradle.BuildImageTask
import com.netflix.graphql.dgs.codegen.gradle.GenerateJavaTask
import io.gitlab.arturbosch.detekt.Detekt
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.spring)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.spring.boot)
    alias(libs.plugins.spring.dependencyManagement)
    alias(libs.plugins.dgsCodegen)
    alias(libs.plugins.jib)
    alias(libs.plugins.detekt)
}

dependencyManagement {
    imports {
        mavenBom(libs.dgs.bom.get().toString())
    }
}

repositories {
    mavenCentral()
}

group = "io.github.reactivecircus.kstreamlined.backend"
version = "0.0.1-SNAPSHOT"

tasks.withType<GenerateJavaTask> {
    packageName = "io.github.reactivecircus.kstreamlined.backend.schema.generated"
}

jib.to.image = "gcr.io/kstreamlined-backend/kstreamlined-api"

tasks.withType<BuildImageTask> {
    notCompatibleWithConfigurationCache("Jib Gradle plugin does not support configuration cache.")
}

dependencies {
    implementation(libs.spring.boot.starter)
    implementation(libs.dgs.starter)
    implementation(libs.ktor.client.core)
    implementation(libs.ktor.client.cio)
    implementation(libs.ktor.client.contentNegotiation)
    implementation(libs.ktor.serialization.xml)
    implementation(libs.apacheCommonsText)
    implementation(libs.caffeine)

    testImplementation(libs.spring.boot.starter.test)
    testImplementation(kotlin("test"))
    testImplementation(libs.ktor.client.mock)
}

configurations.all {
    resolutionStrategy.eachDependency {
        if (requested.group == "org.jetbrains.kotlinx" && requested.name.startsWith("kotlinx-coroutines")) {
            useVersion(libs.versions.kotlinx.coroutines.get())
        }
    }
}

kotlin {
    jvmToolchain {
        languageVersion.set(JavaLanguageVersion.of("11"))
    }
}

allprojects {
    detekt {
        source = files("src/")
        config = files("${project.rootDir}/detekt.yml")
        buildUponDefaultConfig = true
        allRules = true
    }
    tasks.withType<Detekt>().configureEach {
        jvmTarget = "11"
        reports {
            html.outputLocation.set(file("build/reports/detekt/${project.name}.html"))
        }
    }
    dependencies.add("detektPlugins", libs.detektFormatting)
}


tasks.withType<KotlinCompile> {
    kotlinOptions {
        languageVersion = "1.7"
        useK2 = false // TODO enable once K2 supports compiler plugins
        freeCompilerArgs += listOf(
            "-opt-in=kotlin.RequiresOptIn",
            "-Xjsr305=strict",
            "-Xcontext-receivers",
            "-Xbackend-threads=0",
        )
        jvmTarget = "11"
    }
}

tasks.withType<Test> {
    useJUnitPlatform()
    maxParallelForks = (Runtime.getRuntime().availableProcessors() / 2).coerceAtLeast(1)
}

fun Project.envOrProp(name: String): String {
    return providers.environmentVariable(name).orNull
        ?: providers.gradleProperty(name).orNull
        ?: throw GradleException("Missing environment variable or system property $name")
}
