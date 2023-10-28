import com.google.cloud.tools.jib.gradle.BuildImageTask
import com.netflix.graphql.dgs.codegen.gradle.GenerateJavaTask
import io.gitlab.arturbosch.detekt.Detekt
import org.jetbrains.kotlin.gradle.dsl.KotlinVersion
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

tasks.withType<GenerateJavaTask>().configureEach {
    packageName = "io.github.reactivecircus.kstreamlined.backend.schema.generated"
}

jib.to.image = "gcr.io/kstreamlined-backend/kstreamlined-api"

tasks.withType<BuildImageTask>().configureEach {
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
        if (this@all.name == "detekt" && requested.group == "org.jetbrains.kotlin") {
            useVersion("1.9.0")
        }
    }
}

kotlin {
    jvmToolchain {
        languageVersion.set(JavaLanguageVersion.of(20))
    }
}

allprojects {
    detekt {
        source.setFrom(files("src/"))
        config.setFrom(files("${project.rootDir}/detekt.yml"))
        buildUponDefaultConfig = true
        allRules = true
    }
    tasks.withType<Detekt>().configureEach {
        reports {
            html.outputLocation.set(file("build/reports/detekt/${project.name}.html"))
        }
    }
    dependencies.add("detektPlugins", libs.detektFormatting)
}

tasks.withType<KotlinCompile>().configureEach {
    compilerOptions {
        languageVersion.set(KotlinVersion.KOTLIN_2_0)
        freeCompilerArgs.addAll(
            "-opt-in=kotlin.RequiresOptIn",
            "-Xjsr305=strict",
            "-Xcontext-receivers",
            "-Xbackend-threads=0",
        )
    }
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
    maxParallelForks = (Runtime.getRuntime().availableProcessors() / 2).coerceAtLeast(1)
}

fun Project.envOrProp(name: String): String {
    return providers.environmentVariable(name).orNull
        ?: providers.gradleProperty(name).orNull
        ?: throw GradleException("Missing environment variable or system property $name")
}
