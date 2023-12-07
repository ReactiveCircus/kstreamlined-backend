import com.google.cloud.tools.jib.gradle.BuildImageTask
import com.netflix.graphql.dgs.codegen.gradle.GenerateJavaTask
import io.gitlab.arturbosch.detekt.Detekt
import org.jetbrains.kotlin.gradle.dsl.KotlinVersion

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

group = "io.github.reactivecircus.kstreamlined.backend"
version = "0.0.1-SNAPSHOT"

tasks.withType<GenerateJavaTask>().configureEach {
    packageName = "io.github.reactivecircus.kstreamlined.backend.schema.generated"
}

jib {
    to.image = "australia-southeast1-docker.pkg.dev/kstreamlined-backend/kstreamlined/kstreamlined-api"
    from.image = "azul/zulu-openjdk:21"
}

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
    implementation(libs.apacheCommonsLang3)
    implementation(libs.caffeine)

    testImplementation(libs.spring.boot.starter.test)
    testImplementation(kotlin("test"))
    testImplementation(libs.ktor.client.mock)
}

kotlin {
    jvmToolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
        vendor.set(JvmVendorSpec.AZUL)
    }
    compilerOptions {
        languageVersion.set(KotlinVersion.KOTLIN_1_9) // TODO enable once tests pass with K2
        freeCompilerArgs.addAll(
            "-opt-in=kotlin.RequiresOptIn",
            "-Xjsr305=strict",
            "-Xcontext-receivers",
        )
    }
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
    maxParallelForks = (Runtime.getRuntime().availableProcessors() / 2).coerceAtLeast(1)
}

configurations.configureEach {
    resolutionStrategy.eachDependency {
        if (this@configureEach.name == "detekt" && requested.group == "org.jetbrains.kotlin") {
            useVersion("1.9.21")
        }
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

fun Project.envOrProp(name: String): String {
    return providers.environmentVariable(name).orNull
        ?: providers.gradleProperty(name).orNull
        ?: throw GradleException("Missing environment variable or system property $name")
}
